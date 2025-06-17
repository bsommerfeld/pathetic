package de.bsommerfeld.pathetic.engine.pathfinder.heuristic;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

public class LinearHeuristic extends BaseHeuristic {

  public LinearHeuristic(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights) {
    super(position, start, target, heuristicWeights);
  }

  @Override
  public double calculate() {
    double manhattanDistance = this.position.manhattanDistance(target);
    double octileDistance = this.position.octileDistance(target);
    double perpendicularDistance = calculatePerpendicularDistance();
    double heightDifference = Math.abs(this.position.getFlooredY() - target.getFlooredY());

    double manhattanWeight = heuristicWeights.getManhattanWeight();
    double octileWeight = heuristicWeights.getOctileWeight();
    double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
    double heightWeight = heuristicWeights.getHeightWeight();

    return (manhattanDistance * manhattanWeight)
        + (octileDistance * octileWeight)
        + (perpendicularDistance * perpendicularWeight)
        + (heightDifference * heightWeight);
  }

  private double calculatePerpendicularDistance() {
    PathVector a = this.position.toVector();
    PathVector b = this.start.toVector();
    PathVector c = this.target.toVector();
    return a.subtract(b).getCrossProduct(c.subtract(b)).length() / c.subtract(b).length();
  }
}
