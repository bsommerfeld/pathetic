package de.bsommerfeld.pathetic.engine.pathfinder.heap.impl;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.Resizable;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.Siftable;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.NoSuchElementException;

/**
 * A highly optimized, array-backed binary min-heap for A* pathfinding.
 *
 * <p>This implementation provides {@link MinHeap} contract with additional optimizations:
 *
 * <ul>
 *   <li>Guarantees <b>zero object allocations</b> during runtime (hot-path).
 *   <li>Uses primitive arrays for perfect CPU cache locality.
 *   <li>Supports O(log n) {@code decreaseKey} operations via an internal lookup map.
 *   <li>Automatically resizes when capacity is exceeded (see {@link Resizable}).
 *   <li>Uses 1-based indexing internally for efficient parent/child calculations.
 * </ul>
 *
 * <p>Implements {@link Siftable} for heap property maintenance through sift operations.
 *
 * @since 5.4.1
 * @see MinHeap
 * @see Siftable
 * @see Resizable
 */
public class PrimitiveMinHeap implements MinHeap, Siftable, Resizable {

  // Maps packed-coordinate -> current index in the heap arrays.
  // Crucial for O(1) lookup to perform fast decreaseKey.
  private final Long2IntOpenHashMap nodeToIndexMap;
  private long[] nodes; // The packed coordinates (payload)
  private double[] costs; // The F-Costs (priority)
  private int size = 0;

  /**
   * @param initialCapacity The initial size of the heap. Pro-tip: Set this to ~1.5x expected path
   *     length to avoid resizing.
   */
  public PrimitiveMinHeap(int initialCapacity) {
    // 1-based indexing simplifies parent/child math (1 is root)
    this.nodes = new long[initialCapacity + 1];
    this.costs = new double[initialCapacity + 1];

    this.nodeToIndexMap = new Long2IntOpenHashMap(initialCapacity);
    this.nodeToIndexMap.defaultReturnValue(-1); // -1 indicates "not in heap"
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    size = 0;
    nodeToIndexMap.clear();
  }

  @Override
  public boolean contains(long packedNode) {
    return nodeToIndexMap.containsKey(packedNode);
  }

  @Override
  public double cost(long packedNode) {
    int index = nodeToIndexMap.get(packedNode);
    if (index == -1) return Double.MAX_VALUE;
    return costs[index];
  }

  @Override
  public void insertOrUpdate(long packedNode, double cost) {
    int existingIndex = nodeToIndexMap.get(packedNode);

    if (existingIndex != -1) {
      // Node is already in heap -> decreaseKey logic
      if (cost < costs[existingIndex]) {
        costs[existingIndex] = cost;
        siftUp(existingIndex);
      }
    } else {
      // New node -> insert logic
      ensureCapacity();

      size++;
      nodes[size] = packedNode;
      costs[size] = cost;
      nodeToIndexMap.put(packedNode, size);
      siftUp(size);
    }
  }

  @Override
  public long extractMin() {
    if (size == 0) throw new NoSuchElementException();

    long minNode = nodes[1];

    // Remove from lookup map
    nodeToIndexMap.remove(minNode);

    // Move last element to root
    long lastNode = nodes[size];
    double lastCost = costs[size];

    nodes[1] = lastNode;
    costs[1] = lastCost;

    size--;

    if (size > 0) {
      // Update map for the moved node and restore heap property
      nodeToIndexMap.put(lastNode, 1);
      siftDown(1);
    }

    return minNode;
  }

  @Override
  public int capacity() {
    return nodes.length - 1; // -1 because we use 1-based indexing
  }

  @Override
  public void ensureCapacity() {
    // Check if we hit the limit (index == length - 1)
    if (size >= nodes.length - 1) {
      int newCap = nodes.length * 2;
      long[] newNodes = new long[newCap];
      double[] newCosts = new double[newCap];

      // Native copy is extremely fast
      System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
      System.arraycopy(costs, 0, newCosts, 0, costs.length);

      this.nodes = newNodes;
      this.costs = newCosts;
      // Note: Map handles its own resizing automatically
    }
  }

  @Override
  public void siftUp(int index) {
    int current = index;
    long nodeToMove = nodes[current];
    double costToMove = costs[current];

    while (current > 1) {
      int parentIndex = current >> 1; // current / 2
      double parentCost = costs[parentIndex];

      if (costToMove < parentCost) {
        // Swap parent down
        nodes[current] = nodes[parentIndex];
        costs[current] = parentCost;

        // Update map for parent
        nodeToIndexMap.put(nodes[current], current);

        current = parentIndex;
      } else {
        break;
      }
    }

    nodes[current] = nodeToMove;
    costs[current] = costToMove;
    nodeToIndexMap.put(nodeToMove, current);
  }

  @Override
  public void siftDown(int index) {
    int current = index;
    long nodeToMove = nodes[current];
    double costToMove = costs[current];
    int half = size >> 1; // Loop while non-leaf

    while (current <= half) {
      int childIndex = current << 1; // Left child (current * 2)
      double childCost = costs[childIndex];

      int rightIndex = childIndex + 1;

      // Check if right child exists and is smaller than left
      if (rightIndex <= size && costs[rightIndex] < childCost) {
        childIndex = rightIndex;
        childCost = costs[rightIndex];
      }

      if (costToMove > childCost) {
        // Swap child up
        nodes[current] = nodes[childIndex];
        costs[current] = childCost;

        // Update map for child
        nodeToIndexMap.put(nodes[current], current);

        current = childIndex;
      } else {
        break;
      }
    }

    nodes[current] = nodeToMove;
    costs[current] = costToMove;
    nodeToIndexMap.put(nodeToMove, current);
  }
}
