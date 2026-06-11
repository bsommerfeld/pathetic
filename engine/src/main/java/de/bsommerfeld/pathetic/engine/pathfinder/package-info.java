/**
 * The A* implementation.
 *
 * <p>{@link de.bsommerfeld.pathetic.engine.pathfinder.AbstractPathfinder} owns the
 * algorithm-independent skeleton: request handling, the main loop, hook dispatch, result
 * construction, and the conversion of runtime exceptions into failed results. {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.AStarPathfinder} implements the per-iteration A*
 * work on top of it. {@link de.bsommerfeld.pathetic.engine.pathfinder.PathfindingSession} holds
 * all per-search state: a single hash map assigns each visited grid cell a dense id, and
 * open-set nodes, closed-set membership, and reopen G-costs are id-indexed arrays. Keys are
 * packed relative to the search origin, so absolute world coordinates are unconstrained and the
 * packable range bounds only the exploration radius of one search. Sessions live in a
 * thread-local, which makes a single pathfinder instance safe for concurrent requests.
 */
package de.bsommerfeld.pathetic.engine.pathfinder;
