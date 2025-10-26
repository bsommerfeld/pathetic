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
 * An A* (A-star) pathfinding algorithm implementation. It uses a heuristic to guide the search
 * towards the target and considers both the actual cost from the start (G-cost) and the estimated
 * cost to the target (H-cost).
 *
 * <p>This implementation employs:
 *
 * <ul>
 *   <li>An open set (priority queue) implemented with a {@link FibonacciHeap}.
 *   <li>A mechanism to track nodes in the open set via {@link AddressableHeap.Handle} objects for
 *       efficient G-cost updates (decrease-key operations).
 *   <li>A closed set optimization using a grid-based approach with Bloom filters (via {@link
 *       GridRegionData}) to quickly identify previously expanded nodes.
 * </ul>
 *
 * <p>Thread-Safety: This implementation creates a new {@link PathfindingSession} for each
 * pathfinding operation, ensuring thread-safety even when multiple pathfinding requests are
 * executed concurrently.
 */
public final class AStarPathfinder extends AbstractPathfinder {

  private static final double EPS = 1e-9; // tolerance for double-comparison
  private static final double TIE_BREAKER_WEIGHT = 1e-6;

  /**
   * Thread-local storage for the current pathfinding session. This ensures that each pathfinding
   * operation has its own isolated state.
   */
  private final ThreadLocal<PathfindingSession> currentSession = new ThreadLocal<>();

  /**
   * Constructs an AStarPathfinder with the given configuration.
   *
   * @param pathfinderConfiguration The configuration for this pathfinder.
   */
  public AStarPathfinder(PathfinderConfiguration pathfinderConfiguration) {
    super(pathfinderConfiguration);
  }

  /**
   * Initializes data structures for a new search by creating a new PathfindingSession. Each search
   * gets its own isolated state to prevent race conditions.
   */
  @Override
  protected void initializeSearch() {
    currentSession.set(new PathfindingSession());
  }

  /**
   * Processes the successors of the {@code currentNode}. For each successor, it checks if it's
   * already in the open or closed set, calculates costs, validates traversability, and updates the
   * open set accordingly.
   *
   * @param requestStart The starting position of the overall pathfinding request.
   * @param requestTarget The target position of the overall pathfinding request.
   * @param currentNode The node currently being expanded.
   * @param openSet The priority queue (FibonacciHeap) holding nodes to be explored.
   * @param searchContext The context for the current search operation.
   */
  @Override
  protected void processSuccessors(
      PathPosition requestStart,
      PathPosition requestTarget,
      Node currentNode,
      int currentDepth,
      FibonacciHeap<Double, Node> openSet,
      SearchContext searchContext) {

    PathfindingSession session = currentSession.get();
    if (session == null) {
      throw new IllegalStateException("PathfindingSession not initialized.");
    }

    for (PathVector offset : offsets) {
      PathPosition neighborPosition = currentNode.getPosition().add(offset);

      // 1. Check if the neighbor is already in the open set
      AddressableHeap.Handle<Double, Node> existingHandle =
          session.openSetEntries.get(neighborPosition);

      if (existingHandle != null) {
        Node existing = existingHandle.getValue();

        NodeEvaluationContext nodeEvalContext =
            new NodeEvaluationContextImpl(
                searchContext,
                existing,
                currentNode,
                pathfinderConfiguration.getHeuristicStrategy());

        double newG = calculateGCostForSuccessor(nodeEvalContext);

        // Check the new gcost against the current to avoid unnecessary updates
        if (newG + EPS < existing.getGCost()) {
          if (isValidByCustomProcessors(nodeEvalContext)) {
            existing.setParent(currentNode);
            existing.setGCost(newG);

            double newF = existing.getFCost();
            double oldF = existingHandle.getKey();

            if (newF + EPS < oldF) existingHandle.decreaseKey(existing.getFCost());
            else if (Math.abs(newF - oldF) <= EPS) existingHandle.decreaseKey(oldF - EPS);
          }
        }
        continue; // Already processed or updated in open set
      }

      // 2. Check if the neighbor has already been expanded (in the closed set)
      GridRegionData regionData = session.getOrCreateRegionData(neighborPosition);
      boolean possiblyVisited = regionData.getBloomFilter().mightContain(neighborPosition);

      if (possiblyVisited && regionData.getRegionalExaminedPositions().contains(neighborPosition)) {
        // Standard A* with a consistent heuristic assumes the first time a node
        // is expanded, it's via the optimal path. So, skip.
        continue;
      }

      // 3. Process as a new node
      Node neighborNode =
          new Node(
              neighborPosition,
              requestStart,
              requestTarget,
              pathfinderConfiguration.getHeuristicWeights(),
              pathfinderConfiguration.getHeuristicStrategy(),
              currentNode.getDepth() + 1);

      neighborNode.setParent(currentNode); // Set parent early for context

      NodeEvaluationContext nodeEvalContext =
          new NodeEvaluationContextImpl(
              searchContext,
              neighborNode,
              currentNode,
              pathfinderConfiguration.getHeuristicStrategy());

      // Validate the new neighbor node
      if (!isValidByCustomProcessors(nodeEvalContext)) {
        continue;
      }

      /*
       * ---------------------------------------------------------------------------
       * Calculate the G-cost for the new neighbor and add it to the open set.
       *
       * This block performs three key tasks:
       *  1. Computes the transition cost (G) from the current node to the neighbor.
       *  2. Calculates the total estimated cost (F = G + H) for prioritization.
       *  3. Inserts the node into the open set (FibonacciHeap) with a tiny heuristic-based
       *     tie-breaker to ensure smoother pathfinding performance when multiple nodes
       *     have identical F-costs.
       *
       * Tie-breaker explanation:
       * ------------------------
       *   - When multiple nodes share the same F-cost, A* may expand them arbitrarily,
       *     which can cause zig-zag or inconsistent exploration.
       *   - To stabilize this, we slightly bias the F-cost by a very small multiple
       *     of the heuristic (H). Nodes closer to the goal (smaller H) get marginally
       *     lower F' = F - 1e-6 * H, and are therefore expanded first.
       *   - This does NOT affect optimality or correctness, since the bias is negligible.
       *
       * EPS note:
       * ----------
       *   A minimal safety check is included to protect against floating-point
       *   inaccuracies or NaN/Infinity values.
       * ---------------------------------------------------------------------------
       */

      // Calculate base G-cost for the new neighbor
      double gCostForNewNeighbor = calculateGCostForSuccessor(nodeEvalContext);
      neighborNode.setGCost(gCostForNewNeighbor);

      // Standard F-cost (F = G + H)
      double fCost = neighborNode.getFCost();

      // Apply a small heuristic-based tie-breaker for smoother queue behavior
      double heuristic = neighborNode.getHeuristic().get();
      double fForHeap =
          fCost - TIE_BREAKER_WEIGHT * heuristic; // smaller H → slightly higher priority

      // Numerical safety guard (avoid NaN or Infinity)
      if (Double.isNaN(fForHeap) || Double.isInfinite(fForHeap)) {
        fForHeap = fCost; // fallback to default
      }

      // Insert into open set (priority queue)
      AddressableHeap.Handle<Double, Node> newHandle = openSet.insert(fForHeap, neighborNode);

      // Track the handle in session for future decreaseKey updates
      session.openSetEntries.put(neighborPosition, newHandle);
    }
  }

  private boolean isValidByCustomProcessors(NodeEvaluationContext nodeEvalContext) {
    boolean isValidByCustomProcessors = true;
    if (this.nodeValidationProcessors != null && !this.nodeValidationProcessors.isEmpty()) {
      for (final NodeValidationProcessor validator : this.nodeValidationProcessors) {
        if (!validator.isValid(nodeEvalContext)) {
          isValidByCustomProcessors = false;
          break;
        }
      }
    }
    return isValidByCustomProcessors;
  }

  /**
   * Calculates the G-cost for a successor node based on the transition from its predecessor. This
   * involves the base transition cost and contributions from {@link NodeCostProcessor}s.
   *
   * @param nodeEvalContext The evaluation context for the successor node. Its {@code
   *     getPathCostToPreviousPosition()} should provide the G-cost of the predecessor.
   * @return The calculated G-cost for the successor node.
   */
  private double calculateGCostForSuccessor(NodeEvaluationContext nodeEvalContext) {
    double baseTransitionCost = nodeEvalContext.getBaseTransitionCost();
    double accumulatedContributions = 0.0;

    // Ensure nodeCostProcessors is accessed from the instance (inherited from AbstractPathfinder)
    if (super.nodeCostProcessors != null && !super.nodeCostProcessors.isEmpty()) {
      for (NodeCostProcessor costCalculator : super.nodeCostProcessors) {
        Cost contribution = costCalculator.calculateCostContribution(nodeEvalContext);
        if (contribution == null) {
          contribution = Cost.ZERO;
        }
        accumulatedContributions += contribution.getValue();
      }
    }

    double finalTransitionCost = baseTransitionCost + accumulatedContributions;
    if (finalTransitionCost < 0 && !pathfinderConfiguration.areNegativeCostsAllowed()) {
      finalTransitionCost = 0;
    }
    // nodeEvalContext.getPathCostToPreviousPosition() gets the G-cost of the node *from which* we
    // are moving.
    return nodeEvalContext.getPathCostToPreviousPosition() + finalTransitionCost;
  }

  /**
   * Marks the given node as expanded (i.e., moved to the "closed set"). This involves removing it
   * from the {@code openSetEntries} tracker and adding its position to the {@code
   * visitedRegionGrid}.
   *
   * @param node The node that has been taken from the open set and is being expanded.
   */
  @Override
  protected void markNodeAsExpanded(Node node) {
    PathfindingSession session = currentSession.get();
    if (session == null) {
      throw new IllegalStateException(
          "PathfindingSession not initialized. Call initializeSearch() first.");
    }

    PathPosition position = node.getPosition();

    // Remove from open set tracking
    session.openSetEntries.remove(position);

    // And add to closed set
    GridRegionData regionData = session.getOrCreateRegionData(position);
    regionData.getBloomFilter().put(position);
    regionData.getRegionalExaminedPositions().add(position);
  }

  /**
   * Performs cleanup after a pathfinding search is complete. This clears the thread-local session
   * to prevent memory leaks.
   */
  @Override
  protected void performAlgorithmCleanup() {
    currentSession.remove(); // Clear the thread-local session
  }

  /**
   * Encapsulates all state required for a single pathfinding operation. This ensures thread-safety
   * by giving each pathfinding operation its own isolated state.
   *
   * @apiNote A PathfindingSession is NOT threadsafe and is currently only used within a
   *     ThreadLocal. If this should change, the developer must ensure thread-safety by
   *     synchronizing access to shared resources.
   */
  private class PathfindingSession {

    /**
     * Stores regional data for the closed set optimization. Keys are grid cell coordinates, values
     * contain Bloom filters and sets of examined positions.
     */
    private final Map<Tuple3<Integer>, GridRegionData> visitedRegionGrid = new HashMap<>();

    /**
     * Tracks heap handles for nodes currently in the open set, keyed by their {@link PathPosition}.
     * This allows for efficient lookup and updates (e.g., {@code decreaseKey}) if a cheaper path to
     * an already-open node is found.
     */
    private final Map<PathPosition, AddressableHeap.Handle<Double, Node>> openSetEntries =
        new HashMap<>();

    /**
     * Retrieves or creates {@link GridRegionData} for the grid cell corresponding to the given
     * position. This is part of the closed set optimization.
     *
     * @param position The {@link PathPosition}.
     * @return The {@link GridRegionData} for that region.
     */
    private GridRegionData getOrCreateRegionData(PathPosition position) {
      int gridCellSize = pathfinderConfiguration.getGridCellSize();
      int gridX = Math.floorDiv(position.getFlooredX(), gridCellSize);
      int gridY = Math.floorDiv(position.getFlooredY(), gridCellSize);
      int gridZ = Math.floorDiv(position.getFlooredZ(), gridCellSize);

      // Using Tuple3 as a key for the grid cell coordinates
      Tuple3<Integer> gridKey = new Tuple3<>(gridX, gridY, gridZ);

      return visitedRegionGrid.computeIfAbsent(
          gridKey, k -> new GridRegionData(pathfinderConfiguration));
    }
  }
}
