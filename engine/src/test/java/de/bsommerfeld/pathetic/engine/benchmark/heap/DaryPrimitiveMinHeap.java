package de.bsommerfeld.pathetic.engine.benchmark.heap;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Power-of-two d-ary primitive min-heap, used only to A/B the branching factor against the
 * production {@link de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.QuaternaryPrimitiveMinHeap}
 * (which is the d=4 case). Mirrors that heap's three-parallel-array layout and dense-id contract;
 * the only difference is that the fan-out is configurable via a shift amount (2 -> 4 children,
 * 3 -> 8 children, ...).
 *
 * <p>Benchmark-only: lives under test sources and is not part of the engine's public surface.
 */
public final class DaryPrimitiveMinHeap implements MinHeap {

  private final int shift;
  private int[] heap;
  private double[] costs;
  private int[] idToPos;
  private int size = 0;

  /**
   * @param initialCapacity initial array capacity
   * @param childrenPerNode branching factor; must be a power of two (4, 8, 16, ...)
   */
  public DaryPrimitiveMinHeap(int initialCapacity, int childrenPerNode) {
    if (Integer.bitCount(childrenPerNode) != 1 || childrenPerNode < 2) {
      throw new IllegalArgumentException("childrenPerNode must be a power of two >= 2");
    }
    this.shift = Integer.numberOfTrailingZeros(childrenPerNode);
    this.heap = new int[initialCapacity];
    this.costs = new double[initialCapacity];
    this.idToPos = new int[initialCapacity];
    Arrays.fill(idToPos, -1);
  }

  @Override
  public void insertOrUpdate(long nodeId, double cost) {
    MinHeap.requireOrderableCost(nodeId, cost);
    int id = (int) nodeId;
    ensureIdCapacity(id);
    int pos = idToPos[id];
    if (pos != -1) {
      if (cost < costs[pos]) {
        costs[pos] = cost;
        siftUp(pos);
      }
      return;
    }
    ensureHeapCapacity();
    costs[size] = cost;
    heap[size] = id;
    idToPos[id] = size;
    siftUp(size++);
  }

  @Override
  public long extractMin() {
    if (size == 0) throw new NoSuchElementException();
    int minId = heap[0];
    idToPos[minId] = -1;
    size--;
    if (size > 0) {
      heap[0] = heap[size];
      costs[0] = costs[size];
      idToPos[heap[0]] = 0;
      siftDown(0);
    }
    return minId;
  }

  private void siftUp(int index) {
    int id = heap[index];
    double cost = costs[index];
    while (index > 0) {
      int parent = (index - 1) >>> shift;
      if (cost >= costs[parent]) break;
      heap[index] = heap[parent];
      costs[index] = costs[parent];
      idToPos[heap[index]] = index;
      index = parent;
    }
    heap[index] = id;
    costs[index] = cost;
    idToPos[id] = index;
  }

  private void siftDown(int index) {
    int id = heap[index];
    double cost = costs[index];
    while (true) {
      int firstChild = (index << shift) + 1;
      if (firstChild >= size) break;
      int minChild = firstChild;
      double minCost = costs[firstChild];
      int lastChild = Math.min(firstChild + (1 << shift), size);
      for (int i = firstChild + 1; i < lastChild; i++) {
        if (costs[i] < minCost) {
          minCost = costs[i];
          minChild = i;
        }
      }
      if (cost <= minCost) break;
      heap[index] = heap[minChild];
      costs[index] = minCost;
      idToPos[heap[index]] = index;
      index = minChild;
    }
    heap[index] = id;
    costs[index] = cost;
    idToPos[id] = index;
  }

  private void ensureIdCapacity(int id) {
    if (id < idToPos.length) return;
    int newCapacity = Math.max(id + 1, idToPos.length * 2);
    int[] grown = new int[newCapacity];
    Arrays.fill(grown, -1);
    System.arraycopy(idToPos, 0, grown, 0, idToPos.length);
    idToPos = grown;
  }

  private void ensureHeapCapacity() {
    if (size >= heap.length) {
      int newCapacity = Math.max(heap.length * 2, 16);
      heap = Arrays.copyOf(heap, newCapacity);
      costs = Arrays.copyOf(costs, newCapacity);
    }
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
    Arrays.fill(idToPos, -1);
  }

  @Override
  public boolean contains(long nodeId) {
    if (nodeId < 0 || nodeId >= idToPos.length) return false;
    return idToPos[(int) nodeId] != -1;
  }

  @Override
  public double cost(long nodeId) {
    if (nodeId < 0 || nodeId >= idToPos.length) return Double.MAX_VALUE;
    int pos = idToPos[(int) nodeId];
    return pos == -1 ? Double.MAX_VALUE : costs[pos];
  }
}
