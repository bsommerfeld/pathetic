package de.bsommerfeld.pathetic.api.pathing.heuristic;

/**
 * Represents the modes of heuristic application, defining different prioritizations or strategies
 * for pathfinding algorithms. This enum is typically used to influence the behavior or preference
 * when calculating paths in environments with various navigational constraints.
 *
 * <ul>
 *   <li>{@code PERFORMANCE}: Optimizes the heuristic calculation for faster computations,
 *       potentially compromising the precision of the resultant path. This mode is suitable for
 *       scenarios where speed is a higher priority than path accuracy.
 *   <li>{@code PRECISION}: Optimizes the heuristic calculation for greater accuracy or realism in
 *       pathfinding, which may result in slower computations. This mode is ideal for applications
 *       where precise navigation is required.
 * </ul>
 */
public enum HeuristicMode {
  PERFORMANCE,
  PRECISION;
}
