package de.bsommerfeld.pathetic.engine.benchmark.heap;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.QuaternaryPrimitiveMinHeap;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Lightweight, non-JMH timing + precision harness for the open-set heaps. JMH gives the
 * authoritative throughput numbers (see {@link PathfinderHeapBenchmark}); this exists to produce a
 * quick side-by-side and, crucially, to quantify the ordering error the {@link BucketMinHeap}
 * introduces - something a pure throughput benchmark cannot show.
 *
 * <p>Run with: {@code ./mvnw -pl engine test-compile exec:java
 * -Dexec.mainClass=de.bsommerfeld.pathetic.engine.benchmark.heap.HeapMicroBench}
 */
public final class HeapMicroBench {

  private static final double MAX_COST = 100.0;

  private HeapMicroBench() {}

  public static void main(String[] args) {
    int[] sizes = {1_000, 10_000, 100_000};

    System.out.println("=== Throughput (mixed insert + 1/5 extract), lower ns/op is better ===");
    for (int ops : sizes) {
      long[] ids = new long[ops];
      double[] costs = new double[ops];
      buildWorkload(ids, costs, ops);

      System.out.println("\noperations = " + ops);
      time("Quaternary (d=4, prod)", ops, () -> new QuaternaryPrimitiveMinHeap(ops), ids, costs);
      time("Dary d=8", ops, () -> new DaryPrimitiveMinHeap(ops, 8), ids, costs);
      time("Dary d=16", ops, () -> new DaryPrimitiveMinHeap(ops, 16), ids, costs);
      time("Bucket g=1.0", ops, () -> new BucketMinHeap(ops, MAX_COST, 1.0), ids, costs);
      time("Bucket g=0.1", ops, () -> new BucketMinHeap(ops, MAX_COST, 0.1), ids, costs);
      time("Bucket g=0.01", ops, () -> new BucketMinHeap(ops, MAX_COST, 0.01), ids, costs);
    }

    System.out.println(
        "\n=== Precision: extraction-order inversions vs exact heap (lower is better) ===");
    System.out.println(
        "Pure insert-then-drain of 50k unique costs; counts pairs popped out of true cost order.");
    precision(0.1);
    precision(0.5);
    precision(1.0);
  }

  /* 30% of operations reference an existing id (enabling decrease-key), like the JMH workload. */
  private static void buildWorkload(long[] ids, double[] costs, int ops) {
    Random random = new Random(42);
    int uniqueNodes = Math.max(1, ops / 2);
    for (int i = 0; i < ops; i++) {
      ids[i] = random.nextInt(uniqueNodes);
      costs[i] = random.nextDouble() * MAX_COST;
    }
  }

  private static void time(
      String label, int ops, Supplier<MinHeap> factory, long[] ids, double[] costs) {
    /* Warmup to let the JIT compile the hot loop before measuring. */
    for (int w = 0; w < 5; w++) runOnce(factory.get(), ops, ids, costs);

    int reps = 20;
    long best = Long.MAX_VALUE;
    long checksum = 0;
    for (int r = 0; r < reps; r++) {
      MinHeap heap = factory.get();
      long t0 = System.nanoTime();
      checksum += runOnce(heap, ops, ids, costs);
      long elapsed = System.nanoTime() - t0;
      if (elapsed < best) best = elapsed;
    }
    double nsPerOp = (double) best / ops;
    System.out.printf(
        "  %-24s %7.2f ns/op  (best of %d, checksum=%d)%n", label, nsPerOp, reps, checksum);
  }

  /* Returns a checksum of extracted ids so the JIT cannot eliminate the work. */
  private static long runOnce(MinHeap heap, int ops, long[] ids, double[] costs) {
    long checksum = 0;
    for (int i = 0; i < ops; i++) {
      heap.insertOrUpdate(ids[i], costs[i]);
      if (i % 5 == 0 && !heap.isEmpty()) checksum += heap.extractMin();
    }
    while (!heap.isEmpty()) checksum += heap.extractMin();
    return checksum;
  }

  private static void precision(double granularity) {
    int n = 50_000;
    Random random = new Random(7);
    long[] ids = new long[n];
    double[] costs = new double[n];
    for (int i = 0; i < n; i++) {
      ids[i] = i;
      costs[i] = random.nextDouble() * MAX_COST;
    }

    BucketMinHeap bucket = new BucketMinHeap(n, MAX_COST, granularity);
    for (int i = 0; i < n; i++) bucket.insertOrUpdate(ids[i], costs[i]);

    long inversions = 0;
    double maxRegression = 0.0;
    double prevCost = -1.0;
    for (int i = 0; i < n; i++) {
      int id = (int) bucket.extractMin();
      double c = costs[id];
      if (c < prevCost) {
        inversions++;
        maxRegression = Math.max(maxRegression, prevCost - c);
      }
      prevCost = Math.max(prevCost, c);
    }
    System.out.printf(
        "  granularity=%-5s  inversions=%6d / %d pops (%.2f%%), worst out-of-order gap=%.4f cost units%n",
        granularity, inversions, n, 100.0 * inversions / n, maxRegression);
  }
}
