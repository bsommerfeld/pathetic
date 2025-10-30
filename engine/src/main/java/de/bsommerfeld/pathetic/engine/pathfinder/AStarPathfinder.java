package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeCostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.NodeEvaluationContextImpl;
import de.bsommerfeld.pathetic.engine.util.GridRegionData;
import de.bsommerfeld.pathetic.engine.util.Tuple3;
import java.util.HashMap;
import java.util.Map;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.FibonacciHeap;

/**
 * An A* pathfinding algorithm that uses a heuristic to guide the search toward the target. It
 * balances the actual cost from the start (G-cost) with the estimated cost to the target (H-cost).
 *
 * <p>This implementation uses:
 *
 * <ul>
 *   <li>A Fibonacci heap for the open set (priority queue).
 *   <li>Addressable heap handles for efficient G-cost updates (decrease-key).
 *   <li>A grid-based closed set with Bloom filters ({@link GridRegionData}) to quickly check
 *       expanded nodes.
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
  protected void initializeSearch() {
    currentSession.set(new PathfindingSession());
  }

  /**
   * Processes the successors of the current node, checking if they're in the open or closed set,
   * calculating costs, validating traversability, and updating the open set as needed.
   *
   * @param start The starting position of the pathfinding request.
   * @param target The target position of the pathfinding request.
   * @param currentNode The node being expanded.
   * @param openSet The priority queue (Fibonacci heap) holding nodes to explore.
   * @param searchContext The context for the current search.
   */
  @Override
  protected void processSuccessors(
      PathPosition start,
      PathPosition target,
      Node currentNode,
      FibonacciHeap<Double, Node> openSet,
      SearchContext searchContext) {

    PathfindingSession session = getSessionOrThrow();

    for (PathVector offset : offsets) {
      PathPosition neighborPos = currentNode.getPosition().add(offset);

      // Check if neighbor is in the open set
      AddressableHeap.Handle<Double, Node> handle = session.openSetEntries.get(neighborPos);
      if (handle != null) {
        updateExistingNode(handle, currentNode, searchContext);
        continue;
      }

      // Check if neighbor is in the closed set
      GridRegionData regionData = session.getOrCreateRegionData(neighborPos);
      if (regionData.getBloomFilter().mightContain(neighborPos)
          && regionData.getRegionalExaminedPositions().contains(neighborPos)) {

        /*
         * TODO 30.10.2025 bsommerfeld: At some point we might want to enable
         *  reopening nodes from the closed set. This would be the point to achieve this.
         *  In order to implement this, past me suggests implementing a new config value.
         */

        continue; // Skip if already expanded (assumes consistent heuristic)
      }

      // Process as a new node
      Node neighbor = createNeighborNode(neighborPos, start, target, currentNode);
      neighbor.setParent(currentNode);
      NodeEvaluationContext context =
          new NodeEvaluationContextImpl(
              searchContext, neighbor, currentNode, pathfinderConfiguration.getHeuristicStrategy());

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
       *  3. Inserts the node into the open set (Fibonacci heap) with a small tie-breaker
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
      double gCost = calculateGCost(context);
      neighbor.setGCost(gCost);
      double fCost = neighbor.getFCost();
      double heapKey = calculateHeapKey(neighbor, fCost);

      AddressableHeap.Handle<Double, Node> newHandle = openSet.insert(heapKey, neighbor);
      session.openSetEntries.put(neighborPos, newHandle);
    }
  }

  private void updateExistingNode(
      AddressableHeap.Handle<Double, Node> handle, Node currentNode, SearchContext searchContext) {
    Node existing = handle.getValue();
    NodeEvaluationContext context =
        new NodeEvaluationContextImpl(
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
    double oldKey = handle.getKey();
    double newKey = calculateHeapKey(existing, newF);

    if (newKey + Math.ulp(newKey) < oldKey) {
      handle.decreaseKey(newKey);
    } else if (Math.abs(newKey - oldKey) <= Math.ulp(newKey)) {
      handle.decreaseKey(oldKey - Math.ulp(oldKey));
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

  private boolean isValidByCustomProcessors(NodeEvaluationContext context) {
    if (nodeValidationProcessors == null || nodeValidationProcessors.isEmpty()) {
      return true;
    }
    for (NodeValidationProcessor validator : nodeValidationProcessors) {
      if (!validator.isValid(context)) {
        return false;
      }
    }
    return true;
  }

  private double calculateGCost(NodeEvaluationContext context) {
    double baseCost = context.getBaseTransitionCost();
    double additionalCost = 0.0;

    if (nodeCostProcessors != null && !nodeCostProcessors.isEmpty()) {
      for (NodeCostProcessor processor : nodeCostProcessors) {
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

    session.openSetEntries.remove(position);

    GridRegionData regionData = session.getOrCreateRegionData(position);
    regionData.getBloomFilter().put(position);
    regionData.getRegionalExaminedPositions().add(position);
  }

  @Override
  protected void performAlgorithmCleanup() {
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

  /**
   * Manages state for a single pathfinding operation, ensuring thread-safety via isolation.
   *
   * @apiNote This class is not thread-safe and is used within a ThreadLocal. If used elsewhere,
   *     developers must synchronize access to shared resources.
   */
  private class PathfindingSession {
    private final Map<Tuple3<Integer>, GridRegionData> visitedRegions = new HashMap<>();
    private final Map<PathPosition, AddressableHeap.Handle<Double, Node>> openSetEntries =
        new HashMap<>();

    GridRegionData getOrCreateRegionData(PathPosition position) {
      int cellSize = pathfinderConfiguration.getGridCellSize();
      Tuple3<Integer> gridKey =
          new Tuple3<>(
              Math.floorDiv(position.getFlooredX(), cellSize),
              Math.floorDiv(position.getFlooredY(), cellSize),
              Math.floorDiv(position.getFlooredZ(), cellSize));

      return visitedRegions.computeIfAbsent(
          gridKey, k -> new GridRegionData(pathfinderConfiguration));
    }
  }
}
