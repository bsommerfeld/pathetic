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
 * @api.Note This heuristic strategy operates on floored coordinates to ensure consistent and
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
   * Calculates the <strong>linear perpendicular distance</strong> from the current node to the
   * straight line segment defined by the start and target positions.
   *
   * <p>This metric penalizes nodes that deviate from the direct path between start and target. The
   * formula used is:
   *
   * <pre>
   * d = √[ | (current - start) × (target - start) |² / |target - start|² ]
   * </pre>
   *
   * <p>If the start and target are nearly identical (line length squared < 1e-9), the Euclidean
   * distance to the start position is returned instead to avoid division by zero.
   *
   * <p>Computed on cell centers (floor+0.5) to avoid asymmetries at cell boundaries.
   *
   * <p>This linear version uses {@code sqrt} for true distance, unlike the squared variant which
   * avoids it for performance.
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
   * Calculates the <strong>linear Octile distance</strong> between the current and target
   * positions.
   *
   * <p>Octile distance is a diagonal-aware heuristic for 3D grid movement, providing a more
   * accurate approximation than Manhattan distance when diagonal and "knight-like" moves (1,1,1)
   * are allowed. The linear Octile distance is computed as:
   *
   * <pre>
   * D1 = 1, D2 = √2 ≈ 1.414, D3 = √3 ≈ 1.732
   * octile = (D3 - D2)·min + (D2 - D1)·mid + D1·max
   * </pre>
   *
   * where {@code min}, {@code mid}, and {@code max} are the sorted absolute differences in x, y,
   * and z coordinates.
   *
   * <p>This implementation returns the <strong>true linear distance</strong> (not squared).
   *
   * <p>Ideal for accurate 3D pathfinding with 26-directional movement.
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
   * Calculates the <strong>linear Manhattan distance</strong> between the current and target
   * positions using floored block coordinates.
   *
   * <p>Manhattan distance (also known as taxicab or L1 distance) is the sum of absolute differences
   * in each axis:
   *
   * <pre>
   * manhattan = |Δx| + |Δy| + |Δz|
   * </pre>
   *
   * This version operates on <strong>floored integer coordinates</strong> ({@link
   * PathPosition#getFlooredX()}, etc.), making it suitable for block-based grid environments.
   *
   * <p>The result is the <strong>true linear distance</strong> (not squared).
   *
   * <p>Fast and admissible when used with appropriate weights. Use with higher weights than in
   * squared heuristics.
   *
   * @see PathPosition#getFlooredX()
   * @see #manhattanCalc
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

  /**
   * Calculates the <strong>linear vertical height difference</strong> between the current and
   * target positions.
   *
   * <p>This simple metric measures only the Y-axis (vertical) difference using floored block
   * coordinates:
   *
   * <pre>
   * heightDiff = |current.flooredY - target.flooredY|
   * </pre>
   *
   * <p>Useful in environments where climbing or falling is costly (e.g. stairs, ladders, or
   * gravity-influenced movement).
   *
   * <p>This metric is extremely cheap to compute and pairs well with horizontal distance
   * heuristics. In linear heuristics, it scales naturally with distance.
   *
   * <p>Weight this higher if vertical movement should be discouraged.
   *
   * @see PathPosition#getFlooredY()
   * @see #heightCalc
   */
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
