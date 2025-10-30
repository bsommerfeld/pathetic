package de.bsommerfeld.pathetic.api.pathing.heuristic;

/**
 * A util class to provide the two predefined {@link IHeuristicStrategy}s of Pathetic.
 *
 * @see LinearHeuristicStrategy
 * @see SquaredHeuristicStrategy
 */
public class HeuristicStrategies {

  public static final IHeuristicStrategy LINEAR = new LinearHeuristicStrategy();
  public static final IHeuristicStrategy SQUARED = new SquaredHeuristicStrategy();

  private HeuristicStrategies() {}
}
