package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.pathing.PathfindingProgress;
import de.bsommerfeld.pathetic.api.pathing.calc.DistanceCalculator;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

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
 *
 * @apiNote This heuristic strategy operates on floored coordinates to ensure consistent and
 *     predictable pathfinding behavior in grid-based environments. The floored coordinates
 *     represent the discrete grid cell positions that nodes occupy, which is essential for
 *     maintaining the admissibility and consistency of heuristic calculations in traditional
 *     pathfinding algorithms. Using floored coordinates prevents fractional grid positions from
 *     introducing inaccuracies or inconsistencies in distance calculations that could lead to
 *     suboptimal paths or heuristic violations.
 *     <p>While floored coordinates are preferred for grid-based pathfinding to maintain
 *     mathematical correctness and performance, there may be scenarios where continuous coordinates
 *     provide more accurate path planning. For example, when dealing with environments where nodes
 *     can occupy positions between grid cells or when high precision is required for movement
 *     within a cell, continuous coordinate handling should be considered. However, in standard 3D
 *     grid environments with discrete cell-based navigation, floored coordinates remain the optimal
 *     choice for reliable heuristic evaluation.
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
   * <p>Computed on cell centers (floor+0.5) to avoid asymmetries at cell boundaries.
   *
   * <p>Returns the perpendicular distance of the current node from the start-target line. If the
   * start and target are nearly identical, it returns the distance to the start node.
   */
  private final DistanceCalculator<Double> perpendicularCalc =
      progress -> {
        PathPosition s = progress.startPosition();
        PathPosition c = progress.currentPosition();
        PathPosition t = progress.targetPosition();

        double sx = s.getCenteredX(), sy = s.getCenteredY(), sz = s.getCenteredZ();
        double cx = c.getCenteredX(), cy = c.getCenteredY(), cz = c.getCenteredZ();
        double tx = t.getCenteredX(), ty = t.getCenteredY(), tz = t.getCenteredZ();

        double lineX = tx - sx;
        double lineY = ty - sy;
        double lineZ = tz - sz;
        double lineSq = lineX * lineX + lineY * lineY + lineZ * lineZ;

        if (lineSq < EPSILON) {
          double dx = cx - sx, dy = cy - sy, dz = cz - sz;
          return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        double toX = cx - sx;
        double toY = cy - sy;
        double toZ = cz - sz;

        double crossX = toY * lineZ - toZ * lineY;
        double crossY = toZ * lineX - toX * lineZ;
        double crossZ = toX * lineY - toY * lineX;
        double crossSq = crossX * crossX + crossY * crossY + crossZ * crossZ;

        return Math.sqrt(crossSq / lineSq);
      };

  /**
   * Calculates the <strong>linear Octile distance</strong> between current and target positions.
   *
   * <p>Octile distance is a 3D diagonal heuristic that approximates the true Euclidean distance in
   * grid-based environments with 26-directional movement. It is computed as:
   *
   * <pre>
   * D1 = 1, D2 = √2 ≈ 1.414, D3 = √3 ≈ 1.732
   * octile = (D3 - D2)·min + (D2 - D1)·mid + D1·max
   * </pre>
   *
   * where {@code min}, {@code mid}, {@code max} are the sorted absolute differences in x, y, z.
   *
   * <p>This linear version returns the true distance (not squared), making it admissible when used
   * with appropriate weights. It operates on <strong>floored integer coordinates</strong> for
   * consistency with grid-based pathfinding.
   *
   * <p>More accurate than Manhattan, but slower than squared variants.
   *
   * @see SquaredHeuristicStrategy
   */
  private final DistanceCalculator<Double> octileCalc =
      progress -> {
        int dx =
            Math.abs(
                progress.currentPosition().getFlooredX() - progress.targetPosition().getFlooredX());
        int dy =
            Math.abs(
                progress.currentPosition().getFlooredY() - progress.targetPosition().getFlooredY());
        int dz =
            Math.abs(
                progress.currentPosition().getFlooredZ() - progress.targetPosition().getFlooredZ());

        int min = Math.min(Math.min(dx, dy), dz);
        int max = Math.max(Math.max(dx, dy), dz);
        int mid = dx + dy + dz - min - max;

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
    double dx = to.getCenteredX() - from.getCenteredX();
    double dy = to.getCenteredY() - from.getCenteredY();
    double dz = to.getCenteredZ() - from.getCenteredZ();
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
