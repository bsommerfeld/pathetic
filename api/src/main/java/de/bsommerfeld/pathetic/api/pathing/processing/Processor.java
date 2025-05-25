package de.bsommerfeld.pathetic.api.pathing.processing;

import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;

/**
 * Base interface for pathfinding processors.
 * It defines lifecycle methods that can be implemented by concrete processors
 * to initialize or finalize resources or states related to a path search.
 */
public interface Processor { // PathPosition is implied globally for pathetic
  /**
   * Called once at the beginning of a path search operation.
   * Implementations can use this to set up initial state in the
   * {@link SearchContext#getSharedData() shared data map} or perform
   * other prerequisite tasks.
   * <p>
   * The default implementation does nothing.
   *
   * @param context The context for the entire search operation.
   */
  default void initializeSearch(SearchContext context) {
    // Default implementation: no operation
  }

  /**
   * Called once at the end of a path search operation, regardless of whether
   * a path was found or the search was aborted.
   * Implementations can use this for cleanup tasks or final processing.
   * <p>
   * The default implementation does nothing.
   *
   * @param context The context for the entire search operation.
   */
  default void finalizeSearch(SearchContext context) {
    // Default implementation: no operation
  }
}
