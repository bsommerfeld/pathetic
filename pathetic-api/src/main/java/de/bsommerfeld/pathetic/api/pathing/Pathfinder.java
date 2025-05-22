package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.List;
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
  CompletionStage<PathfinderResult> findPath(PathPosition start, PathPosition target);

  /**
   * Aborts the running pathfinding process.
   *
   * <p>In this context aborts means that the pathfinding process will be stopped and the result
   * will be {@link PathState#ABORTED}.
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
