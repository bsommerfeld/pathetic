/**
 * Array-backed primitive min-heap implementations.
 *
 * <p>{@link de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.QuaternaryPrimitiveMinHeap} is
 * the heap the engine uses: 4-ary, three parallel primitive arrays, position tracking by plain
 * array index. It requires small, dense, non-negative int ids (the session's dense node ids) and
 * rejects sparse keys. {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.PrimitiveMinHeap} is the binary
 * predecessor; it accepts arbitrary 64-bit keys via an internal hash map and remains for callers
 * that need sparse keys. Zero allocations in the hot paths of both heaps is a load-bearing
 * invariant.
 */
package de.bsommerfeld.pathetic.engine.pathfinder.heap.impl;
