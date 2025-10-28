package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.pathing.PathfindingProgress;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

public class HeuristicContext {

  private final PathfindingProgress pathfindingProgress;
  private final HeuristicWeights heuristicWeights;

  public HeuristicContext(
      PathPosition position,
      PathPosition startPosition,
      PathPosition targetPosition,
      HeuristicWeights heuristicWeights) {
    this.pathfindingProgress = new PathfindingProgress(startPosition, position, targetPosition);
    this.heuristicWeights = heuristicWeights;
  }

  /** The current to-evaluate position. */
  public PathPosition position() {
    return pathfindingProgress.currentPosition();
  }

  /** The overall start position of the pathfinding. */
  public PathPosition startPosition() {
    return pathfindingProgress.startPosition();
  }

  /** The overall target position of the pathfinding. */
  public PathPosition targetPosition() {
    return pathfindingProgress.targetPosition();
  }

  /** The heuristic weights used in the pathfinding process. */
  public HeuristicWeights heuristicWeights() {
    return heuristicWeights;
  }
}
