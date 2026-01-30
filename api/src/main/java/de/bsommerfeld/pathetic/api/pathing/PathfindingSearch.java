package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import java.util.function.Consumer;

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
   * @return this PathfindingSearch instance for method chaining
   */
  PathfindingSearch ifPresent(Consumer<PathfinderResult> callback);

  /**
   * Executes the given callback if the pathfinding operation results in a failure, abortion, length
   * limitation, or reaching maximum iterations.
   *
   * @param callback the callback to execute if the pathfinding operation fails, is aborted, reaches
   *     length limitation, or maximum iterations
   * @return this PathfindingSearch instance for method chaining
   */
  PathfindingSearch orElse(Consumer<PathfinderResult> callback);

  /**
   * Executes the given callback if the pathfinding operation results in an exception.
   *
   * @param callback the callback to execute if the pathfinding operation results in an exception
   * @return this PathfindingSearch instance for method chaining
   */
  PathfindingSearch exceptionally(Consumer<Throwable> callback);

  /**
   * Aborts the pathfinding operation if it is still in progress.
   *
   * <p>This method uses <b>cooperative cancellation</b>. Instead of abruptly terminating the
   * execution thread, it signals the pathfinder to gracefully stop at the next available
   * opportunity (usually the start of the next iteration). This ensures that internal resources are
   * released and cleanup logic is executed correctly.
   */
  void abort();
}
