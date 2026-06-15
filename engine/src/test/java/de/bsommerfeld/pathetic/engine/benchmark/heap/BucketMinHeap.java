package de.bsommerfeld.pathetic.engine.benchmark.heap;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Dial-style bucket priority queue, implemented to measure whether O(1) decrease-key buys anything
 * over the production d-ary heap for A*-shaped workloads.
 *
 * <p>Costs are quantized into fixed-width buckets ({@code bucket = floor(cost / granularity)}).
 * Each bucket is an intrusive doubly-linked list threaded through per-id {@code next}/{@code prev}
 * arrays, so:
 *
 * <ul>
 *   <li><strong>insert</strong> is O(1): link at the bucket head.
 *   <li><strong>decrease-key</strong> is genuinely O(1): unlink from the old bucket, link into the
 *       new one. No sift, unlike the comparison heaps.
 *   <li><strong>extractMin</strong> is amortized O(1): advance a monotone-ish min pointer to the
 *       first non-empty bucket and unlink its head.
 * </ul>
 *
 * <p><strong>Ordering is only exact up to {@code granularity}.</strong> Within a bucket, nodes come
 * out in LIFO order rather than by exact cost. Substituting this for the engine's heap would
 * therefore change which node A* expands next and can alter expansion count and path quality - it
 * is not a drop-in replacement, which is the whole point of benchmarking it. Keys must also fall in
 * the bounded range {@code [0, maxCost]} the buckets were sized for.
 *
 * <p>This makes it the wrong choice for the general weighted-A* engine, whose {@code CostProcessor}
 * contributions and distance-scaled heuristic produce unbounded, fractional {@code double} f-costs.
 * The microbenchmark measures it at roughly 2-3x the d-ary heap on raw open-set operations, but
 * that speed is bought with bounded ordering error, so it loses on path quality.
 *
 * <p><strong>Where it would be both faster AND exact: Jump Point Search.</strong> On a uniform-cost
 * grid (the regime JPS targets), step costs are small integers - 1 for a cardinal step, a fixed
 * scaled constant for a diagonal - so f-costs are small bounded integers. With {@code granularity}
 * set to that common unit (effectively g=1), every value lands in its own bucket and the LIFO
 * within-bucket order never reorders distinct costs: the ordering error collapses to zero. A JPS
 * pathfinder over uniform terrain could therefore use this bucket queue as its open set and get the
 * 2-3x operation speedup with no loss of optimality. That is the configuration in which swapping
 * the comparison heap for buckets pays off; the general engine is not it.
 *
 * <p>Benchmark-only: lives under test sources and is not part of the engine's public surface.
 */
public final class BucketMinHeap implements MinHeap {

  private final double invGranularity;
  private final int[] bucketHead;
  private int[] next;
  private int[] prev;
  private int[] bucketOf;
  private double[] costOf;
  private int size = 0;
  private int minBucket;

  /**
   * @param initialIdCapacity initial per-id array capacity (dense ids)
   * @param maxCost largest cost the queue must accommodate
   * @param granularity bucket width; smaller = more exact ordering, more buckets to scan
   */
  public BucketMinHeap(int initialIdCapacity, double maxCost, double granularity) {
    this.invGranularity = 1.0 / granularity;
    int numBuckets = (int) Math.ceil(maxCost * invGranularity) + 2;
    this.bucketHead = new int[numBuckets];
    Arrays.fill(bucketHead, -1);
    this.next = new int[initialIdCapacity];
    this.prev = new int[initialIdCapacity];
    this.bucketOf = new int[initialIdCapacity];
    Arrays.fill(bucketOf, -1);
    this.costOf = new double[initialIdCapacity];
    this.minBucket = numBuckets;
  }

  private int bucketIndex(double cost) {
    int b = (int) (cost * invGranularity);
    if (b < 0) return 0;
    if (b >= bucketHead.length) return bucketHead.length - 1;
    return b;
  }

  @Override
  public void insertOrUpdate(long nodeId, double cost) {
    MinHeap.requireOrderableCost(nodeId, cost);
    int id = (int) nodeId;
    ensureIdCapacity(id);
    int existingBucket = bucketOf[id];
    if (existingBucket != -1) {
      if (cost < costOf[id]) {
        unlink(id, existingBucket);
        int b = bucketIndex(cost);
        link(id, b);
        costOf[id] = cost;
        bucketOf[id] = b;
        if (b < minBucket) minBucket = b;
      }
      return;
    }
    int b = bucketIndex(cost);
    link(id, b);
    costOf[id] = cost;
    bucketOf[id] = b;
    if (b < minBucket) minBucket = b;
    size++;
  }

  @Override
  public long extractMin() {
    if (size == 0) throw new NoSuchElementException();
    while (minBucket < bucketHead.length && bucketHead[minBucket] == -1) minBucket++;
    int id = bucketHead[minBucket];
    unlink(id, minBucket);
    bucketOf[id] = -1;
    size--;
    return id;
  }

  private void link(int id, int b) {
    int head = bucketHead[b];
    next[id] = head;
    prev[id] = -1;
    if (head != -1) prev[head] = id;
    bucketHead[b] = id;
  }

  private void unlink(int id, int b) {
    int p = prev[id];
    int n = next[id];
    if (p != -1) next[p] = n;
    else bucketHead[b] = n;
    if (n != -1) prev[n] = p;
  }

  private void ensureIdCapacity(int id) {
    if (id < bucketOf.length) return;
    int newCapacity = Math.max(id + 1, bucketOf.length * 2);
    next = Arrays.copyOf(next, newCapacity);
    prev = Arrays.copyOf(prev, newCapacity);
    costOf = Arrays.copyOf(costOf, newCapacity);
    int[] grownBucketOf = new int[newCapacity];
    Arrays.fill(grownBucketOf, -1);
    System.arraycopy(bucketOf, 0, grownBucketOf, 0, bucketOf.length);
    bucketOf = grownBucketOf;
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
    Arrays.fill(bucketHead, -1);
    Arrays.fill(bucketOf, -1);
    minBucket = bucketHead.length;
  }

  @Override
  public boolean contains(long nodeId) {
    if (nodeId < 0 || nodeId >= bucketOf.length) return false;
    return bucketOf[(int) nodeId] != -1;
  }

  @Override
  public double cost(long nodeId) {
    if (nodeId < 0 || nodeId >= bucketOf.length) return Double.MAX_VALUE;
    return bucketOf[(int) nodeId] == -1 ? Double.MAX_VALUE : costOf[(int) nodeId];
  }
}
