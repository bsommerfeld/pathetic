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
import org.jheaps.AddressableHeap;
import org.jheaps.tree.FibonacciHeap;

import java.util.HashMap;
import java.util.Map;

/**
 * An A* (A-star) pathfinding algorithm implementation. It uses a heuristic to guide the search towards the target and
 * considers both the actual cost from the start (G-cost) and the estimated cost to the target (H-cost).
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
public class AStarPathfinder extends AbstractPathfinder {

    /**
     * Thread-local storage for the current pathfinding session. This ensures that each pathfinding operation has its
     * own isolated state.
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
     * Initializes data structures for a new search by creating a new PathfindingSession. Each search gets its own
     * isolated state to prevent race conditions.
     */
    @Override
    protected void initializeSearch() {
        currentSession.set(new PathfindingSession());
    }

    /**
     * Processes the successors of the {@code currentNode}. For each successor, it checks if it's already in the open or
     * closed set, calculates costs, validates traversability, and updates the open set accordingly.
     *
     * @param requestStart       The starting position of the overall pathfinding request.
     * @param requestTarget      The target position of the overall pathfinding request.
     * @param currentNode        The node currently being expanded.
     * @param currentSearchDepth The current depth of the search (number of expansions).
     * @param openSet            The priority queue (FibonacciHeap) holding nodes to be explored.
     * @param searchContext      The context for the current search operation.
     */
    @Override
    protected void processSuccessors(
            PathPosition requestStart,
            PathPosition requestTarget,
            Node currentNode,
            int currentSearchDepth,
            FibonacciHeap<Double, Node> openSet,
            SearchContext searchContext) {

        PathfindingSession session = currentSession.get();
        if (session == null) {
            throw new IllegalStateException(
                    "PathfindingSession not initialized. Call initializeSearch() first.");
        }

        for (PathVector offset : offsets) {
            PathPosition neighborPosition = currentNode.getPosition().add(offset);

            // 1. Check if the neighbor is already in the open set
            AddressableHeap.Handle<Double, Node> existingHandle =
                    session.openSetEntries.get(neighborPosition);
            if (existingHandle != null) {
                Node existingNodeInHeap = existingHandle.getValue();
                NodeEvaluationContext nodeEvalContext =
                        new NodeEvaluationContextImpl(
                                searchContext,
                                existingNodeInHeap,
                                currentNode,
                                pathfinderConfiguration.getHeuristicStrategy());

                double newGCostForExisting = calculateGCostForSuccessor(nodeEvalContext);

                if (newGCostForExisting < existingNodeInHeap.getGCost()) {
                    boolean isValidByCustomProcessors = true;
                    if (this.nodeValidationProcessors != null && !this.nodeValidationProcessors.isEmpty()) {
                        for (final NodeValidationProcessor validator : this.nodeValidationProcessors) {
                            if (!validator.isValid(nodeEvalContext)) {
                                isValidByCustomProcessors = false;
                                break;
                            }
                        }
                    }

                    if (isValidByCustomProcessors) {
                        existingNodeInHeap.setParent(currentNode);
                        existingNodeInHeap.setGCost(newGCostForExisting);
                        existingHandle.decreaseKey(existingNodeInHeap.getFCost());
                    }
                }
                continue; // Already processed or updated in open set
            }

            // 2. Check if the neighbor has already been expanded (in the closed set)
            GridRegionData regionData = session.getOrCreateRegionData(neighborPosition);
            boolean alreadyExpanded =
                    regionData.getBloomFilter().mightContain(neighborPosition);

            if(!alreadyExpanded)
                alreadyExpanded = regionData.getRegionalExaminedPositions().contains(neighborPosition);

            if (alreadyExpanded) {
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
                            searchContext, neighborNode, currentNode, pathfinderConfiguration.getHeuristicStrategy());

            // Validate the new neighbor node
            boolean isValidByCustomProcessors = true;
            if (this.nodeValidationProcessors != null && !this.nodeValidationProcessors.isEmpty()) {
                for (NodeValidationProcessor validator : this.nodeValidationProcessors) {
                    if (!validator.isValid(nodeEvalContext)) {
                        isValidByCustomProcessors = false;
                        break;
                    }
                }
            }

            if (!isValidByCustomProcessors) {
                continue;
            }

            // Calculate G-cost and add to open set
            double gCostForNewNeighbor = calculateGCostForSuccessor(nodeEvalContext);
            neighborNode.setGCost(gCostForNewNeighbor);

            AddressableHeap.Handle<Double, Node> newHandle =
                    openSet.insert(neighborNode.getFCost(), neighborNode);
            session.openSetEntries.put(neighborPosition, newHandle);
        }
    }

    /**
     * Calculates the G-cost for a successor node based on the transition from its predecessor. This involves the base
     * transition cost and contributions from {@link NodeCostProcessor}s.
     *
     * @param nodeEvalContext The evaluation context for the successor node. Its {@code getPathCostToPreviousPosition()}
     *                        should provide the G-cost of the predecessor.
     *
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
     * Marks the given node as expanded (i.e., moved to the "closed set"). This involves removing it from the
     * {@code openSetEntries} tracker and adding its position to the {@code visitedRegionGrid}.
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
     * Performs cleanup after a pathfinding search is complete. This clears the thread-local session to prevent memory
     * leaks.
     */
    @Override
    protected void performAlgorithmCleanup() {
        currentSession.remove(); // Clear the thread-local session
    }

    /**
     * Encapsulates all state required for a single pathfinding operation. This ensures thread-safety by giving each
     * pathfinding operation its own isolated state.
     */
    private class PathfindingSession {

        /**
         * Stores regional data for the closed set optimization. Keys are grid cell coordinates, values contain Bloom
         * filters and sets of examined positions.
         */
        private final Map<Tuple3<Integer>, GridRegionData> visitedRegionGrid = new HashMap<>();

        /**
         * Tracks heap handles for nodes currently in the open set, keyed by their {@link PathPosition}. This allows for
         * efficient lookup and updates (e.g., {@code decreaseKey}) if a cheaper path to an already-open node is found.
         */
        private final Map<PathPosition, AddressableHeap.Handle<Double, Node>> openSetEntries =
                new HashMap<>();

        /**
         * Retrieves or creates {@link GridRegionData} for the grid cell corresponding to the given position. This is
         * part of the closed set optimization.
         *
         * @param position The {@link PathPosition}.
         *
         * @return The {@link GridRegionData} for that region.
         */
        private GridRegionData getOrCreateRegionData(PathPosition position) {
            int gridCellSize = pathfinderConfiguration.getGridCellSize();
            int gridX = position.getFlooredX() / gridCellSize;
            int gridY = position.getFlooredY() / gridCellSize;
            int gridZ = position.getFlooredZ() / gridCellSize;

            // Using Tuple3 as a key for the grid cell coordinates
            Tuple3<Integer> gridKey = new Tuple3<>(gridX, gridY, gridZ);

            return visitedRegionGrid.computeIfAbsent(
                    gridKey, k -> new GridRegionData(pathfinderConfiguration));
        }
    }
}
