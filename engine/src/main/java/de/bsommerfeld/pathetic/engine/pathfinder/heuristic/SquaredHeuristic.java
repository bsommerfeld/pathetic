package de.bsommerfeld.pathetic.engine.pathfinder.heuristic;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

public class SquaredHeuristic extends BaseHeuristic {

  public SquaredHeuristic(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights) {
    super(position, start, target, heuristicWeights);
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
  @Override
  protected double heuristic() {
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
}
