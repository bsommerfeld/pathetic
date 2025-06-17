package de.bsommerfeld.pathetic.engine.pathfinder.heuristic;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

public class LinearHeuristic extends BaseHeuristic {

  public LinearHeuristic(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights) {
    super(position, start, target, heuristicWeights);
  }

  /**
   * Calculates the heuristic (H-cost) for the A* search algorithm in PRECISION mode.
   *
   * <p>This heuristic combines multiple metrics to estimate the cost from the current node to the
   * target. While this is the PRECISION mode, we still use scaled distances to ensure consistency
   * with the A* algorithm requirements. The final value is a weighted sum of these scaled metrics,
   * allowing for fine-tuning of the pathfinder's behavior while maintaining consistency.
   *
   * <p>Note: To ensure the heuristic is consistent (satisfies the triangle inequality), we scale
   * the distances and avoid taking square roots in the perpendicular distance calculation.
   *
   * @return The composite heuristic value, representing a weighted sum of scaled distances.
   */
  @Override
  protected double heuristic() {
    final double manhattanWeight = heuristicWeights.getManhattanWeight();
    final double octileWeight = heuristicWeights.getOctileWeight();
    final double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
    final double heightWeight = heuristicWeights.getHeightWeight();

    final double manhattanDistance = this.position.manhattanDistance(target);
    final double octileDistance = this.position.octileDistance(target);
    final double perpendicularDistance = calculatePerpendicularDistance();
    final double heightDifference = Math.abs(this.position.getFlooredY() - target.getFlooredY());

    // Scale the distances to ensure consistency
    final double manhattanDistanceScaled = manhattanDistance * 0.5;
    final double octileDistanceScaled = octileDistance * 0.5;
    final double perpendicularDistanceScaled = perpendicularDistance * 0.5;
    final double heightDifferenceScaled = heightDifference * 0.5;

    return (manhattanDistanceScaled * manhattanWeight)
        + (octileDistanceScaled * octileWeight)
        + (perpendicularDistanceScaled * perpendicularWeight)
        + (heightDifferenceScaled * heightWeight);
  }

  /**
   * Calculates the squared perpendicular distance from the current node's position to the straight line
   * segment defined by the start and target nodes.
   *
   * <p>This metric is used as a component of the main heuristic to penalize nodes that stray far
   * from the direct path. The calculation uses vector mathematics. To ensure consistency with the
   * A* algorithm requirements, we avoid taking the square root and work with squared distances,
   * similar to the performance variant.
   *
   * @return The squared perpendicular distance of the current node from the start-target line. If the start
   *     and target are nearly identical, it returns the squared distance to the start node.
   */
  private double calculatePerpendicularDistance() {
    // Create vectors for the involved positions
    final PathVector currentVec = this.position.toVector();
    final PathVector startVec = this.start.toVector();
    final PathVector targetVec = this.target.toVector();

    // Vector representing the line from start to target
    final PathVector lineVec = targetVec.subtract(startVec);

    // The squared length of the line vector is calculated using the dot product of the vector with
    // itself, which is equivalent to v.length² and more efficient.
    final double lineVecLengthSq = lineVec.dot(lineVec);

    // Edge case: If start and target are almost identical, the "line" is a point.
    // In this case, the perpendicular distance is simply the squared distance to that point.
    if (lineVecLengthSq < 1e-9) {
      return this.position.distanceSquared(this.start);
    }

    // Vector from start point to current position
    final PathVector startToCurrentVec = currentVec.subtract(startVec);

    // The cross product gives a vector perpendicular to the plane spanned by startToCurrentVec
    // and lineVec. Its length is proportional to the area of the parallelogram formed by the
    // vectors.
    final PathVector crossProduct = startToCurrentVec.getCrossProduct(lineVec);
    final double crossProductLengthSq = crossProduct.dot(crossProduct);

    // The formula for perpendicular distance d is: d = |startToCurrentVec x lineVec| / |lineVec|
    // Since we have squared lengths, the formula becomes: d² = crossProductLengthSq /
    // lineVecLengthSq
    // We don't take the square root to maintain consistency with the movement costs
    return crossProductLengthSq / lineVecLengthSq;
  }
}
