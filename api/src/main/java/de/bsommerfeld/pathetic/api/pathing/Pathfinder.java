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
   * @return A {@link PathfindingSearch} instance that can be used to configure and retrieve the
   *     result of the pathfinding operation.
   */
  default PathfindingSearch findPath(PathPosition start, PathPosition target) {
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
   * @return A {@link PathfindingSearch} instance that can be used to configure and retrieve the
   *     result of the pathfinding operation.
   */
  PathfindingSearch findPath(PathPosition start, PathPosition target, EnvironmentContext context);

  /**
   * Registers a {@link PathfinderHook} that will be called on every step of the pathfinding
   * process. This can be used to modify the pathfinding process or to collect data.
   *
   * @param hook The hook to register.
   */
  void registerPathfindingHook(PathfinderHook hook);
}
