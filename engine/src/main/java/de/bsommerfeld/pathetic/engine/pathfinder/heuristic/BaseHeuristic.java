package de.bsommerfeld.pathetic.engine.pathfinder.heuristic;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

public abstract class BaseHeuristic implements IHeuristic {

  protected final PathPosition position;
  protected final PathPosition start;
  protected final PathPosition target;
  protected final HeuristicWeights heuristicWeights;

  public BaseHeuristic(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights) {
    this.position = position;
    this.start = start;
    this.target = target;
    this.heuristicWeights = heuristicWeights;
  }

  @Override
  public double calculate() {
    return heuristic();
  }

  protected abstract double heuristic();
}
