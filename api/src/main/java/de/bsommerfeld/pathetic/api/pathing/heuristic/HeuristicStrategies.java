package de.bsommerfeld.pathetic.api.pathing.heuristic;

/**
 * A util class to provide the predefined {@link IHeuristicStrategy}s of Pathetic.
 *
 * @see LinearHeuristicStrategy
 * @see SquaredHeuristicStrategy
 * @see OctileHeuristicStrategy
 * @see ManhattanHeuristicStrategy
 */
public class HeuristicStrategies {

  public static final IHeuristicStrategy LINEAR = new LinearHeuristicStrategy();
  public static final IHeuristicStrategy SQUARED = new SquaredHeuristicStrategy();

  /**
   * Admissible heuristic for diagonal movement ({@code DIAGONAL_3D}): optimal paths with the
   * default weights, bounded-suboptimal ones when the octile weight is raised above 1.
   *
   * @since 5.6.0
   */
  public static final IHeuristicStrategy OCTILE = new OctileHeuristicStrategy();

  /**
   * Admissible heuristic for axis-aligned movement ({@code VERTICAL_AND_HORIZONTAL}): optimal paths
   * with the default weights, bounded-suboptimal ones when the Manhattan weight is raised above 1.
   *
   * @since 5.6.0
   */
  public static final IHeuristicStrategy MANHATTAN = new ManhattanHeuristicStrategy();

  private HeuristicStrategies() {}
}
