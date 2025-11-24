package de.bsommerfeld.pathetic.engine.benchmark.heap;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.PrimitiveMinHeap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class HeapBenchmark {

  @Param({"1000", "10000", "50000"})
  int datasetSize;

  private long[] ids;
  private double[] costs;
  private double[] updateCosts;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder().include(HeapBenchmark.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }

  // --- SCENARIO 1: INSERT ONLY ---

  @Setup(Level.Trial)
  public void setupTrial() {
    Random rand = new Random(1337);
    ids = new long[datasetSize];
    costs = new double[datasetSize];
    updateCosts = new double[datasetSize];

    for (int i = 0; i < datasetSize; i++) {
      ids[i] = rand.nextLong();
      costs[i] = rand.nextDouble() * 1000;
      updateCosts[i] = costs[i] * 0.5;
    }
  }

  @Benchmark
  public void insert_PrimitiveMinHeap(Blackhole bh) {
    PrimitiveMinHeap heap = new PrimitiveMinHeap(datasetSize);
    for (int i = 0; i < datasetSize; i++) {
      heap.insertOrUpdate(ids[i], costs[i]);
    }
    bh.consume(heap);
  }

  // --- SCENARIO 2: A* SIMULATION (Mixed Ops) ---

  @Benchmark
  public void aStarSim_PrimitiveMinHeap(Blackhole bh) {
    PrimitiveMinHeap primHeap = new PrimitiveMinHeap(datasetSize);

    // 1. Initial Fill
    for (int i = 0; i < datasetSize; i++) {
      primHeap.insertOrUpdate(ids[i], costs[i]);
    }

    // 2. Process
    while (!primHeap.isEmpty()) {
      long minId = primHeap.extractMin();
      bh.consume(minId);

      // 10% Chance for DecreaseKey
      if (primHeap.size() > 10 && (minId % 10 == 0)) {
        int pickIndex = Math.min(primHeap.size(), datasetSize - 1);
        long updateId = ids[pickIndex];

        if (primHeap.contains(updateId)) {
          primHeap.insertOrUpdate(updateId, updateCosts[0]);
        }
      }
    }
  }
}
