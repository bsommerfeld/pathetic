package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.concurrent.CompletionStage;

/**
 * A Pathfinder is a class that can find a path between two positions while following a given set of
 * rules.
 */
public interface Pathfinder {

  /**
   * Tries to find a Path between the two {@link PathPosition}s.
   *
   * @return An {@link CompletionStage} that will contain a {@link PathfinderResult}.
   */
  default CompletionStage<PathfinderResult> findPath(PathPosition start, PathPosition target) {
    return findPath(start, target, null);
  }

  /**
   * Tries to find a path between the specified start and target positions within the provided
   * environment context.
   *
   * @param start The starting position for pathfinding.
   * @param target The target position for pathfinding.
   * @param context The environment context that provides additional information for the pathfinding
   *     operation. This parameter can be null if no specific context is required.
   * @return A {@link CompletionStage} containing the {@link PathfinderResult} of the pathfinding
   *     operation, indicating the outcome and the resulting path.
   */
  CompletionStage<PathfinderResult> findPath(
      PathPosition start, PathPosition target, EnvironmentContext context);

  /**
   * Requests all currently running pathfinding operations of this pathfinder instance to abort. The
   * abortion is cooperative and might not be immediate.
   *
   * <p><strong>Scope of Abortion:</strong> This method affects all pathfinding operations that are
   * currently being executed by this specific pathfinder instance. If you need to abort individual
   * operations independently, consider using separate pathfinder instances for each operation.
   *
   * <p><strong>Cooperative Abortion:</strong> The actual termination depends on the pathfinding
   * algorithm's main loop checking the abort flag. The operation will complete with {@link
   * de.bsommerfeld.pathetic.api.pathing.result.PathState#ABORTED} as soon as possible.
   *
   * @see de.bsommerfeld.pathetic.api.pathing.result.PathState#ABORTED
   */
  void abort();

  /**
   * Registers a {@link PathfinderHook} that will be called on every step of the pathfinding
   * process. This can be used to modify the pathfinding process or to collect data.
   *
   * @param hook The hook to register.
   */
  void registerPathfindingHook(PathfinderHook hook);
}
