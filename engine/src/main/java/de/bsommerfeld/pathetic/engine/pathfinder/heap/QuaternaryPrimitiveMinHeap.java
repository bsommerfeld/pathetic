package de.bsommerfeld.pathetic.engine.pathfinder.heap;

import java.util.Arrays;

/**
 * A quaternary (4-ary) min-heap implementation optimized for pathfinding algorithms.
 *
 * <p>This heap uses primitive arrays for memory efficiency and cache locality, making it
 * particularly suitable for performance-critical pathfinding operations. Each node in the heap can
 * have up to 4 children, which reduces the height of the tree compared to a binary heap, resulting
 * in fewer comparisons during sift operations.
 *
 * <p>The heap maintains three parallel arrays:
 *
 * <ul>
 *   <li>{@code heap}: stores node IDs in heap order
 *   <li>{@code costs}: stores the corresponding costs for each node
 *   <li>{@code idToPos}: provides O(1) lookup from node ID to heap position
 * </ul>
 *
 * <p>This implementation supports efficient decrease-key operations through the {@link
 * #insertOrUpdate(int, double)} method, which is essential for pathfinding algorithms like A* and
 * Dijkstra's algorithm.
 *
 * @since 5.4.2
 */
public class QuaternaryPrimitiveMinHeap {

  /** Array storing node IDs in heap order. */
  private final int[] heap;

  /** Array storing the cost associated with each node in the heap. */
  private final double[] costs;

  /**
   * Index mapping from node ID to position in the heap. A value of -1 indicates the node is not in
   * the heap.
   */
  private final int[] idToPos;

  /** Current number of elements in the heap. */
  private int size = 0;

  /**
   * Constructs a new quaternary min-heap with the specified maximum capacity.
   *
   * @param maxNodes the maximum number of nodes this heap can hold
   * @since 5.4.1
   */
  public QuaternaryPrimitiveMinHeap(int maxNodes) {
    this.heap = new int[maxNodes];
    this.costs = new double[maxNodes];
    this.idToPos = new int[maxNodes];
    // Initialize all positions to -1 to indicate nodes are not in the heap
    Arrays.fill(idToPos, -1);
  }

  /**
   * Inserts a new node or updates an existing node's cost in the heap.
   *
   * <p>If the node is not currently in the heap, it is inserted with the given cost. If the node
   * already exists and the new cost is lower, the node's cost is updated (decrease-key operation).
   * If the new cost is higher or equal, no operation is performed.
   *
   * @param nodeId the unique identifier of the node
   * @param cost the cost associated with the node
   * @since 5.4.1
   */
  public void insertOrUpdate(int nodeId, double cost) {
    int pos = idToPos[nodeId];
    if (pos != -1) {
      // Node is already in the heap
      if (cost < costs[pos]) {
        // Decrease-key operation: update cost and restore heap property
        costs[pos] = cost;
        siftUp(pos);
      }
      // If new cost is higher or equal, we ignore the update
    } else {
      // Node is not in the heap, insert it at the end
      costs[size] = cost;
      heap[size] = nodeId;
      idToPos[nodeId] = size;
      siftUp(size++);
    }
  }

  /**
   * Removes and returns the node with the minimum cost from the heap.
   *
   * <p>This operation takes O(log n) time due to the sift-down operation needed to restore the heap
   * property after removing the root element.
   *
   * @return the ID of the node with the minimum cost
   * @since 5.4.1
   */
  public int extractMin() {
    int minId = heap[0];
    // Mark the extracted node as no longer in the heap
    idToPos[minId] = -1;
    size--;
    if (size > 0) {
      // Move the last element to the root and restore heap property
      heap[0] = heap[size];
      costs[0] = costs[size];
      idToPos[heap[0]] = 0;
      siftDown(0);
    }
    return minId;
  }

  /**
   * Moves a node up the heap until the heap property is restored.
   *
   * <p>This method is used after inserting a new node or decreasing a node's cost. The node at the
   * given index is compared with its parent and swapped if necessary, continuing until the node is
   * in the correct position.
   *
   * <p>In a quaternary heap, the parent of a node at index i is at index (i-1)/4, which is computed
   * efficiently using the unsigned right shift operator {@code >>> 2}.
   *
   * @param index the starting position of the node to sift up
   */
  private void siftUp(int index) {
    int id = heap[index];
    double cost = costs[index];
    while (index > 0) {
      // Calculate parent index using unsigned right shift: (index - 1) / 4
      int parent = (index - 1) >>> 2;
      // If heap property is satisfied, we're done
      if (cost >= costs[parent]) break;

      // Move parent down
      heap[index] = heap[parent];
      costs[index] = costs[parent];
      idToPos[heap[index]] = index;
      index = parent;
    }
    // Place the node in its final position
    heap[index] = id;
    costs[index] = cost;
    idToPos[id] = index;
  }

  /**
   * Moves a node down the heap until the heap property is restored.
   *
   * <p>This method is used after removing the minimum element. The node at the given index is
   * compared with its children and swapped with the smallest child if necessary, continuing until
   * the node is in the correct position.
   *
   * <p>In a quaternary heap, a node at index i has up to 4 children at indices 4*i+1, 4*i+2, 4*i+3,
   * and 4*i+4. The method finds the child with the minimum cost and swaps if needed. The
   * multiplication by 4 is computed efficiently using left shift: {@code index << 2}.
   *
   * @param index the starting position of the node to sift down
   */
  private void siftDown(int index) {
    int id = heap[index];
    double cost = costs[index];
    while (true) {
      // Calculate index of first child: 4 * index + 1
      int firstChild = (index << 2) + 1;
      // If no children exist, we're done
      if (firstChild >= size) break;

      // Find the child with minimum cost among all 4 children
      int minChild = firstChild;
      double minCost = costs[firstChild];
      // Calculate the last valid child index (at most 4 children)
      int lastChild = Math.min(firstChild + 4, size);

      for (int i = firstChild + 1; i < lastChild; i++) {
        if (costs[i] < minCost) {
          minCost = costs[i];
          minChild = i;
        }
      }

      // If heap property is satisfied, we're done
      if (cost <= minCost) break;

      // Move the minimum child up
      heap[index] = heap[minChild];
      costs[index] = minCost;
      idToPos[heap[index]] = index;
      index = minChild;
    }
    // Place the node in its final position
    heap[index] = id;
    costs[index] = cost;
    idToPos[id] = index;
  }
}
