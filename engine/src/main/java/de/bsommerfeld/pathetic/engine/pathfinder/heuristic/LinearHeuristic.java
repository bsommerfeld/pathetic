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
   * target. All distance calculations use linear (true) distances instead of squared values, which
   * leads to paths that often appear more direct and intuitive to the human eye. The trade-off is a
   * slight performance overhead due to necessary square root operations, particularly in {@code
   * calculatePerpendicularDistance}. The final value is a weighted sum of these linear metrics,
   * allowing for fine-tuning of the pathfinder's behavior.
   *
   * @return The composite heuristic value, representing a weighted sum of linear distances.
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

    return (manhattanDistance * manhattanWeight)
        + (octileDistance * octileWeight)
        + (perpendicularDistance * perpendicularWeight)
        + (heightDifference * heightWeight);
  }

  /**
   * Calculates the perpendicular distance from the current node's position to the straight line
   * segment defined by the start and target nodes.
   *
   * <p>This metric is used as a component of the main heuristic to penalize nodes that stray far
   * from the direct path. The calculation uses vector mathematics and returns a true linear
   * distance via a final square root operation. This is the main difference from the performance
   * variant, which works with squared distances to avoid costly {@code sqrt} operations.
   *
   * @return The perpendicular distance of the current node from the start-target line. If the start
   *     and target are nearly identical, it returns the distance to the start node.
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
    // In this case, the perpendicular distance is simply the distance to that point.
    if (lineVecLengthSq < 1e-9) {
      return this.position.distance(this.start);
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
    // To get d, we take the square root of the entire expression.
    return Math.sqrt(crossProductLengthSq / lineVecLengthSq);
  }
}
