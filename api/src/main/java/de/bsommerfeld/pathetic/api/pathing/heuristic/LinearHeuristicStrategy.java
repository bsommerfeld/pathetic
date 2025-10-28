package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.pathing.PathfindingProgress;
import de.bsommerfeld.pathetic.api.pathing.calc.DistanceCalculator;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

/**
 * A linear heuristic strategy combining multiple distance metrics for pathfinding.
 *
 * <p>This implementation evaluates nodes using a weighted sum of:
 *
 * <ul>
 *   <li>Manhattan distance (grid movement)
 *   <li>Octile distance (diagonal-aware approximation)
 *   <li>Perpendicular deviation from the start-target line
 *   <li>Vertical (Y-axis) height difference
 * </ul>
 *
 * <p>All metrics are computed in <strong>linear space</strong> (not squared), making this heuristic
 * admissible and consistent when weights are properly configured.
 *
 * <p>Ideal for 3D grid-based environments with diagonal movement and path-straightness penalties.
 */
public class LinearHeuristicStrategy implements IHeuristicStrategy {

  private static final double EPSILON = 1e-9;
  private static final double D1 = 1.0;
  private static final double D2 = Math.sqrt(2); // ≈1.414
  private static final double D3 = Math.sqrt(3); // ≈1.732

  /**
   * Calculates the perpendicular distance from the current node's position to the straight line
   * segment defined by the start and target nodes.
   *
   * <p>This metric is used as a component of the main heuristic to penalize nodes that stray far
   * from the direct path. The calculation uses vector mathematics and returns a true linear
   * distance via a final square root operation. This is the main difference from the performance
   * variant, which works with squared distances to avoid costly {@code sqrt} operations.
   *
   * <p>Returns the perpendicular distance of the current node from the start-target line. If the
   * start and target are nearly identical, it returns the distance to the start node.
   */
  private final DistanceCalculator<Double> perpendicularCalc =
      progress -> {
        PathPosition start = progress.startPosition();
        PathPosition pos = progress.currentPosition();
        PathPosition target = progress.targetPosition();

        PathVector lineVec = target.toVector().subtract(start.toVector());
        double lineLenSq = lineVec.dot(lineVec);

        if (lineLenSq < EPSILON) {
          return pos.distance(start);
        }

        PathVector toCurr = pos.toVector().subtract(start.toVector());
        PathVector cross = toCurr.getCrossProduct(lineVec);
        double crossLenSq = cross.dot(cross);

        return Math.sqrt(crossLenSq / lineLenSq);
      };

  /**
   * Calculates the Octile distance between this position and target position. Octile distance is a
   * more accurate approximation of diagonal distance in a grid-based environment compared to
   * Manhattan distance.
   */
  private final DistanceCalculator<Double> octileCalc =
      progress -> {
        double dx = Math.abs(progress.currentPosition().getX() - progress.targetPosition().getX());
        double dy = Math.abs(progress.currentPosition().getY() - progress.targetPosition().getY());
        double dz = Math.abs(progress.currentPosition().getZ() - progress.targetPosition().getZ());

        double min = Math.min(Math.min(dx, dy), dz);
        double max = Math.max(Math.max(dx, dy), dz);
        double mid = dx + dy + dz - min - max;

        return (D3 - D2) * min + (D2 - D1) * mid + D1 * max;
      };

  /**
   * Calculates the Manhattan distance between this position and target position. Manhattan distance
   * is the sum of the absolute differences of their coordinates.
   */
  private final DistanceCalculator<Double> manhattanCalc =
      progress -> {
        PathPosition position = progress.currentPosition();
        PathPosition target = progress.targetPosition();

        return (double)
            (Math.abs(position.getFlooredX() - target.getFlooredX())
                + Math.abs(position.getFlooredY() - target.getFlooredY())
                + Math.abs(position.getFlooredZ() - target.getFlooredZ()));
      };

  /** Calculates the height difference between this position to the target position. */
  private final DistanceCalculator<Double> heightCalc =
      progress -> {
        PathPosition position = progress.currentPosition();
        PathPosition target = progress.targetPosition();

        return (double) Math.abs(position.getFlooredY() - target.getFlooredY());
      };

  @Override
  public double calculate(HeuristicContext context) {
    PathfindingProgress progress = context.getPathfindingProgress();
    HeuristicWeights weights = context.heuristicWeights();

    return manhattanCalc.calculate(progress) * weights.getManhattanWeight()
        + octileCalc.calculate(progress) * weights.getOctileWeight()
        + perpendicularCalc.calculate(progress) * weights.getPerpendicularWeight()
        + heightCalc.calculate(progress) * weights.getHeightWeight();
  }

  @Override
  public double calculateTransitionCost(PathPosition from, PathPosition to) {
    return to.distance(from);
  }
}
