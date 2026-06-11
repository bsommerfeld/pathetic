package de.bsommerfeld.pathetic.engine.benchmark.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.PathfindingSearch;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Concurrent throughput benchmark: one operation submits a batch of 10,000 asynchronous {@code
 * findPath} requests through the default shared executor and waits for all of them to complete.
 *
 * <p>The score is wall time per batch; divide by {@link #BATCH_SIZE} for the amortized cost per
 * path. The single-threaded submission loop is part of the measurement on purpose: at these path
 * sizes it dominates the wall time, which is exactly the claim this benchmark backs.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class PathfinderThroughputBenchmark {

  private static final int BATCH_SIZE = 10_000;

  @Param({"8", "30"})
  private int distance;

  private Pathfinder pathfinder;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(PathfinderThroughputBenchmark.class.getSimpleName())
            .forks(1)
            .build();

    new Runner(opt).run();
  }

  @Setup
  public void setup() {
    NavigationPointProvider provider = (position, context) -> () -> true;

    PathfinderConfiguration configuration =
        PathfinderConfiguration.builder()
            .provider(provider)
            .async(true)
            .fallback(false)
            .maxIterations(100_000)
            .build();

    pathfinder = new AStarPathfinderFactory().createPathfinder(configuration);

    /*
     * Guard against measuring a broken scenario; a failing search does different work than a
     * successful one and would silently skew the numbers.
     */
    PathfinderResult probe =
        pathfinder
            .findPath(new PathPosition(0, 0, 0), new PathPosition(distance, 0, distance))
            .resultBlocking();
    if (probe.getPathState() != PathState.FOUND) {
      throw new IllegalStateException("Scenario is not solvable: " + probe.getPathState());
    }
  }

  @Benchmark
  public void findPathsConcurrently(Blackhole bh) {
    List<PathfindingSearch> searches = new ArrayList<>(BATCH_SIZE);
    for (int i = 0; i < BATCH_SIZE; i++) {
      // Spread the requests over the world so each search has a distinct origin.
      int originX = i * 100;
      int originZ = i * 37;
      searches.add(
          pathfinder.findPath(
              new PathPosition(originX, 0, originZ),
              new PathPosition(originX + distance, 0, originZ + distance)));
    }
    for (PathfindingSearch search : searches) {
      bh.consume(search.resultBlocking().getPathState());
    }
  }
}
