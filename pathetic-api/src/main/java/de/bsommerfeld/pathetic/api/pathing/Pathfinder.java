package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.pathing.filter.PathFilter;
import de.bsommerfeld.pathetic.api.pathing.filter.PathFilterStage;
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
   * Tries to find a Path between the two {@link PathPosition}'s provided with the given filters.
   *
   * @param filters A list of {@link PathFilter}'s to apply to the pathfinding process.
   * @return An {@link CompletionStage} that will contain a {@link PathfinderResult}.
   */
  CompletionStage<PathfinderResult> findPath(
      PathPosition start, PathPosition target, List<PathFilter> filters);

  /**
   * Tries to find a Path between the two {@link PathPosition}'s provided with the given
   * filter-containers.
   *
   * <p>Other than the {@link #findPath(PathPosition, PathPosition, List)} method, this method
   * allows for more complex filtering by using {@link PathFilterStage}'s.
   *
   * <p>The filters in the stages will be applied in the order they are provided. If a filter in a
   * stage returns false, the next stage will be checked. Only one stage needs to return true for
   * the path to be considered valid.
   *
   * @api.Note The stages will be checked in the order they are provided. The sharedFilters will be
   *     applied before the stages.
   * @param start The start position of the path.
   * @param target The target position of the path.
   * @param sharedFilters A list of {@link PathFilter}'s, which will be applied to all stages.
   * @param filterStages A list of {@link PathFilterStage}'s to apply to the pathfinding
   * @return An {@link CompletionStage} that will contain a {@link PathfinderResult}.
   */
  CompletionStage<PathfinderResult> findPath(
      PathPosition start,
      PathPosition target,
      List<PathFilter> sharedFilters,
      List<PathFilterStage> filterStages);

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
