package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * An admissible heuristic strategy based solely on the 3D octile distance.
 *
 * <p>The octile distance is the exact cost of the shortest obstacle-free path between two cells
 * when moving along the 26 unit directions with Euclidean step costs, which is precisely how {@link
 * #calculateTransitionCost(PathPosition, PathPosition)} prices a step. The heuristic therefore never
 * overestimates and is consistent, so A* returns <strong>optimal</strong> paths with it. Compared to
 * {@link LinearHeuristicStrategy}, the search may expand more nodes around obstacles, while in open
 * terrain the estimate is exact and the expansion stays close to the path itself.
 *
 * <p>Only {@link HeuristicWeights#getOctileWeight()} is consulted; the other weights are ignored. It
 * acts as an inflation factor {@code w}:
 *
 * <ul>
 *   <li>{@code w = 1} (the default weights) - admissible, optimal paths.
 *   <li>{@code w > 1} - Weighted A*: fewer expansions, and every returned path is at most {@code w}
 *       times as long as the optimum (e.g. {@code 1.2} allows at most 20% detour).
 *   <li>{@code w < 1} - still admissible, but less informed and therefore slower.
 * </ul>
 *
 * <p>Pair it with diagonal movement ({@link
 * de.bsommerfeld.pathetic.api.pathing.NeighborStrategies#DIAGONAL_3D}). With axis-aligned moves only
 * it stays admissible but underestimates badly, which makes the search expand far more nodes; use
 * {@link ManhattanHeuristicStrategy} there instead.
 *
 * <p>Optimality holds as long as every step is a unit offset (as with the bundled {@link
 * de.bsommerfeld.pathetic.api.pathing.NeighborStrategies}) and cost processors never lower a
 * transition below its Euclidean length. Custom neighbor strategies with longer jumps can make the
 * octile estimate exceed the Euclidean step cost.
 *
 * @api.Note Like the other bundled strategies, the heuristic operates on floored coordinates and
 *     the transition cost on cell centers.
 * @since 5.6.0
 */
public class OctileHeuristicStrategy implements IHeuristicStrategy {

  private static final double D1 = 1.0;
  private static final double D2 = Math.sqrt(2);
  private static final double D3 = Math.sqrt(3);

  @Override
  public double calculate(HeuristicContext context) {
    PathPosition position = context.position();
    PathPosition target = context.targetPosition();

    // Differences in double space so far-apart absolute coordinates cannot overflow an int.
    double dx = Math.abs((double) position.getFlooredX() - target.getFlooredX());
    double dy = Math.abs((double) position.getFlooredY() - target.getFlooredY());
    double dz = Math.abs((double) position.getFlooredZ() - target.getFlooredZ());

    double min = Math.min(Math.min(dx, dy), dz);
    double max = Math.max(Math.max(dx, dy), dz);
    double mid = dx + dy + dz - min - max;

    double octile = (D3 - D2) * min + (D2 - D1) * mid + D1 * max;
    return octile * context.heuristicWeights().getOctileWeight();
  }

  @Override
  public double calculateTransitionCost(PathPosition from, PathPosition to) {
    double dx = to.getCenteredX() - from.getCenteredX();
    double dy = to.getCenteredY() - from.getCenteredY();
    double dz = to.getCenteredZ() - from.getCenteredZ();
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
