package de.bsommerfeld.pathetic.api.pathing.hook;

/**
 * Interface for hooks that are called during the pathfinding process.
 */
public interface PathfinderHook {

  /**
   * Called on each step of the pathfinding process.
   *
   * @param pathfindingContext the context of the current pathfinding step
   */
  void onPathfindingStep(PathfindingContext pathfindingContext);
}
