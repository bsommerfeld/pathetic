package de.bsommerfeld.pathetic.engine.pathfinder.heap;

/**
 * Contract for min-heap implementations used in pathfinding algorithms.
 *
 * <p>This interface defines the core operations required for efficient pathfinding with A* and
 * similar algorithms. Implementations should provide O(log n) insert/update and extract operations.
 *
 * @since 5.4.3
 */
public interface MinHeap {

  /**
   * Checks if the heap is empty.
   *
   * @return true if the heap contains no elements, false otherwise
   */
  boolean isEmpty();

  /**
   * Returns the number of elements currently in the heap.
   *
   * @return the size of the heap
   */
  int size();

  /** Removes all elements from the heap. */
  void clear();

  /**
   * Checks if a specific node is currently in the heap.
   *
   * @param nodeId the node identifier (e.g., packed coordinate or node ID)
   * @return true if the node is present in the heap, false otherwise
   */
  boolean contains(long nodeId);

  /**
   * Gets the current cost of a node in the heap.
   *
   * @param nodeId the node identifier
   * @return the cost associated with the node, or Double.MAX_VALUE if not present
   */
  double getCost(long nodeId);

  /**
   * Inserts a new node or updates an existing node's cost in the heap.
   *
   * <p>If the node is not currently in the heap, it is inserted with the given cost. If the node
   * already exists and the new cost is lower, the node's cost is updated (decrease-key operation).
   *
   * @param nodeId the unique identifier of the node
   * @param cost the cost associated with the node (typically F-cost for A*)
   */
  void insertOrUpdate(long nodeId, double cost);

  /**
   * Removes and returns the node with the minimum cost from the heap.
   *
   * @return the identifier of the node with the minimum cost
   */
  long extractMin();
}
