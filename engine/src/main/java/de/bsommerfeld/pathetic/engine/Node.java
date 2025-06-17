package de.bsommerfeld.pathetic.engine;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicMode;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.util.ComputingCache;
import java.util.Objects;

/**
 * Represents a node in the pathfinding graph. Each node contains position information, references
 * to start and target positions, and methods to calculate heuristic values for the A* algorithm.
 *
 * <p>The Node class supports two heuristic calculation modes:
 *
 * <ul>
 *   <li>{@code PERFORMANCE}: Uses squared distances to avoid expensive square root operations,
 *       resulting in faster computations at the cost of some precision.
 *   <li>{@code PRECISION}: Uses true linear distances for more accurate path calculations, which
 *       may be slightly slower due to square root operations.
 * </ul>
 */
public class Node implements Comparable<Node> {

  private final PathPosition position;
  private final PathPosition start;
  private final PathPosition target;
  private final HeuristicWeights heuristicWeights;
  private final HeuristicMode heuristicMode;
  private final int depth;

  private final ComputingCache<Double> squaredHeuristic =
      new ComputingCache<>(this::heuristic);

  private double gCost;
  private Node parent;

  /**
   * Creates a new Node with the specified parameters.
   *
   * @param position The position of this node
   * @param start The start position of the path
   * @param target The target position of the path
   * @param heuristicWeights The weights to apply to different heuristic components
   * @param heuristicMode The mode of heuristic calculation (PERFORMANCE or PRECISION)
   * @param depth The depth of this node in the search tree
   */
  public Node(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights,
      HeuristicMode heuristicMode,
      int depth) {
    this.position = position;
    this.start = start;
    this.target = target;
    this.heuristicWeights = heuristicWeights;
    this.heuristicMode = heuristicMode;
    this.depth = depth;
  }

  /**
   * Creates a new Node with the specified parameters, using PERFORMANCE as the default heuristic
   * mode.
   *
   * @param position The position of this node
   * @param start The start position of the path
   * @param target The target position of the path
   * @param heuristicWeights The weights to apply to different heuristic components
   * @param depth The depth of this node in the search tree
   */
  public Node(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights,
      int depth) {
    this(position, start, target, heuristicWeights, HeuristicMode.PERFORMANCE, depth);
  }

  public PathPosition getPosition() {
    return position;
  }

  public PathPosition getStart() {
    return start;
  }

  public PathPosition getTarget() {
    return target;
  }

  public ComputingCache<Double> getHeuristic() {
    switch (heuristicMode) {
      case PERFORMANCE:
        return squaredHeuristic;
//      case PRECISION:
//        return linearHeuristic;
      default:
        throw new IllegalStateException("Could not find HeuristicMode for " + heuristicMode);
    }
  }

  public Node getParent() {
    return parent;
  }

  public int getDepth() {
    return depth;
  }

  /**
   * Sets the calculated G-cost for this node. This is typically called by the pathfinding algorithm
   * after evaluating costs, including contributions from cost processors.
   *
   * @param gCost The G-cost (accumulated cost from the start node).
   */
  public void setGCost(double gCost) {
    this.gCost = gCost;
  }

  public void setParent(Node parent) {
    this.parent = parent;
  }

  public boolean isTarget() {
    return this.position.getFlooredX() == target.getFlooredX()
        && this.position.getFlooredY() == target.getFlooredY()
        && this.position.getFlooredZ() == target.getFlooredZ();
  }

  /**
   * Calculates the estimated total cost (F-cost) of the path from the start node to the target
   * node, passing through this node. F-cost = G-cost + H-cost.
   *
   * @return The estimated total cost.
   */
  public double getFCost() {
    return getGCost() + getHeuristic().get();
  }

  /**
   * Gets the calculated G-cost (accumulated known cost from the start node) for this node. This
   * value is set by the pathfinding algorithm.
   *
   * @return The G-cost.
   */
  public double getGCost() {
    if (this.parent == null) return 0.0; // G-Cost for start node is 0.
    return this.gCost;
  }

  /**
   * Calculates the heuristic (H-cost) for the A* search algorithm.
   *
   * <p>This heuristic combines multiple metrics to estimate the cost from the current node to the
   * target. All distance calculations are squared to avoid expensive square root operations, which
   * is acceptable for A* as long as the heuristic remains consistent. The final value is a weighted
   * sum of these squared metrics, allowing for fine-tuning of the pathfinder's behavior.
   *
   * @return The composite heuristic value, representing a weighted sum of squared distances.
   */
  private double heuristic() {
    double manhattan = this.position.manhattanDistance(target);
    double manhattanDistanceSquared = manhattan * manhattan;

    double octile = this.position.octileDistance(target);
    double octileDistanceSquared = octile * octile;

    double perpendicularDistanceSquared = calculatePerpendicularDistance();

    double heightDiff = this.position.getFlooredY() - target.getFlooredY();
    double heightDifferenceSquared = heightDiff * heightDiff;

    double manhattanWeight = heuristicWeights.getManhattanWeight();
    double octileWeight = heuristicWeights.getOctileWeight();
    double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
    double heightWeight = heuristicWeights.getHeightWeight();

    return (manhattanDistanceSquared * manhattanWeight)
            + (octileDistanceSquared * octileWeight)
            + (perpendicularDistanceSquared * perpendicularWeight)
            + (heightDifferenceSquared * heightWeight);
  }

  /**
   * Calculates the squared perpendicular distance from the current node's position to the straight
   * line segment defined by the start and target nodes.
   *
   * <p>This metric is used as a component of the main heuristic to penalize nodes that stray far
   * from the direct path. The calculation uses vector mathematics. Squaring the distance avoids
   * costly {@code sqrt} operations and maintains consistency with the other squared metrics in the
   * heuristic.
   *
   * @return The squared perpendicular distance of the current node from the start-target line. If
   *     the start and target are nearly identical, it returns the squared distance to the start
   *     node.
   */
  private double calculatePerpendicularDistance() {
    PathVector currentVec = this.position.toVector();
    PathVector startVec = this.start.toVector();
    PathVector targetVec = this.target.toVector();

    PathVector lineVec = targetVec.subtract(startVec);

    // The squared length of the line vector is calculated using the dot product of the vector with
    // itself, which is equivalent to v.length² and more efficient.
    double lineVecLengthSq = lineVec.dot(lineVec);
    if (lineVecLengthSq < 1e-9) {
      // Avoid division by zero if start and target are almost identical.
      // The "line" is a point, so the perpendicular distance is simply the distance to that point.
      return this.position.distanceSquared(this.start);
    }

    PathVector startToCurrentVec = currentVec.subtract(startVec);

    // The squared length of the cross product of two vectors is equal to the squared area of the
    // parallelogram they form. By dividing this by the squared length of the base vector (lineVec),
    // we get the squared perpendicular height (the distance we're looking for).
    PathVector crossProduct = startToCurrentVec.getCrossProduct(lineVec);
    double crossProductLengthSq = crossProduct.dot(crossProduct);

    return crossProductLengthSq / lineVecLengthSq;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return Objects.equals(position, node.position);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(position);
  }

  @Override
  public int compareTo(Node o) {
    // First compare by F-cost (G-cost + H-cost)
    int fCostComparison = Double.compare(this.getFCost(), o.getFCost());
    if (fCostComparison != 0) {
      return fCostComparison;
    }

    // If F-costs are equal, compare by heuristic value
    int heuristicComparison = Double.compare(this.getHeuristic().get(), o.getHeuristic().get());
    if (heuristicComparison != 0) {
      return heuristicComparison;
    }

    // If heuristics are equal, compare by depth
    return Integer.compare(this.depth, o.depth);
  }
}
