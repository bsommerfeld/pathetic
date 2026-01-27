package de.bsommerfeld.pathetic.engine.pathfinder.heap;

/**
 * Contract for heap implementations that use sift operations to maintain heap property.
 *
 * <p>Sifting is the fundamental operation in heap data structures to restore the heap invariant
 * after insertions or deletions.
 *
 * @since 5.4.3
 */
public interface Siftable {

  /**
   * Moves a node up the heap until the heap property is restored.
   *
   * <p>Used after inserting a new node or decreasing a node's cost.
   *
   * @param index the starting position of the node to sift up
   */
  void siftUp(int index);

  /**
   * Moves a node down the heap until the heap property is restored.
   *
   * <p>Used after removing the minimum element or increasing a node's cost.
   *
   * @param index the starting position of the node to sift down
   */
  void siftDown(int index);
}
