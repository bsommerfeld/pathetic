package de.bsommerfeld.pathetic.engine.benchmark.heap.baritone;

import java.util.Arrays;

/**
 * Replicated from
 * https://github.com/cabaletta/baritone/blob/1.19.4/src/main/java/baritone/pathing/calc/openset/BinaryHeapOpenSet.java
 *
 * <p>A binary heap implementation of an open set. This is the one used in the AStarPathFinder.
 *
 * @author leijurv
 */
public final class BinaryHeapOpenSet implements IOpenSet {

  /** The initial capacity of the heap (2^10) */
  private static final int INITIAL_CAPACITY = 1024;

  /** The array backing the heap */
  private PathNode[] array;

  /** The size of the heap */
  private int size;

  public BinaryHeapOpenSet() {
    this(INITIAL_CAPACITY);
  }

  public BinaryHeapOpenSet(int size) {
    this.size = 0;
    this.array = new PathNode[size];
  }

  public int size() {
    return size;
  }

  @Override
  public final void insert(PathNode value) {
    if (size >= array.length - 1) {
      array = Arrays.copyOf(array, array.length << 1);
    }
    size++;
    value.heapPosition = size;
    array[size] = value;
    update(value);
  }

  @Override
  public final void update(PathNode val) {
    int index = val.heapPosition;
    int parentInd = index >>> 1;
    double cost = val.combinedCost;
    PathNode parentNode = array[parentInd];
    while (index > 1 && parentNode.combinedCost > cost) {
      array[index] = parentNode;
      array[parentInd] = val;
      val.heapPosition = parentInd;
      parentNode.heapPosition = index;
      index = parentInd;
      parentInd = index >>> 1;
      parentNode = array[parentInd];
    }
  }

  @Override
  public final boolean isEmpty() {
    return size == 0;
  }

  @Override
  public final PathNode removeLowest() {
    if (size == 0) {
      throw new IllegalStateException("Cannot remove from empty heap");
    }
    PathNode result = array[1];
    PathNode val = array[size];
    array[1] = val;
    val.heapPosition = 1;
    array[size] = null;
    size--;
    result.heapPosition = -1;
    if (size < 2) {
      return result;
    }
    int index = 1;
    int smallerChild = 2;
    double cost = val.combinedCost;
    do {
      PathNode smallerChildNode = array[smallerChild];
      double smallerChildCost = smallerChildNode.combinedCost;
      if (smallerChild < size) {
        PathNode rightChildNode = array[smallerChild + 1];
        double rightChildCost = rightChildNode.combinedCost;
        if (smallerChildCost > rightChildCost) {
          smallerChild++;
          smallerChildCost = rightChildCost;
          smallerChildNode = rightChildNode;
        }
      }
      if (cost <= smallerChildCost) {
        break;
      }
      array[index] = smallerChildNode;
      array[smallerChild] = val;
      val.heapPosition = smallerChild;
      smallerChildNode.heapPosition = index;
      index = smallerChild;
    } while ((smallerChild <<= 1) <= size);
    return result;
  }
}
