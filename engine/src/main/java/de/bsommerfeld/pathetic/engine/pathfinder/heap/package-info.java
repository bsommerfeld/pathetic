/**
 * Min-heap contracts for the open set.
 *
 * <p>{@link de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap} defines the operations the
 * search needs (insert-or-update with decrease-key semantics, extract-min, membership and cost
 * lookup) and the shared cost validity check; {@code NaN} costs are rejected because they would
 * silently break the heap ordering. {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.heap.Siftable} and {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.heap.Resizable} are implementation-side contracts
 * for sift operations and capacity growth.
 */
package de.bsommerfeld.pathetic.engine.pathfinder.heap;
