package de.bsommerfeld.pathetic.api.pathing.processing.context;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeCostCalculator;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidator;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

import java.util.Map;

/**
 * Provides context for the evaluation of a single node (PathPosition)
 * during a pathfinding search.
 */
public interface NodeEvaluationContext {
  /**
   * Returns the PathPosition currently being evaluated.
   * This is the potential next step in the path.
   *
   * @return The current PathPosition.
   */
  PathPosition getCurrentPathPosition();

  /**
   * Returns the PathPosition from which the {@link #getCurrentPathPosition() current PathPosition}
   * is being reached. This is the position of the parent A* Node.
   * This will be {@code null} if the {@code currentPathPosition} is the start position
   * being evaluated initially.
   *
   * @return The previous PathPosition in the path segment, or {@code null}.
   */
  PathPosition getPreviousPathPosition();

  /**
   * Returns the depth of the {@link #getCurrentPathPosition() current PathPosition}
   * in the search tree. The start node is typically at depth 0 or 1 depending on convention.
   * This value comes from the engine's internal node representation.
   *
   * @return The depth of the current node.
   */
  int getCurrentNodeDepth();

  /**
   * Returns the heuristic value (H-cost) calculated by the pathfinding engine
   * for the {@link #getCurrentPathPosition() current PathPosition} towards the target.
   * This is the engine's estimate *before* any processor adjustments might
   * influence path choices.
   *
   * @return The engine-calculated heuristic value for the current node.
   */
  double getCurrentNodeHeuristicValue();

  /**
   * Returns the accumulated G-cost (actual known cost from start)
   * up to the {@link #getPreviousPathPosition() previous PathPosition}.
   * This value is derived from {@code getParentEngineNode().getGCost()}.
   * If {@code previousPathPosition} is {@code null} (i.e., current is start), this returns 0.
   *
   * @return The accumulated g-cost to the previous position.
   */
  double getPathCostToPreviousPosition();

  /**
   * Returns the base traversal cost for the transition from the
   * {@link #getPreviousPathPosition() previous PathPosition} to the
   * {@link #getCurrentPathPosition() current PathPosition}.
   * This is typically derived from {@code currentPathPosition.distance(previousPathPosition)}
   * and represents the raw geometric/movement cost *before* any
   * {@code NodeCostCalculator} processors add their contributions for this specific transition.
   * Returns 0 if previousPathPosition is null.
   *
   * @return The base cost of the current transition.
   */
  double getBaseTransitionCost();

  /**
   * Provides access to the overarching {@link SearchContext} for this pathfinding operation.
   *
   * @return The search context.
   */
  SearchContext getSearchContext();

  /**
   * Convenience method to access the pathfinder configuration.
   * Delegates to {@link SearchContext#getPathfinderConfiguration()}.
   */
  default PathfinderConfiguration getPathfinderConfiguration() {
    return getSearchContext().getPathfinderConfiguration();
  }

  /**
   * Convenience method to access the navigation point provider.
   * Delegates to {@link SearchContext#getNavigationPointProvider()}.
   */
  default NavigationPointProvider getNavigationPointProvider() {
    return getSearchContext().getNavigationPointProvider();
  }

  /**
   * Convenience method to access the shared data map for the overall search.
   * Delegates to {@link SearchContext#getSharedData()}.
   */
  default Map<String, Object> getSharedData() {
    return getSearchContext().getSharedData();
  }

  /**
   * Convenience method to access the start PathPosition of the overall search.
   * Delegates to {@link SearchContext#getStartPathPosition()}.
   */
  default PathPosition getStartPathPosition() {
    return getSearchContext().getStartPathPosition();
  }

  /**
   * Convenience method to access the target PathPosition of the overall search.
   * Delegates to {@link SearchContext#getTargetPathPosition()}.
   */
  default PathPosition getTargetPathPosition() {
    return getSearchContext().getTargetPathPosition();
  }
}
