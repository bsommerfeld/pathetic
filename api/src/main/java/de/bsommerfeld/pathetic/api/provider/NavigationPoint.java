package de.bsommerfeld.pathetic.api.provider;

/**
 * Represents information about a position in the pathfinding environment.
 */
public interface NavigationPoint {

  /**
   * Returns whether the position is traversable or not.
   *
   * @return true if the position is traversable, false otherwise
   */
  boolean isTraversable();
}
