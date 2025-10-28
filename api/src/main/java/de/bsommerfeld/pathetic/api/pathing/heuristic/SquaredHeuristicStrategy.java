package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.pathing.PathfindingProgress;
import de.bsommerfeld.pathetic.api.pathing.calc.DistanceCalculator;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * A performance-optimized heuristic strategy using squared distance metrics.
 *
 * <p>This implementation evaluates nodes using a weighted sum of <strong>squared</strong>
 * distances:
 *
 * <ul>
 *   <li>Squared Manhattan distance
 *   <li>Squared Octile distance
 *   <li>Squared perpendicular deviation from the start-target line
 *   <li>Squared vertical height difference
 * </ul>
 *
 * <p>By avoiding {@code sqrt} operations and working in squared space, this heuristic is
 * significantly faster than {@link LinearHeuristicStrategy}, while maintaining consistency and
 * admissibility <strong>if weights are tuned appropriately</strong>.
 *
 * <p>Ideal for high-performance pathfinding in large 3D environments.
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
public class SquaredHeuristicStrategy implements IHeuristicStrategy {

  private static final double EPSILON = 1e-9;
  private static final double D1 = 1.0;
  private static final double D2 = Math.sqrt(2);
  private static final double D3 = Math.sqrt(3);

  /**
   * Calculates the <strong>squared perpendicular distance</strong> from the current node to the
   * straight line segment defined by the start and target positions.
   *
   * <p>This metric penalizes nodes that deviate from the direct path between start and target.
   * Unlike the linear version, this implementation returns the <strong>squared distance</strong> to
   * avoid the costly {@link Math#sqrt(double)} operation. The formula used is:
   *
   * <pre>
   * d² = | (current - start) × (target - start) |² / |target - start|²
   * </pre>
   *
   * <p>If the start and target are nearly identical (line length squared < 1e-9), the line
   * degenerates to a point, and the squared Euclidean distance to the start position is returned
   * instead to avoid division by zero.
   *
   * <p>Computed on cell centers (floor+0.5) to avoid asymmetries at cell boundaries.
   *
   * <p>This squared version is mathematically consistent with the linear perpendicular distance
   * when used in a fully squared heuristic, and is significantly faster due to the elimination of
   * the square root.
   *
   * @see #perpendicularCalc
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
          return dx * dx + dy * dy + dz * dz;
        }

        double toX = cx - sx;
        double toY = cy - sy;
        double toZ = cz - sz;

        double crossX = toY * lineZ - toZ * lineY;
        double crossY = toZ * lineX - toX * lineZ;
        double crossZ = toX * lineY - toY * lineX;
        double crossSq = crossX * crossX + crossY * crossY + crossZ * crossZ;

        return crossSq / lineSq;
      };

  /**
   * Calculates the <strong>squared Octile distance</strong> between the current and target
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
   * <p>This implementation returns the <strong>square of the Octile distance</strong>:
   *
   * <pre>
   * result = octile²
   * </pre>
   *
   * <p>This avoids {@code sqrt} operations and ensures consistency within a squared-space
   * heuristic. The squared value grows quadratically with distance, so weights must be tuned lower
   * than in linear heuristics to maintain admissibility.
   *
   * <p>Ideal for high-performance 3D pathfinding with 26-directional movement.
   *
   * @see #octileCalc
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

        double octile = (D3 - D2) * min + (D2 - D1) * mid + D1 * max;
        return octile * octile;
      };

  /**
   * Calculates the <strong>squared Manhattan distance</strong> between the current and target
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
   * <p>The result is the <strong>square of the Manhattan distance</strong>:
   *
   * <pre>
   * result = (manhattan)²
   * </pre>
   *
   * <p>Squaring the sum (rather than summing squares) ensures consistency with the linear Manhattan
   * heuristic. This avoids {@code sqrt} and is computationally cheaper than Euclidean distance,
   * while still providing a valid heuristic when weighted appropriately.
   *
   * <p>Use with reduced weights in squared heuristics to prevent overestimation.
   *
   * @see PathPosition#getFlooredX()
   * @see #manhattanCalc
   */
  private final DistanceCalculator<Double> manhattanCalc =
      progress -> {
        PathPosition c = progress.currentPosition();
        PathPosition t = progress.targetPosition();

        double manhattan =
            Math.abs(c.getFlooredX() - t.getFlooredX())
                + Math.abs(c.getFlooredY() - t.getFlooredY())
                + Math.abs(c.getFlooredZ() - t.getFlooredZ());
        return manhattan * manhattan;
      };

  /**
   * Calculates the <strong>squared vertical height difference</strong> between the current and
   * target positions.
   *
   * <p>This simple metric measures only the Y-axis (vertical) difference using floored block
   * coordinates:
   *
   * <pre>
   * heightDiff = |current.flooredY - target.flooredY|
   * result = (heightDiff)²
   * </pre>
   *
   * <p>Squaring the difference amplifies the penalty for large vertical deviations, which is useful
   * in environments where climbing or falling is costly (e.g. stairs, ladders, or
   * gravity-influenced movement).
   *
   * <p>This metric is extremely cheap to compute and pairs well with horizontal distance
   * heuristics. In squared-space heuristics, it ensures consistency with other squared terms.
   *
   * <p>Weight this higher if vertical movement should be discouraged.
   *
   * @see PathPosition#getFlooredY()
   * @see #heightCalc
   */
  private final DistanceCalculator<Double> heightCalc =
      progress -> {
        double dy =
            progress.currentPosition().getFlooredY() - progress.targetPosition().getFlooredY();
        return dy * dy;
      };

  @Override
  public double calculate(HeuristicContext context) {
    PathfindingProgress p = context.getPathfindingProgress();
    HeuristicWeights w = context.heuristicWeights();

    return manhattanCalc.calculate(p) * w.getManhattanWeight()
        + octileCalc.calculate(p) * w.getOctileWeight()
        + perpendicularCalc.calculate(p) * w.getPerpendicularWeight()
        + heightCalc.calculate(p) * w.getHeightWeight();
  }

  @Override
  public double calculateTransitionCost(PathPosition from, PathPosition to) {
    double dx = to.getCenteredX() - from.getCenteredX();
    double dy = to.getCenteredY() - from.getCenteredY();
    double dz = to.getCenteredZ() - from.getCenteredZ();
    return dx * dx + dy * dy + dz * dz;
  }
}
