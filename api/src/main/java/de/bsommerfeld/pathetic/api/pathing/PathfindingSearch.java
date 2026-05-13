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
   * <p>The callback is purely observational. The search stays in its exceptional state afterwards
   * - {@link #resultBlocking()} will still throw and {@link #result()} will still return
   * {@link java.util.Optional#empty()}.
   *
   * @param callback the callback to execute if the pathfinding operation results in an exception
   * @return this PathfindingSearch instance for method chaining
   */
  PathfindingSearch exceptionally(Consumer<Throwable> callback);

  /**
   * Blocks until the pathfinding operation completes and returns the result.
   *
   * <p>If the operation completed exceptionally, this method throws a {@link
   * java.util.concurrent.CompletionException} whose {@link Throwable#getCause() cause} is the
   * original error. External cancellation surfaces as a {@link
   * java.util.concurrent.CancellationException}. Use {@link #result()} if you prefer an empty
   * {@link Optional} over a thrown exception.
   *
   * @return the {@link PathfinderResult} representing the outcome of the pathfinding operation,
   *     including success, failure, or fallback states, along with the generated path.
   * @since 5.4.6
   */
  PathfinderResult resultBlocking();

  /**
   * Retrieves the result of the pathfinding operation if it has completed normally.
   *
   * <p>Returns {@link Optional#empty()} when the operation has not yet completed, was cancelled,
   * or completed exceptionally. Use {@link #resultBlocking()} if you want the exception to
   * propagate instead of being swallowed.
   *
   * @return an {@link Optional} containing the {@link PathfinderResult} on normal completion,
   *     empty otherwise.
   * @since 5.4.6
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
   * @since 5.4.6
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
