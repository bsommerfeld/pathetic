package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.filter.PathFilter;
import de.bsommerfeld.pathetic.api.pathing.filter.PathFilterStage;
import de.bsommerfeld.pathetic.api.pathing.filter.PathValidationContext;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.Offset;
import de.bsommerfeld.pathetic.engine.util.ExpiringHashMap;
import de.bsommerfeld.pathetic.engine.util.GridRegionData;
import de.bsommerfeld.pathetic.engine.util.Tuple3;
import java.util.*;
import org.jheaps.tree.FibonacciHeap;

public class AStarPathfinder extends AbstractPathfinder {

  private static final int DEFAULT_GRID_CELL_SIZE = 12;

  /**
   * The grid map used to store the regional examined positions and Bloom filters for each grid
   * region.
   */
  private final Map<Tuple3<Integer>, ExpiringHashMap.Entry<GridRegionData>> gridMap =
      new ExpiringHashMap<>();

  private final NavigationPointProvider navigationPointProvider;

  public AStarPathfinder(
      NavigationPointProvider navigationPointProvider,
      PathfinderConfiguration pathfinderConfiguration) {
    super(pathfinderConfiguration);
    this.navigationPointProvider = navigationPointProvider;
  }

  @Override
  protected void tick(
      PathPosition start,
      PathPosition target,
      Node currentNode,
      Depth depth,
      FibonacciHeap<Double, Node> nodeQueue,
      List<PathFilter> filters,
      List<PathFilterStage> filterStages) {

    evaluateNewNodes(nodeQueue, currentNode, filters, filterStages);
    depth.increment();
  }

  @Override
  protected void cleanup() {
    gridMap.clear();
  }

  private void evaluateNewNodes(
      FibonacciHeap<Double, Node> nodeQueue,
      Node currentNode,
      List<PathFilter> filters,
      List<PathFilterStage> filterStages) {

    Collection<Node> newNodes = fetchValidNeighbours(currentNode, filters, filterStages);

    for (Node newNode : newNodes) {
      // TODO 05.03.2025 b.sommerfeld: why not fCost?
      // TODO 15.05.2025 b.sommerfeld: this piece of code makes this algorithm to a greedy best-first search. fcost is needed
      double nodeCost = newNode.getHeuristic().get();
      nodeQueue.insert(nodeCost, newNode);
    }
  }

  private boolean isNodeValid(
      Node currentNode,
      Node newNode,
      List<PathFilter> filters,
      List<PathFilterStage> filterStages) {
    return !isNodeInvalid(newNode, filters, filterStages);
  }

  private Collection<Node> fetchValidNeighbours(
      Node currentNode, List<PathFilter> filters, List<PathFilterStage> filterStages) {
    Set<Node> newNodes = new HashSet<>(Offset.VERTICAL_AND_HORIZONTAL.getVectors().length);

    for (PathVector vector : Offset.VERTICAL_AND_HORIZONTAL.getVectors()) {
      Node newNode = createNeighbourNode(currentNode, vector);

      if (isNodeValid(currentNode, newNode, filters, filterStages)) {
        newNodes.add(newNode);
      }
    }

    return newNodes;
  }

  private Node createNeighbourNode(Node currentNode, PathVector offset) {
    Node newNode =
        new Node(
            currentNode.getPosition().add(offset),
            currentNode.getStart(),
            currentNode.getTarget(),
            pathfinderConfiguration.getHeuristicWeights(),
            currentNode.getDepth() + 1);
    newNode.setParent(currentNode);
    return newNode;
  }

  /**
   * Checks if the node is invalid. A node is invalid if it is outside the world bounds or is not
   * valid according to the filters.
   */
  private boolean isNodeInvalid(
      Node node, List<PathFilter> filters, List<PathFilterStage> filterStages) {

    int gridX = node.getPosition().getFlooredX() / DEFAULT_GRID_CELL_SIZE;
    int gridY = node.getPosition().getFlooredY() / DEFAULT_GRID_CELL_SIZE;
    int gridZ = node.getPosition().getFlooredZ() / DEFAULT_GRID_CELL_SIZE;

    GridRegionData regionData =
        gridMap
            .computeIfAbsent(
                new Tuple3<>(gridX, gridY, gridZ),
                k -> new ExpiringHashMap.Entry<>(new GridRegionData()))
            .getValue();

    regionData.getRegionalExaminedPositions().add(node.getPosition());

    if (regionData.getBloomFilter().mightContain(node.getPosition())) {
      if (regionData.getRegionalExaminedPositions().contains(node.getPosition())) {
        return true; // Node is invalid if already examined
      }
    } else {
      regionData.getBloomFilter().put(node.getPosition());
      regionData.getRegionalExaminedPositions().add(node.getPosition());
    }

    if (!isWithinWorldBounds(node.getPosition())) {
      return true; // Node is invalid if out of bounds
    }

    boolean filtersPass = doAllFiltersPass(filters, node);
    boolean stagesPass = doAnyFilterStagePass(filterStages, node);

    if (!filtersPass) {
      return true; // Node is invalid if filters fail
    }

    return !stagesPass;
  }

  private boolean doAllFiltersPass(List<PathFilter> filters, Node node) {
    for (PathFilter filter : filters) {
      PathValidationContext context =
          new PathValidationContext(
              node.getPosition(),
              node.getParent() != null ? node.getParent().getPosition() : null,
              node.getStart(),
              node.getTarget(),
              navigationPointProvider);

      if (!filter.filter(context)) {
        return false;
      }
    }
    return true;
  }

  private boolean doAnyFilterStagePass(List<PathFilterStage> filterStages, Node node) {
    if (filterStages.isEmpty()) return true;

    for (PathFilterStage filterStage : filterStages) {
      if (filterStage.filter(
          new PathValidationContext(
              node.getPosition(),
              node.getParent() != null ? node.getParent().getPosition() : null,
              node.getStart(),
              node.getTarget(),
              navigationPointProvider))) {
        return true;
      }
    }
    return false;
  }

  private boolean isWithinWorldBounds(PathPosition position) {
    return position.getPathEnvironment().getMinHeight() < position.getFlooredY()
        && position.getFlooredY() < position.getPathEnvironment().getMaxHeight();
  }
}
