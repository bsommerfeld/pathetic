package de.bsommerfeld.pathetic.engine.pathfinder.heap;

/**
 * Contract for data structures that can dynamically resize their internal storage.
 *
 * @since 5.4.3
 */
public interface Resizable {

  /** Ensures the internal storage has sufficient capacity, resizing if necessary. */
  void ensureCapacity();

  /**
   * Returns the current capacity of the internal storage.
   *
   * @return the total capacity before resizing is needed
   */
  int capacity();
}
