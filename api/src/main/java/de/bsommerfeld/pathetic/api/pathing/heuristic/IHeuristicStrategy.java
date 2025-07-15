package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

public interface IHeuristicStrategy {

    double calculate(HeuristicContext heuristicContext);

    double calculateTransitionCost(PathPosition from, PathPosition to);
}