package de.bsommerfeld.pathetic.engine.pathfinder.heap.impl;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.Resizable;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.Siftable;
import java.util.Arrays;

/**
 * A quaternary (4-ary) min-heap implementation optimized for pathfinding algorithms.
 *
 * <p>This implementation provides {@link MinHeap} contract using a quaternary tree structure. Each
 * node in the heap can have up to 4 children, which reduces the height of the tree compared to a
 * binary heap, resulting in fewer comparisons during sift operations.
 *
 * <p>The heap maintains three parallel arrays:
 *
 * <ul>
 *   <li>{@code heap}: stores node IDs in heap order
 *   <li>{@code costs}: stores the corresponding costs for each node
 *   <li>{@code idToPos}: provides O(1) lookup from node ID to heap position
 * </ul>
 *
 * <p>This implementation uses 0-based indexing and automatically resizes when capacity is exceeded
 * (see {@link Resizable}). Implements {@link Siftable} for heap property maintenance through sift
 * operations with quaternary parent/child relationships.
 *
 * @since 5.4.2
 * @see MinHeap
 * @see Siftable
 * @see Resizable
 */
public class QuaternaryPrimitiveMinHeap implements MinHeap, Siftable, Resizable {

  /** Initial capacity for the heap when no capacity is specified. */
  private static final int INITIAL_CAPACITY = 1024;

  /** Array storing node IDs in heap order. */
  private long[] heap;

  /** Array storing the cost associated with each node in the heap. */
  private double[] costs;

  /**
   * Index mapping from node ID to position in the heap. A value of -1 indicates the node is not in
   * the heap.
   */
  private int[] idToPos;

  /** Current number of elements in the heap. */
  private int size = 0;

  /**
   * Constructs a new quaternary min-heap with the default initial capacity.
   *
   * @since 5.4.2
   */
  public QuaternaryPrimitiveMinHeap() {
    this(INITIAL_CAPACITY);
  }

  /**
   * Constructs a new quaternary min-heap with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity for the heap
   * @since 5.4.1
   */
  public QuaternaryPrimitiveMinHeap(int initialCapacity) {
    this.heap = new long[initialCapacity];
    this.costs = new double[initialCapacity];
    this.idToPos = new int[initialCapacity];
    // Initialize all positions to -1 to indicate nodes are not in the heap
    Arrays.fill(idToPos, -1);
  }

  @Override
  public void insertOrUpdate(long nodeId, double cost) {
    MinHeap.requireOrderableCost(nodeId, cost);

    int nodeIdInt = (int) nodeId;
    ensureNodeIdCapacity(nodeIdInt);
    int pos = idToPos[nodeIdInt];
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
      ensureHeapCapacity();
      costs[size] = cost;
      heap[size] = nodeId;
      idToPos[nodeIdInt] = size;
      siftUp(size++);
    }
  }

  /**
   * Ensures that the idToPos array has sufficient capacity for the given node ID.
   *
   * @param nodeId the node ID that needs to be accommodated
   */
  private void ensureNodeIdCapacity(int nodeId) {
    if (nodeId >= idToPos.length) {
      int newCapacity = Math.max(nodeId + 1, idToPos.length * 2);
      int[] newIdToPos = new int[newCapacity];
      Arrays.fill(newIdToPos, -1);
      System.arraycopy(idToPos, 0, newIdToPos, 0, idToPos.length);
      idToPos = newIdToPos;
    }
  }

  @Override
  public int capacity() {
    return heap.length;
  }

  @Override
  public void ensureCapacity() {
    ensureHeapCapacity();
  }

  /** Ensures that the heap and costs arrays have sufficient capacity for adding a new element. */
  private void ensureHeapCapacity() {
    if (size >= heap.length) {
      int newCapacity = Math.max(heap.length * 2, 16);
      heap = Arrays.copyOf(heap, newCapacity);
      costs = Arrays.copyOf(costs, newCapacity);
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(idToPos, -1);
  }

  @Override
  public boolean contains(long nodeId) {
    int nodeIdInt = (int) nodeId;
    return nodeIdInt < idToPos.length && idToPos[nodeIdInt] != -1;
  }

  @Override
  public double cost(long nodeId) {
    int nodeIdInt = (int) nodeId;
    if (nodeIdInt >= idToPos.length) return Double.MAX_VALUE;
    int pos = idToPos[nodeIdInt];
    return pos == -1 ? Double.MAX_VALUE : costs[pos];
  }

  /**
   * Checks if the heap is empty.
   *
   * @return true if the heap contains no elements, false otherwise
   * @since 5.4.2
   */
  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public long extractMin() {
    long minId = heap[0];
    // Mark the extracted node as no longer in the heap
    idToPos[(int) minId] = -1;
    size--;
    if (size > 0) {
      // Move the last element to the root and restore heap property
      heap[0] = heap[size];
      costs[0] = costs[size];
      idToPos[(int) heap[0]] = 0;
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
  @Override
  public void siftUp(int index) {
    long id = heap[index];
    double cost = costs[index];
    while (index > 0) {
      // Calculate parent index using unsigned right shift: (index - 1) / 4
      int parent = (index - 1) >>> 2;
      // If heap property is satisfied, we're done
      if (cost >= costs[parent]) break;

      // Move parent down
      heap[index] = heap[parent];
      costs[index] = costs[parent];
      idToPos[(int) heap[index]] = index;
      index = parent;
    }
    // Place the node in its final position
    heap[index] = id;
    costs[index] = cost;
    idToPos[(int) id] = index;
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
  @Override
  public void siftDown(int index) {
    long id = heap[index];
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
      idToPos[(int) heap[index]] = index;
      index = minChild;
    }
    // Place the node in its final position
    heap[index] = id;
    costs[index] = cost;
    idToPos[(int) id] = index;
  }
}
