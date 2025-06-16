package de.bsommerfeld.pathetic.engine;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicMode;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.util.ComputingCache;
import java.util.Objects;

public class Node implements Comparable<Node> {

  private final PathPosition position;
  private final PathPosition start;
  private final PathPosition target;
  private final HeuristicWeights heuristicWeights;
  private final HeuristicMode heuristicMode;
  private final int depth;

  private final ComputingCache<Double> squaredHeuristic =
      new ComputingCache<>(this::squaredHeuristic);
  private final ComputingCache<Double> linearHeuristic =
      new ComputingCache<>(this::linearHeuristic);

  private double gCost;
  private Node parent;

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
   * Calculates the heuristic (H-cost) for the A* search algorithm.
   *
   * <p>This heuristic combines multiple metrics to estimate the cost from the current node to the
   * target. All distance calculations are squared to avoid expensive square root operations, which
   * is acceptable for A* as long as the heuristic remains consistent. The final value is a weighted
   * sum of these squared metrics, allowing for fine-tuning of the pathfinder's behavior.
   *
   * @return The composite heuristic value, representing a weighted sum of squared distances.
   */
  private double squaredHeuristic() {
    double manhattan = this.position.manhattanDistance(target);
    double manhattanDistanceSquared = manhattan * manhattan;

    double octile = this.position.octileDistance(target);
    double octileDistanceSquared = octile * octile;

    double perpendicularDistanceSquared = calculateSquaredPerpendicularDistance();

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
  private double calculateSquaredPerpendicularDistance() {
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
  private double linearHeuristic() {
    // Retrieve heuristic weights to control the influence of each metric.
    final double manhattanWeight = heuristicWeights.getManhattanWeight();
    final double octileWeight = heuristicWeights.getOctileWeight();
    final double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
    final double heightWeight = heuristicWeights.getHeightWeight();

    // Calculate distance metrics using true, linear values for higher accuracy.
    final double manhattanDistance = this.position.manhattanDistance(target);
    final double octileDistance = this.position.octileDistance(target);
    final double perpendicularDistance = calculateLinearPerpendicularDistance();
    final double heightDifference = Math.abs(this.position.getFlooredY() - target.getFlooredY());

    // The final heuristic is the weighted sum of all metrics.
    return (manhattanDistance * manhattanWeight)
        + (octileDistance * octileWeight)
        + (perpendicularDistance * perpendicularWeight)
        + (heightDifference * heightWeight);
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
    // Set up vectors for the geometric calculation.
    final PathVector currentVec = this.position.toVector();
    final PathVector startVec = this.start.toVector();
    final PathVector targetVec = this.target.toVector();

    final PathVector lineVec = targetVec.subtract(startVec);
    final double lineVecLengthSq = lineVec.dot(lineVec);

    // If start and target are the same (or very close), the line is a point.
    // The distance is then simply the distance from the current position to the start.
    // A small epsilon (1e-9) is used to avoid floating-point inaccuracies.
    if (lineVecLengthSq < 1e-9) {
      return this.position.distance(this.start);
    }

    // The formula for perpendicular distance 'd' is |(P-A) x (B-A)| / |B-A|,
    // where A is start, B is target, and P is the current position.
    // Calculating with squared lengths and taking the root at the end is more stable.
    final PathVector startToCurrentVec = currentVec.subtract(startVec);
    final PathVector crossProduct = startToCurrentVec.getCrossProduct(lineVec);
    final double crossProductLengthSq = crossProduct.dot(crossProduct);

    return Math.sqrt(crossProductLengthSq / lineVecLengthSq);
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
    int fCostComparison = Double.compare(this.getFCost(), o.getFCost());
    if (fCostComparison != 0) {
      return fCostComparison;
    }
    int heuristicComparison = Double.compare(this.squaredHeuristic.get(), o.squaredHeuristic.get());
    if (heuristicComparison != 0) {
      return heuristicComparison;
    }
    return Integer.compare(this.depth, o.depth);
  }
}
