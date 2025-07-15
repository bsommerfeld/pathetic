package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * Defines the contract for heuristic strategies used in pathfinding algorithms.
 *
 * <p>A heuristic strategy provides two key functions:
 * <ul>
 *   <li>Computing the heuristic value (estimated cost) from a position to the target</li>
 *   <li>Calculating the actual transition cost between two adjacent positions</li>
 * </ul>
 *
 * <p>The heuristic function is crucial for guiding the A* algorithm towards the target
 * efficiently. It must be admissible (never overestimate the actual cost) to guarantee
 * optimal pathfinding results.
 *
 * @see HeuristicContext
 * @see PathPosition
 * @since 5.3.0
 */
public interface IHeuristicStrategy {

    /**
     * Calculates the heuristic value (estimated cost) from the current position to the target.
     *
     * <p>This method computes an estimate of the remaining cost to reach the target from
     * the current position. The heuristic should be admissible (never overestimate the actual cost) to ensure optimal
     * pathfinding results.
     *
     * <p>Common heuristic functions include:
     * <ul>
     *   <li>Manhattan distance for grid-based movement</li>
     *   <li>Euclidean distance for free movement</li>
     *   <li>Octile distance for diagonal movement</li>
     *   <li>Combined metrics considering elevation and terrain</li>
     * </ul>
     *
     * @param heuristicContext the context containing information about the current position, target position, and other
     *                         relevant pathfinding data
     *
     * @return the estimated cost from the current position to the target, must be non-negative
     *
     * @throws IllegalArgumentException if the heuristic context is invalid
     * @see HeuristicContext
     */
    double calculate(HeuristicContext heuristicContext);

    /**
     * Calculates the actual transition cost between two adjacent positions.
     *
     * <p>This method determines the cost of moving from one position to another.
     * The cost should reflect the actual difficulty or expense of the movement, taking into account factors such as:
     * <ul>
     *   <li>Distance between positions</li>
     *   <li>Terrain difficulty</li>
     *   <li>Elevation changes</li>
     *   <li>Movement penalties</li>
     * </ul>
     *
     * <p>The transition cost is used to calculate the G-cost (actual cost from start)
     * in the A* algorithm.
     *
     * @param from the starting position of the movement
     * @param to   the destination position of the movement
     *
     * @return the cost of moving from the 'from' position to the 'to' position, must be positive for valid movements
     *
     * @throws IllegalArgumentException if either position is null or invalid
     * @see PathPosition
     */
    double calculateTransitionCost(PathPosition from, PathPosition to);
}