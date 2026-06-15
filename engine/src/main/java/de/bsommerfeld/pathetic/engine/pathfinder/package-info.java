/**
 * The A* implementation.
 *
 * <p>{@link de.bsommerfeld.pathetic.engine.pathfinder.AbstractPathfinder} owns the
 * algorithm-independent skeleton: request handling, the main loop, hook dispatch, result
 * construction, and the conversion of runtime exceptions into failed results. {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.AStarPathfinder} implements the per-iteration A*
 * work on top of it. {@link de.bsommerfeld.pathetic.engine.pathfinder.AStarSearchState} holds
 * all per-search state: the open-set heap plus a single hash map that assigns each visited grid
 * cell a dense id, with open-set nodes, closed-set membership, and reopen G-costs as id-indexed
 * arrays. Keys are packed relative to the search origin, so absolute world coordinates are
 * unconstrained and the packable range bounds only the exploration radius of one search. The
 * state is created as a stack local per request (see {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.SearchState}) and passed explicitly into the
 * template methods, so a single pathfinder instance is safe for concurrent requests without any
 * per-instance mutable state.
 */
package de.bsommerfeld.pathetic.engine.pathfinder;
