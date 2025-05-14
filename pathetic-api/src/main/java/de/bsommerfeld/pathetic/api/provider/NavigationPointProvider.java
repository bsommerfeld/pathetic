package de.bsommerfeld.pathetic.api.provider;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * The NavigationPointProvider interface defines methods for retrieving navigation point data
 * at specific positions within a 3D environment.
 */
public interface NavigationPointProvider {

  /**
   * Gets the navigation point at the given position.
   *
   * @param position the position to get the navigation point for.
   * @return {@link NavigationPoint} the navigation point.
   */
  NavigationPoint getNavigationPoint(PathPosition position);
}
