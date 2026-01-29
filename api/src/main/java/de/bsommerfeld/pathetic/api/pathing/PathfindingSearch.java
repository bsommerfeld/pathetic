package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a pathfinding operation that can be configured with callbacks for different outcomes.
 * This interface provides methods to handle successful results, failures, and exceptions, as well
 * as the ability to abort the operation.
 */
public interface PathfindingSearch {

  /**
   * Executes the given callback if the pathfinding operation results in a successful path or a
   * fallback.
   *
   * @param callback the callback to execute if the pathfinding operation is successful or results
   *     in a fallback
   * @return this PathfindingSearchImpl instance for method chaining
   */
  PathfindingSearch ifPresent(Consumer<PathfinderResult> callback);

  /**
   * Executes the given callback if the pathfinding operation results in a failure, abortion, length
   * limitation, or reaching maximum iterations.
   *
   * @param callback the callback to execute if the pathfinding operation fails, is aborted, reaches
   *     length limitation, or maximum iterations
   * @return this PathfindingSearchImpl instance for method chaining
   */
  PathfindingSearch orElse(Consumer<PathfinderResult> callback);

  /**
   * Executes the given callback if the pathfinding operation results in an exception.
   *
   * @param callback the callback to execute if the pathfinding operation results in an exception
   * @return this PathfindingSearchImpl instance for method chaining
   */
  PathfindingSearch exceptionally(Function<Throwable, PathfinderResult> callback);

  /**
   * Aborts the pathfinding operation if it is still in progress. This method has no effect on
   * already-finished PathfindingSearches.
   *
   * @return true if the operation was successfully aborted, false if it was already finished
   */
  boolean abort();
}
