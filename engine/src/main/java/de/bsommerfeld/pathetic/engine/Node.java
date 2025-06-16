package de.bsommerfeld.pathetic.engine;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicMode;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.util.ComputingCache;
import java.util.Objects;

/**
 * Represents a node in the pathfinding graph. Each node contains position information,
 * references to start and target positions, and methods to calculate heuristic values
 * for the A* algorithm.
 * 
 * <p>The Node class supports two heuristic calculation modes:
 * <ul>
 *   <li>{@code PERFORMANCE}: Uses squared distances to avoid expensive square root operations,
 *       resulting in faster computations at the cost of some precision.</li>
 *   <li>{@code PRECISION}: Uses true linear distances for more accurate path calculations,
 *       which may be slightly slower due to square root operations.</li>
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
      new ComputingCache<>(this::calculateSquaredHeuristic);
  private final ComputingCache<Double> linearHeuristic =
      new ComputingCache<>(this::calculateLinearHeuristic);

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
   * Creates a new Node with the specified parameters, using PERFORMANCE as the default heuristic mode.
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
      case PRECISION:
        return linearHeuristic;
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
   * Calculates the heuristic (H-cost) for the A* search algorithm in PERFORMANCE mode.
   *
   * <p>This heuristic combines multiple metrics to estimate the cost from the current node to the
   * target. All distance calculations are squared to avoid expensive square root operations, which
   * is acceptable for A* as long as the heuristic remains consistent. The final value is a weighted
   * sum of these squared metrics, allowing for fine-tuning of the pathfinder's behavior.
   *
   * @return The composite heuristic value, representing a weighted sum of squared distances.
   */
  private double calculateSquaredHeuristic() {
    // Calculate squared distances for each metric
    double manhattanDistance = this.position.manhattanDistance(target);
    double manhattanDistanceSquared = manhattanDistance * manhattanDistance;

    double octileDistance = this.position.octileDistance(target);
    double octileDistanceSquared = octileDistance * octileDistance;

    double perpendicularDistanceSquared = calculateSquaredPerpendicularDistance();

    double heightDifference = this.position.getFlooredY() - target.getFlooredY();
    double heightDifferenceSquared = heightDifference * heightDifference;

    // Apply weights to each metric
    return applyWeightsToMetrics(
        manhattanDistanceSquared,
        octileDistanceSquared,
        perpendicularDistanceSquared,
        heightDifferenceSquared);
  }

  /**
   * Calculates the heuristic (H-cost) for the {@code PRECISION} mode.
   *
   * <p>This method uses linear (true) distances instead of squared values, resulting in paths that
   * are often more visually direct and intuitive. The trade-off is a minor performance overhead
   * from square root calculations, primarily in {@link #calculateLinearPerpendicularDistance()}.
   *
   * <p>The heuristic combines several weighted metrics for a comprehensive cost estimation:
   *
   * <ul>
   *   <li><b>Manhattan Distance:</b> Effective for grid-based, non-diagonal movement.
   *   <li><b>Octile Distance:</b> A better estimate for diagonal movement in 3D space.
   *   <li><b>Perpendicular Distance:</b> Penalizes deviation from the direct line between start and
   *       target, promoting straighter paths.
   *   <li><b>Height Difference:</b> Accounts for vertical distance to the target.
   * </ul>
   *
   * @return The composite, linear heuristic value.
   */
  private double calculateLinearHeuristic() {
    // Calculate linear distances for each metric
    double manhattanDistance = this.position.manhattanDistance(target);
    double octileDistance = this.position.octileDistance(target);
    double perpendicularDistance = calculateLinearPerpendicularDistance();
    double heightDifference = Math.abs(this.position.getFlooredY() - target.getFlooredY());

    // Apply weights to each metric
    return applyWeightsToMetrics(
        manhattanDistance,
        octileDistance,
        perpendicularDistance,
        heightDifference);
  }

  /**
   * Applies the configured weights to the distance metrics and returns the weighted sum.
   *
   * @param manhattanDistance The Manhattan distance metric
   * @param octileDistance The Octile distance metric
   * @param perpendicularDistance The perpendicular distance metric
   * @param heightDifference The height difference metric
   * @return The weighted sum of all metrics
   */
  private double applyWeightsToMetrics(
      double manhattanDistance,
      double octileDistance,
      double perpendicularDistance,
      double heightDifference) {

    double manhattanWeight = heuristicWeights.getManhattanWeight();
    double octileWeight = heuristicWeights.getOctileWeight();
    double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
    double heightWeight = heuristicWeights.getHeightWeight();

    return (manhattanDistance * manhattanWeight)
        + (octileDistance * octileWeight)
        + (perpendicularDistance * perpendicularWeight)
        + (heightDifference * heightWeight);
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
  private double calculateSquaredPerpendicularDistance() {
    return calculatePerpendicularDistance(false);
  }

  /**
   * Calculates the true perpendicular distance from the current node to the straight line segment
   * between the start and target nodes.
   *
   * <p>This method is a key component for the precision heuristic. It computes the shortest
   * distance from the current position to the line defined by the start and target. The result is a
   * true distance, obtained via a final {@code Math.sqrt} operation, which differs from the
   * performance-focused approach that uses squared distances to avoid this cost.
   *
   * @return The linear (non-squared) perpendicular distance.
   */
  private double calculateLinearPerpendicularDistance() {
    return calculatePerpendicularDistance(true);
  }

  /**
   * Calculates the perpendicular distance from the current node to the straight line segment
   * between the start and target nodes.
   *
   * @param linear If true, returns the linear (true) distance; if false, returns the squared distance
   * @return The perpendicular distance (linear or squared based on the parameter)
   */
  private double calculatePerpendicularDistance(boolean linear) {
    // Set up vectors for the geometric calculation
    PathVector currentVec = this.position.toVector();
    PathVector startVec = this.start.toVector();
    PathVector targetVec = this.target.toVector();

    PathVector lineVec = targetVec.subtract(startVec);
    double lineVecLengthSq = lineVec.dot(lineVec);

    // If start and target are the same (or very close), the line is a point
    if (lineVecLengthSq < 1e-9) {
      return linear 
          ? this.position.distance(this.start) 
          : this.position.distanceSquared(this.start);
    }

    // Calculate the perpendicular distance
    PathVector startToCurrentVec = currentVec.subtract(startVec);
    PathVector crossProduct = startToCurrentVec.getCrossProduct(lineVec);
    double crossProductLengthSq = crossProduct.dot(crossProduct);

    double result = crossProductLengthSq / lineVecLengthSq;
    return linear ? Math.sqrt(result) : result;
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
