package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

public class HeuristicContext {

    private final PathPosition position;
    private final PathPosition start;
    private final PathPosition target;
    private final HeuristicWeights heuristicWeights;

    public HeuristicContext(PathPosition position, PathPosition startPosition, PathPosition targetPosition, HeuristicWeights heuristicWeights) {
        this.position = position;
        this.start = startPosition;
        this.target = targetPosition;
        this.heuristicWeights = heuristicWeights;
    }

    /**
     * The current to-evaluate position.
     */
    public PathPosition position() {
        return position;
    }

    /**
     * The overall start position of the pathfinding.
     */
    public PathPosition startPosition() {
        return start;
    }

    /**
     * The overall target position of the pathfinding.
     */
    public PathPosition targetPosition() {
        return target;
    }

    /**
     * The heuristic weights used in the pathfinding process.
     */
    public HeuristicWeights heuristicWeights() {
        return heuristicWeights;
    }
}
