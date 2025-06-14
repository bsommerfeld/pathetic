package de.bsommerfeld.pathetic.api.provider;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * The NavigationPointProvider interface defines methods for retrieving navigation point data at
 * specific positions within a 3D environment.
 */
public interface NavigationPointProvider {

  /**
   * Gets the navigation point at the given position.
   *
   * @param position the position to get the navigation point for.
   * @return {@link NavigationPoint} the navigation point.
   */
  default NavigationPoint getNavigationPoint(PathPosition position) {
    return getNavigationPoint(position, null);
  }

  /**
   * Retrieves the navigation point at the specified position within the provided environment
   * context.
   *
   * @param position the position for which the navigation point is requested.
   * @param environmentContext the context of the environment in which to evaluate the navigation
   *     point; can be null if context is not required.
   * @return the navigation point corresponding to the specified position within the given
   *     environment context.
   */
  NavigationPoint getNavigationPoint(PathPosition position, EnvironmentContext environmentContext);
}
