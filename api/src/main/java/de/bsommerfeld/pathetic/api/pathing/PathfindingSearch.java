package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a pathfinding operation that can be configured with callbacks for different outcomes.
 * This interface provides methods to handle successful results, failures, and exceptions, as well
 * as the ability to abort the operation.
 *
 * @since 5.4.4
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
   * Blocks until the pathfinding operation completes and returns the result.
   *
   * <p>This method waits for the pathfinding process to finish and retrieves the final outcome,
   * encapsulated as a {@link PathfinderResult}. It ensures that the calling thread is blocked until
   * the result is ready, making it suitable for scenarios where synchronous behavior is required.
   *
   * @return the {@link PathfinderResult} representing the outcome of the pathfinding operation,
   *     including success, failure, or fallback states, along with the generated path.
   */
  PathfinderResult resultBlocking();

  /**
   * Retrieves the result of the pathfinding operation if it has completed.
   *
   * <p>This method returns an {@link Optional}, which may contain the {@link PathfinderResult} if
   * the pathfinding process has finished. The result encapsulates the outcome of the operation,
   * which could be successful, failed, or a fallback, along with the potential generated path. If
   * the pathfinding has not completed or there is no result available, an empty {@link Optional} is
   * returned.
   *
   * @return an {@link Optional} containing the {@link PathfinderResult} if the pathfinding
   *     operation has completed, or an empty {@link Optional} if the result is not yet available or
   *     the operation did not complete.
   */
  Optional<PathfinderResult> result();

  /**
   * Checks if the pathfinding operation has completed.
   *
   * <p>This method returns a boolean indicating whether the pathfinding process has finished.
   * Completion could mean success, failure, or the operation being aborted. It allows determining
   * if the operation's result can be retrieved or further actions should be performed based on its
   * state.
   *
   * @return true if the pathfinding operation is complete; false otherwise.
   */
  boolean done();

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
