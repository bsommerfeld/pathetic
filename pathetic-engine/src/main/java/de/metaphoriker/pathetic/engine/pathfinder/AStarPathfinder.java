package de.metaphoriker.pathetic.engine.pathfinder;

import de.metaphoriker.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.PathFilterStage;
import de.metaphoriker.pathetic.api.pathing.filter.PathValidationContext;
import de.metaphoriker.pathetic.api.provider.NavigationPointProvider;
import de.metaphoriker.pathetic.api.wrapper.Depth;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;
import de.metaphoriker.pathetic.api.wrapper.PathVector;
import de.metaphoriker.pathetic.engine.Node;
import de.metaphoriker.pathetic.engine.Offset;
import de.metaphoriker.pathetic.engine.util.ExpiringHashMap;
import de.metaphoriker.pathetic.engine.util.GridRegionData;
import de.metaphoriker.pathetic.engine.util.Tuple3;
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
      double nodeCost = newNode.getHeuristic().get();
      nodeQueue.insert(nodeCost, newNode);
    }
  }

  private boolean isNodeValid(
      Node currentNode,
      Node newNode,
      List<PathFilter> filters,
      List<PathFilterStage> filterStages) {
    if (isNodeInvalid(newNode, filters, filterStages)) return false;
    /*
     * If it is not a diagonal move and survived #isNodeInvalid,
     * then it's definitely valid to this point.
     */
    if (!isDiagonalMove(currentNode, newNode)) return true;
    return isReachable(currentNode, newNode, filters, filterStages);
  }

  private boolean isDiagonalMove(Node from, Node to) {
    int xDifference = Math.abs(from.getPosition().getFlooredX() - to.getPosition().getFlooredX());
    int zDifference = Math.abs(from.getPosition().getFlooredZ() - to.getPosition().getFlooredZ());

    return xDifference != 0 && zDifference != 0;
  }

  /**
   * Returns whether the diagonal jump is possible by checking if the adjacent nodes are passable or
   * not. With adjacent nodes are the shared overlapping neighbours meant.
   */
  // TODO 05.03.2025 b.sommerfeld: has some validation problems and is a bit too complex with a
  //  double loop. can be simplified
  private boolean isReachable(
      Node from, Node to, List<PathFilter> filters, List<PathFilterStage> filterStages) {
    boolean hasYDifference = to.getPosition().getFlooredY() != from.getPosition().getFlooredY();
    PathVector[] offsets = Offset.VERTICAL_AND_HORIZONTAL.getVectors();

    for (PathVector vector1 : offsets) {
      Node neighbour1 = createNeighbourNode(from, vector1);
      for (PathVector vector2 : offsets) {
        Node neighbour2 = createNeighbourNode(to, vector2);
        if (neighbour1.getPosition().equals(neighbour2.getPosition())) {

          boolean heightDifferencePassable = true;

          /*
           * if it has a Y difference, we also need to check the nodes above or below,
           *  depending on the Y difference
           */
          if (hasYDifference) {
            heightDifferencePassable =
                isHeightDifferencePassable(from, to, vector1, filters, filterStages);
          }

          if (doAllFiltersPass(filters, neighbour1)
              && doAnyFilterStagePass(filterStages, neighbour1)
              && heightDifferencePassable) return true;
        }
      }
    }

    return false;
  }

  private boolean isHeightDifferencePassable(
      Node from,
      Node to,
      PathVector vector1,
      List<PathFilter> filters,
      List<PathFilterStage> filterStages) {
    int yDifference = to.getPosition().getFlooredY() - from.getPosition().getFlooredY();
    Node neighbour3 = createNeighbourNode(from, vector1.add(new PathVector(0, yDifference, 0)));
    return doAllFiltersPass(filters, neighbour3) && doAnyFilterStagePass(filterStages, neighbour3);
  }

  private Collection<Node> fetchValidNeighbours(
      Node currentNode, List<PathFilter> filters, List<PathFilterStage> filterStages) {
    Set<Node> newNodes = new HashSet<>(Offset.MERGED.getVectors().length);

    for (PathVector vector : Offset.MERGED.getVectors()) {
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
