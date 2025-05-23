package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeCostCalculator;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidator;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.Offset;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.NodeEvaluationContextImpl;
import de.bsommerfeld.pathetic.engine.util.ExpiringHashMap;
import de.bsommerfeld.pathetic.engine.util.GridRegionData;
import de.bsommerfeld.pathetic.engine.util.Tuple3;
import java.util.Map;
import org.jheaps.tree.FibonacciHeap;

/**
 * An A* (A-star) pathfinding algorithm implementation. It uses a heuristic to guide the search
 * towards the target and considers the actual cost from the start (G-cost) and the estimated cost
 * to the target (H-cost).
 *
 * <p>This implementation uses a grid-based approach to optimize checking for already visited nodes
 * within regions using Bloom filters and sets.
 */
public class AStarPathfinder extends AbstractPathfinder {

  private static final int DEFAULT_GRID_CELL_SIZE = 12; // Should be configurable if possible

  /**
   * Stores regional data, including sets of examined positions and Bloom filters, to optimize
   * visited node checks. The map keys are grid cell coordinates. Uses an {@link ExpiringHashMap} to
   * manage memory for long-running applications if regions become stale.
   */
  private final Map<Tuple3<Integer>, ExpiringHashMap.Entry<GridRegionData>> visitedRegionGrid =
      new ExpiringHashMap<>();

  public AStarPathfinder(PathfinderConfiguration pathfinderConfiguration) {
    super(pathfinderConfiguration);
  }

  @Override
  protected void processSuccessors(
      PathPosition requestStart,
      PathPosition requestTarget,
      Node currentNode,
      int currentSearchDepth,
      FibonacciHeap<Double, Node> openSet,
      SearchContext searchContext) {

    for (PathVector offset : Offset.VERTICAL_AND_HORIZONTAL.getVectors()) {
      PathPosition neighborPosition = currentNode.getPosition().add(offset);

      GridRegionData regionData = getOrCreateRegionData(neighborPosition);
      boolean alreadyExpanded =
          regionData.getBloomFilter().mightContain(neighborPosition)
              && regionData.getRegionalExaminedPositions().contains(neighborPosition);

      if (alreadyExpanded) {
        // TODO: G-cost comparison for re-opening if this path is cheaper instead of just
        // invalidating.
        continue;
      }

      Node neighborNode =
          new Node(
              neighborPosition,
              requestStart,
              requestTarget,
              pathfinderConfiguration.getHeuristicWeights(),
              currentNode.getDepth() + 1);
      neighborNode.setParent(currentNode);

      NodeEvaluationContext nodeEvalContext =
          new NodeEvaluationContextImpl(searchContext, neighborNode, currentNode);

      boolean isValidByCustomProcessors = true;
      if (this.nodeValidators != null && !this.nodeValidators.isEmpty()) {
        for (NodeValidator validator : this.nodeValidators) {
          if (!validator.isValid(nodeEvalContext)) {
            isValidByCustomProcessors = false;
            break;
          }
        }
      }

      if (!isValidByCustomProcessors) {
        continue;
      }

      double baseTransitionCost = nodeEvalContext.getBaseTransitionCost();
      double accumulatedContributions = 0.0;

      if (this.nodeCostCalculators != null && !this.nodeCostCalculators.isEmpty()) {
        for (NodeCostCalculator costCalculator : this.nodeCostCalculators) {
          Cost contribution = costCalculator.calculateCostContribution(nodeEvalContext);
          if (contribution == null) contribution = Cost.ZERO;
          accumulatedContributions += contribution.getValue();
        }
      }

      double finalTransitionCost = baseTransitionCost + accumulatedContributions;
      if (finalTransitionCost < 0 && !pathfinderConfiguration.areNegativeCostsAllowed()) {
        finalTransitionCost = 0;
      }

      double gCostForNeighbor =
          nodeEvalContext.getPathCostToPreviousPosition() + finalTransitionCost;

      neighborNode.setGCost(gCostForNeighbor);
      openSet.insert(neighborNode.getFCost(), neighborNode);
    }
  }

  /**
   * Retrieves or creates {@link GridRegionData} for the grid cell corresponding to the given
   * position.
   *
   * @param position The {@link PathPosition}.
   * @return The {@link GridRegionData} for that region.
   */
  private GridRegionData getOrCreateRegionData(PathPosition position) {
    int gridX = position.getFlooredX() / DEFAULT_GRID_CELL_SIZE;
    int gridY = position.getFlooredY() / DEFAULT_GRID_CELL_SIZE;
    int gridZ = position.getFlooredZ() / DEFAULT_GRID_CELL_SIZE;

    ExpiringHashMap.Entry<GridRegionData> entry =
        visitedRegionGrid.computeIfAbsent(
            new Tuple3<>(gridX, gridY, gridZ),
            k -> new ExpiringHashMap.Entry<>(new GridRegionData()));
    return entry.getValue();
  }

  @Override
  protected void markNodeAsExpanded(Node node) {
    PathPosition position = node.getPosition();
    GridRegionData regionData = getOrCreateRegionData(position);

    if (!regionData.getBloomFilter().mightContain(position)) {
      regionData.getBloomFilter().put(position);
    }
    regionData.getRegionalExaminedPositions().add(position);
  }

  @Override
  protected void performAlgorithmCleanup() {
    visitedRegionGrid.clear();
  }
}
