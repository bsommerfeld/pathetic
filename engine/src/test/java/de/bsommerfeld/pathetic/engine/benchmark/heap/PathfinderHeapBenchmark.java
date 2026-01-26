package de.bsommerfeld.pathetic.engine.benchmark.heap;

import de.bsommerfeld.pathetic.engine.benchmark.heap.baritone.BinaryHeapOpenSet;
import de.bsommerfeld.pathetic.engine.benchmark.heap.baritone.PathNode;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.PrimitiveMinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.QuaternaryPrimitiveMinHeap;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.FibonacciHeap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PathfinderHeapBenchmark {

  @Param({"1000", "10000", "100000"})
  private int operations;

  private long[] packedNodes;
  private double[] costs;
  private boolean[] isUpdate;

  private PathNode[] baritoneNodes;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(PathfinderHeapBenchmark.class.getSimpleName())
            .forks(1)
            .build();

    new Runner(opt).run();
  }

  @Setup(Level.Invocation)
  public void setupInvocation() {
    for (PathNode node : baritoneNodes) {
      if (node != null) node.heapPosition = -1;
    }
  }

  @Setup
  public void setup() {
    Random random = new Random(42);
    packedNodes = new long[operations];
    costs = new double[operations];
    isUpdate = new boolean[operations];
    baritoneNodes = new PathNode[operations];

    // Create a smaller pool of unique nodes to allow realistic updates
    int uniqueNodes = operations / 2;

    for (int i = 0; i < operations; i++) {
      // Reuse node IDs to enable updates (30% of the time reference existing nodes)
      int nodeId = random.nextInt(uniqueNodes);
      packedNodes[i] = nodeId;
      costs[i] = random.nextDouble() * 100;
      isUpdate[i] = random.nextDouble() < 0.3;
    }

    // Create PathNode instances for Baritone (one per unique ID)
    for (int i = 0; i < uniqueNodes; i++) {
      baritoneNodes[i] = new PathNode(i, 0, 0);
    }
  }

  /** Dein ursprünglicher binärer Heap (optimiert mit Hole-Filling). */
  @Benchmark
  public void benchPrimitiveMinHeap(Blackhole bh) {
    PrimitiveMinHeap heap = new PrimitiveMinHeap(operations);
    for (int i = 0; i < operations; i++) {
      heap.insertOrUpdate(packedNodes[i], costs[i]);
      if (i % 5 == 0 && !heap.isEmpty()) bh.consume(heap.extractMin());
    }
  }

  @Benchmark
  public void benchQuaternaryPrimitiveMinHeap(Blackhole bh) {
    QuaternaryPrimitiveMinHeap heap = new QuaternaryPrimitiveMinHeap(operations);
    for (int i = 0; i < operations; i++) {
      heap.insertOrUpdate(packedNodes[i], costs[i]);
      if (i % 5 == 0 && !heap.isEmpty()) {
        bh.consume(heap.extractMin());
      }
    }
  }

  @Benchmark
  public void benchBaritoneHeap(Blackhole bh) {
    BinaryHeapOpenSet heap = new BinaryHeapOpenSet(operations);
    for (int i = 0; i < operations; i++) {
      int nodeId = (int) packedNodes[i];
      PathNode node = baritoneNodes[nodeId];
      node.combinedCost = costs[i];
      if (node.heapPosition != -1) {
        heap.update(node);
      } else {
        heap.insert(node);
      }
      if (i % 5 == 0 && !heap.isEmpty()) bh.consume(heap.removeLowest());
    }
  }

  @Benchmark
  public void benchFibonacciHeap(Blackhole bh) {
    FibonacciHeap<Double, Long> heap = new FibonacciHeap<>();
    Map<Long, AddressableHeap.Handle<Double, Long>> handles = new HashMap<>();

    for (int i = 0; i < operations; i++) {
      long node = packedNodes[i];
      double cost = costs[i];
      if (handles.containsKey(node)) {
        try {
          handles.get(node).decreaseKey(cost);
        } catch (IllegalArgumentException ignored) {
          // Cost is not lower, treat as no-op like insertOrUpdate does
        }
      } else {
        handles.put(node, heap.insert(cost, node));
      }
      if (i % 5 == 0 && !heap.isEmpty()) bh.consume(heap.deleteMin());
    }
  }

  @Benchmark
  public void benchPriorityQueue(Blackhole bh) {
    PriorityQueue<NodeWrapper> pq = new PriorityQueue<>();
    Map<Long, NodeWrapper> map = new HashMap<>();

    for (int i = 0; i < operations; i++) {
      long node = packedNodes[i];
      double cost = costs[i];
      if (map.containsKey(node)) {
        NodeWrapper old = map.get(node);
        pq.remove(old);
        NodeWrapper next = new NodeWrapper(node, cost);
        pq.add(next);
        map.put(node, next);
      } else {
        NodeWrapper next = new NodeWrapper(node, cost);
        pq.add(next);
        map.put(node, next);
      }
      if (i % 5 == 0 && !pq.isEmpty()) bh.consume(pq.poll());
    }
  }

  private static class NodeWrapper implements Comparable<NodeWrapper> {
    private final long id;
    private final double cost;

    public NodeWrapper(long id, double cost) {
      this.id = id;
      this.cost = cost;
    }

    @Override
    public int compareTo(NodeWrapper o) {
      return Double.compare(this.cost, o.cost);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      NodeWrapper that = (NodeWrapper) o;
      return id == that.id;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(id);
    }
  }
}
