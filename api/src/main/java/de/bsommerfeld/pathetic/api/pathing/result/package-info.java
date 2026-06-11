/**
 * Result types of a pathfinding operation.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult} pairs a terminal {@link
 * de.bsommerfeld.pathetic.api.pathing.result.PathState} (found, failed, fallback, aborted,
 * length-limited, max-iterations-reached) with the {@link
 * de.bsommerfeld.pathetic.api.pathing.result.Path} that was produced; the path is empty when the
 * search did not yield positions. {@code Path} is an iterable, ordered sequence of {@link
 * de.bsommerfeld.pathetic.api.wrapper.PathPosition} from start to target.
 */
package de.bsommerfeld.pathetic.api.pathing.result;
