package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * An admissible heuristic strategy for axis-aligned movement, based solely on the Manhattan
 * distance.
 *
 * <p>With only the six axis-aligned unit moves (as with {@link
 * de.bsommerfeld.pathetic.api.pathing.NeighborStrategies#VERTICAL_AND_HORIZONTAL}), every step
 * costs {@code 1} and the Manhattan distance is the exact cost of the shortest obstacle-free path.
 * The heuristic therefore never overestimates and is consistent, so A* returns
 * <strong>optimal</strong> paths with it. Use {@link OctileHeuristicStrategy} when diagonal moves
 * are allowed: there the Manhattan distance overestimates.
 *
 * <p>Only {@link HeuristicWeights#getManhattanWeight()} is consulted; the other weights are
 * ignored. It acts as an inflation factor {@code w}:
 *
 * <ul>
 *   <li>{@code w = 1} (the default weights) - admissible, optimal paths.
 *   <li>{@code w > 1} - Weighted A*: fewer expansions, and every returned path is at most {@code w}
 *       times as long as the optimum.
 *   <li>{@code w < 1} - still admissible, but less informed and therefore slower.
 * </ul>
 *
 * <p>Optimality holds as long as cost processors never lower a transition below its Euclidean
 * length.
 *
 * @api.Note Like the other bundled strategies, the heuristic operates on floored coordinates and
 *     the transition cost on cell centers.
 * @since 5.6.0
 */
public class ManhattanHeuristicStrategy implements IHeuristicStrategy {

  @Override
  public double calculate(HeuristicContext context) {
    PathPosition position = context.position();
    PathPosition target = context.targetPosition();

    // Differences in double space so far-apart absolute coordinates cannot overflow an int.
    double manhattan =
        Math.abs((double) position.getFlooredX() - target.getFlooredX())
            + Math.abs((double) position.getFlooredY() - target.getFlooredY())
            + Math.abs((double) position.getFlooredZ() - target.getFlooredZ());
    return manhattan * context.heuristicWeights().getManhattanWeight();
  }

  @Override
  public double calculateTransitionCost(PathPosition from, PathPosition to) {
    double dx = to.getCenteredX() - from.getCenteredX();
    double dy = to.getCenteredY() - from.getCenteredY();
    double dz = to.getCenteredZ() - from.getCenteredZ();
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
