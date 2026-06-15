package de.bsommerfeld.pathetic.api.pathing.hook;

/** Interface for hooks that are called during the pathfinding process. */
public interface PathfinderHook {

  /**
   * Called on each step of the pathfinding process.
   *
   * @param pathfindingContext the context of the current pathfinding step
   */
  void onPathfindingStep(PathfindingContext pathfindingContext);

  /**
   * Called once before the pathfinding loop begins, after the start node has been initialized but
   * before any node is expanded. Useful for one-time setup or for recording the search origin.
   *
   * <p>The supplied context carries the start position as its current position, a depth of zero,
   * and the search-wide {@link PathfindingContext#target()} and {@link
   * PathfindingContext#environmentContext()}. The default implementation does nothing, so existing
   * hooks remain source- and binary-compatible.
   *
   * @param pathfindingContext the context describing the start of the pathfinding process
   */
  default void onPathfindingStart(PathfindingContext pathfindingContext) {}
}
