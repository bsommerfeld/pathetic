package de.bsommerfeld.pathetic.api.pathing.filter;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;

/**
 * A PathFilter is a functional interface that allows customization of the pathfinding process
 * within the {@link Pathfinder}. It provides a mechanism to influence the selection of paths during
 * the pathfinding algorithm's execution.
 *
 * <p>In essence, a PathFilter acts as a whitelist for blocks during the pathfinding process. It
 * evaluates each potential position (or node) on the path and determines whether it is valid or not
 * based on the implemented filter logic. Only the positions that pass the filter are considered
 * valid and included in the final path.
 */
@FunctionalInterface
public interface PathFilter {

  /**
   * Evaluates the given {@link PathValidationContext} to determine if the path is valid. This
   * method is used during the pathfinding process to filter out unwanted paths.
   *
   * @param pathValidationContext The context providing the information necessary to evaluate
   * @return true if the path is valid, false otherwise
   */
  boolean filter(PathValidationContext pathValidationContext);

  /**
   * Cleans up the resources used during the pathfinding process. This method is guaranteed to
   * always be called after pathfinding and should be overridden to ensure proper disposal of
   * resources. Devs can rely on the fact that this method will be invoked post pathfinding to do
   * necessary clean-ups.
   */
  default void cleanup() {}
}
