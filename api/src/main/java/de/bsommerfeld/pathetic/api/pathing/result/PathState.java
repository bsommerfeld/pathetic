package de.bsommerfeld.pathetic.api.pathing.result;

/**
 * The state of a finished pathfinding process. Finished does not mean successful, it just means
 * that the pathfinding process has ended.
 */
public enum PathState {

  /** The pathfinding process was aborted */
  ABORTED,
  /** The Path was successfully found */
  FOUND,
  /**
   * The Path wasn't found, either it reached its max search depth or it couldn't find more
   * positions
   */
  FAILED,
  /** Signifies that the pathfinder fell back during the pathfinding attempt */
  FALLBACK,
  /** Signifies that the pathfinder reached its length limit */
  LENGTH_LIMITED,
  /** Signifies that the pathfinder reached its iteration limit */
  MAX_ITERATIONS_REACHED
}
