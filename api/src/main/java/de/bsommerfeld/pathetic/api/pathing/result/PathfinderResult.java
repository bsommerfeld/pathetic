package de.bsommerfeld.pathetic.api.pathing.result;

/**
 * The result of a pathfinding operation.
 *
 * <p>Pathfinding results are used to encapsulate the outcome of a pathfinding operation. They provide
 * information about the success of the operation and the resulting path.
 */
public interface PathfinderResult {

  /**
   * Returns whether the pathfinding was successful.
   *
   * @return true if the pathfinding was successful
   */
  boolean successful();

  /**
   * Whether the pathfinder has failed to reach its target. This includes {@link PathState#FAILED},
   * {@link PathState#LENGTH_LIMITED}, {@link PathState#MAX_ITERATIONS_REACHED} and {@link
   * PathState#FALLBACK}
   *
   * @return Whether the pathfinder has failed to reach its target
   */
  boolean hasFailed();

  /**
   * Whether a pathfinder has resulted in a fallback.
   *
   * @return Whether a pathfinder has resulted in a fallback
   */
  boolean hasFallenBack();

  /**
   * Returns the state of the pathfinding.
   *
   * @return The {@link PathState}
   */
  PathState getPathState();

  /**
   * Returns the found {@link Path} regardless if successful or not. The path is empty if the
   * pathfinding failed.
   *
   * @return The found {@link Path}
   */
  Path getPath();
}
