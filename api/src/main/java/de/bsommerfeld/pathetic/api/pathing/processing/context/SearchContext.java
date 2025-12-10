package de.bsommerfeld.pathetic.api.pathing.processing.context;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.processing.Processor;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.Map;

/**
 * Provides context for an entire pathfinding search operation. This context is typically created
 * once per search request and passed to processor lifecycle methods (e.g., {@link
 * Processor#initializeSearch(SearchContext)}) and is accessible via the {@link EvaluationContext}.
 */
public interface SearchContext {
  /**
   * Returns the starting PathPosition of the path search.
   *
   * @return The start PathPosition.
   */
  PathPosition getStartPathPosition();

  /**
   * Returns the target (or goal) PathPosition of the path search.
   *
   * @return The target PathPosition.
   */
  PathPosition getTargetPathPosition();

  /**
   * Returns the configuration used for this pathfinding search. Processors can use this to access
   * settings like heuristic weights, max length, etc.
   *
   * @return The PathfinderConfiguration instance.
   */
  PathfinderConfiguration getPathfinderConfiguration();

  /**
   * Returns the NavigationPointProvider configured for the search. Processors can use this to query
   * intrinsic properties of PathPositions or the environment, aiding in validation or cost
   * calculation. This is crucial for understanding traversability or terrain types.
   *
   * @return The NavigationPointProvider instance.
   */
  NavigationPointProvider getNavigationPointProvider();

  /**
   * Returns a mutable map that can be used by {@link Processor}s to share data throughout the
   * lifecycle of this search operation. This map is initialized once per search.
   *
   * @return A modifiable map for sharing data. Keys are typically strings.
   */
  Map<String, Object> getSharedData();

  /**
   * Returns the environment-specific context associated with the pathfinding operation. This can
   * provide additional domain-specific information or behavior needed during the search process.
   *
   * @return The EnvironmentContext instance associated with this search, or {@code null} if no
   *     context is available.
   */
  EnvironmentContext getEnvironmentContext();
}
