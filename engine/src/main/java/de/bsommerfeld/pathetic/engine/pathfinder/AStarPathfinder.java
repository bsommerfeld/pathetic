package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.EvaluationContextImpl;
import de.bsommerfeld.pathetic.engine.pathfinder.spatial.SpatialData;
import de.bsommerfeld.pathetic.engine.util.RegionKey;

/**
 * An A* pathfinding algorithm that uses a heuristic to guide the search toward the target. It
 * balances the actual cost from the start (G-cost) with the estimated cost to the target (H-cost).
 *
 * <p>This implementation uses:
 *
 * <ul>
 *   <li>A primitive min-heap for the open set (priority queue).
 *   <li>Addressable heap handles for efficient G-cost updates (decrease-key).
 *   <li>A spatial closed set with Bloom filters ({@link SpatialData}) to quickly check expanded
 *       nodes.
 * </ul>
 *
 * <p>Thread-safety: Each pathfinding operation gets its own {@link PathfindingSession}, ensuring
 * thread-safety for concurrent requests.
 */
public final class AStarPathfinder extends AbstractPathfinder {

  private final ThreadLocal<PathfindingSession> currentSession = new ThreadLocal<>();

  public AStarPathfinder(PathfinderConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected void insertStartNode(Node node, double fCost, MinHeap openSet) {
    PathfindingSession session = getSessionOrThrow();
    long packedPos = RegionKey.pack(node.getPosition());

    openSet.insertOrUpdate(packedPos, fCost);
    session.openSetNodes.put(packedPos, node);
  }

  @Override
  protected Node extractBestNode(MinHeap openSet) {
    PathfindingSession session = getSessionOrThrow();

    long packedPos = openSet.extractMin();
    Node node = session.openSetNodes.get(packedPos);
    session.openSetNodes.remove(packedPos);

    return node;
  }

  @Override
  protected void initializeSearch() {
    currentSession.set(new PathfindingSession(pathfinderConfiguration));
  }

  /**
   * Processes the successors of the current node, checking if they're in the open or closed set,
   * calculating costs, validating traversability, and updating the open set as needed.
   *
   * @param start The starting position of the pathfinding request.
   * @param target The target position of the pathfinding request.
   * @param currentNode The node being expanded.
   * @param openSet The priority queue holding nodes to explore.
   * @param searchContext The context for the current search.
   */
  @Override
  protected void processSuccessors(
      PathPosition start,
      PathPosition target,
      Node currentNode,
      MinHeap openSet,
      SearchContext searchContext) {

    PathfindingSession session = getSessionOrThrow();
    Iterable<PathVector> offsets = neighborStrategy.getOffsets(currentNode.getPosition());

    for (PathVector offset : offsets) {
      PathPosition neighborPos = currentNode.getPosition().add(offset);
      long packedPos = RegionKey.pack(neighborPos);

      // Check if neighbor is in the open set
      if (openSet.contains(packedPos)) {
        Node existing = session.openSetNodes.get(packedPos);
        updateExistingNode(existing, packedPos, currentNode, searchContext, openSet);
        continue;
      }

      Node neighbor = createNeighborNode(neighborPos, start, target, currentNode);

      /*
       * Reused across the reopen check and the "process as new node" branch below. The reopen
       * path falls through into new-node processing with identical context arguments, so a single
       * instance serves both and avoids a second allocation. Stays null when the neighbor is in
       * the closed set and reopening is disabled, so the common skip path allocates nothing.
       */
      EvaluationContext context = null;

      /*
       * G-cost computed during the reopen check, reused verbatim when the node is processed as a
       * new node below. CostProcessors are a public extension point and may be stateful, so
       * recomputing could yield a different value than the one that justified reopening - the
       * node would then enter the open set with a key inconsistent with the reopen decision.
       */
      double reopenGCost = 0.0;
      boolean reopening = false;

      // Check if neighbor is in the closed set
      SpatialData spatialData = session.getOrCreateSpatialData(neighborPos);
      if (spatialData.flod(neighborPos)) {

        /*
         * This block handles the edge case where we find a path to a node,
         * that has already been fully processed (is in the closed set).
         *
         * Normally (with consistent heuristics), the first time,
         * we close a node, we have found the shortest path to it.
         *
         * However, if the heuristic is inconsistent (or weights change dynamically),
         * we might find a "shorter" path later.
         *
         * Since this functionality can have a performance impact (in comparison to the rest of the Pathfinder),
         * we hide this behind a configuration flag, so the user can decide whether this should be active.
         */

        if (pathfinderConfiguration.shouldReopenClosedNodes()) {
          double oldCost = session.closedSetGCosts.get(packedPos);

          context =
              new EvaluationContextImpl(
                  searchContext,
                  neighbor,
                  currentNode,
                  pathfinderConfiguration.getHeuristicStrategy());

          reopenGCost = calculateGCost(context);

          // Is this path significantly better?
          if (Double.isNaN(oldCost) || reopenGCost + Math.ulp(reopenGCost) < oldCost) {
            // Mark this node for reopening
            reopening = true;
          }
        }

        if (!reopening) continue;

        // Once we got here, the node will be processed as "new" node
        // and with that effectively reopened.
      }

      // Process as a new node
      neighbor.setParent(currentNode);
      if (context == null) {
        context =
            new EvaluationContextImpl(
                searchContext, neighbor, currentNode, pathfinderConfiguration.getHeuristicStrategy());
      }

      if (!isValidByCustomProcessors(context)) {
        continue;
      }

      /*
       * --------------------------------------------------------------------------------
       * Calculates the G-cost for the new neighbor and adds it to the open set.
       *
       * This block handles three main tasks:
       *  1. Figures out the transition cost (G) from the current node to the neighbor.
       *  2. Computes the total estimated cost (F = G + H) for prioritization.
       *  3. Inserts the node into the open set with a small tie-breaker
       *     to make pathfinding smoother when multiple nodes have the same F-cost.
       *
       * What's the tie-breaker about?
       * -----------------------------
       * If multiple nodes have the same F-cost, A* might pick them randomly, which can
       * lead to jagged or inconsistent paths. To smooth things out, we give a tiny
       * advantage to nodes closer to the goal (smaller H). We do this by subtracting
       * a small value (TIE_BREAKER_WEIGHT * (H / (|F| + 1))) from F. This ensures
       * nodes closer to the goal get expanded first, without messing up correctness
       * or optimality—the bias is super small!
       *
       * Note on numerics:
       * -----------------
       * To handle floating-point inaccuracies or problematic values (like NaN/Infinity),
       * we include a safety check and use Math.ulp for precise comparisons that adapt
       * to the magnitude of the values.
       * --------------------------------------------------------------------------------
       */
      double gCost = reopening ? reopenGCost : calculateGCost(context);

      /*
       * Recorded only after validation so that a vetoed reopen attempt does not lower the stored
       * G-cost; otherwise a later, valid path with a cost between the vetoed and the stored value
       * could no longer reopen this node.
       */
      if (reopening) {
        session.closedSetGCosts.put(packedPos, gCost);
      }

      neighbor.setGCost(gCost);
      double fCost = neighbor.getFCost();
      double heapKey = calculateHeapKey(neighbor, fCost);

      openSet.insertOrUpdate(packedPos, heapKey);
      session.openSetNodes.put(packedPos, neighbor);
    }
  }

  private void updateExistingNode(
      Node existing,
      long packedPos,
      Node currentNode,
      SearchContext searchContext,
      MinHeap openSet) {

    EvaluationContext context =
        new EvaluationContextImpl(
            searchContext, existing, currentNode, pathfinderConfiguration.getHeuristicStrategy());

    double newG = calculateGCost(context);
    double tol = Math.ulp(Math.max(Math.abs(newG), Math.abs(existing.getGCost())));
    if (newG + tol >= existing.getGCost()) return;

    if (!isValidByCustomProcessors(context)) {
      return;
    }

    existing.setParent(currentNode);
    existing.setGCost(newG);

    double newF = existing.getFCost();
    double newKey = calculateHeapKey(existing, newF);

    double oldKey = openSet.cost(packedPos);

    // We only call the heap once the key actually decreased
    if (newKey + Math.ulp(newKey) < oldKey) {
      // O(log n)
      openSet.insertOrUpdate(packedPos, newKey);
    }
    // edge-case handling
    else if (Math.abs(newKey - oldKey) <= Math.ulp(newKey)) {
      /*
       * Sometimes a tiny nudging helps to maintain consistency,
       * but usually insertOrUpdate catches that.
       *
       * Since our heap strictly checks <, we can force it here
       */
      openSet.insertOrUpdate(packedPos, oldKey - Math.ulp(oldKey));
    }
  }

  private Node createNeighborNode(
      PathPosition position, PathPosition start, PathPosition target, Node parent) {
    return new Node(
        position,
        start,
        target,
        pathfinderConfiguration.getHeuristicWeights(),
        pathfinderConfiguration.getHeuristicStrategy(),
        parent.getDepth() + 1);
  }

  private boolean isValidByCustomProcessors(EvaluationContext context) {
    if (validationProcessors == null || validationProcessors.isEmpty()) {
      return true;
    }
    for (ValidationProcessor validator : validationProcessors) {
      if (!validator.isValid(context)) {
        return false;
      }
    }
    return true;
  }

  private double calculateGCost(EvaluationContext context) {
    double baseCost = context.getBaseTransitionCost();
    double additionalCost = 0.0;

    if (costProcessors != null && !costProcessors.isEmpty()) {
      for (CostProcessor processor : costProcessors) {
        Cost contribution = processor.calculateCostContribution(context);
        additionalCost += (contribution != null) ? contribution.getValue() : Cost.ZERO.getValue();
      }
    }

    double transitionCost = baseCost + additionalCost;
    if (transitionCost < 0) {
      transitionCost = 0;
    }
    return context.getPathCostToPreviousPosition() + transitionCost;
  }

  @Override
  protected void markNodeAsExpanded(Node node) {
    PathfindingSession session = getSessionOrThrow();
    PathPosition position = node.getPosition();

    long packedPos = RegionKey.pack(position);
    session.openSetNodes.remove(packedPos);

    if (pathfinderConfiguration.shouldReopenClosedNodes())
      session.closedSetGCosts.put(packedPos, node.getGCost());

    SpatialData spatialData = session.getOrCreateSpatialData(position);
    spatialData.register(position);
  }

  @Override
  protected void performAlgorithmCleanup() {
    /*
     * Single source of truth for per-search cleanup: dropping the ThreadLocal entry makes the
     * PathfindingSession unreferenced, and the next GC cycle implicitly reclaims its full state
     * (open-set node map, closed-set G-costs, visited region cache, spatial bloom filters) in
     * one sweep.
     */
    currentSession.remove();
  }

  private PathfindingSession getSessionOrThrow() {
    PathfindingSession session = currentSession.get();
    if (session == null) {
      throw new IllegalStateException(
          "Pathfinding session not initialized. Call initializeSearch() first.");
    }
    return session;
  }
}
