/**
 * Core pathfinding contracts.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.Pathfinder} is the entry point: it takes a start
 * and target {@link de.bsommerfeld.pathetic.api.wrapper.PathPosition} and returns a {@link
 * de.bsommerfeld.pathetic.api.pathing.PathfindingSearch}, the handle to the possibly asynchronous
 * operation (result access, completion callbacks, abort). {@link
 * de.bsommerfeld.pathetic.api.pathing.INeighborStrategy} defines the offset vectors used to expand
 * a node during the search; {@link de.bsommerfeld.pathetic.api.pathing.NeighborStrategies} bundles
 * the cardinal and full-3D presets. {@link
 * de.bsommerfeld.pathetic.api.pathing.PathfindingProgress} carries the start, current, and target
 * positions of a search in flight.
 */
package de.bsommerfeld.pathetic.api.pathing;
