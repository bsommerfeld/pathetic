package de.bsommerfeld.pathetic.engine.benchmark.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import java.util.Collections;
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
 * End-to-end benchmark of a full {@code findPath} call against a synthetic, deterministic world.
 *
 * <p>This benchmark deliberately restricts itself to API surface that exists in older releases so
 * it can be dropped into a checkout of a previous version unchanged for A/B comparisons.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>{@code distance}: straight-line offset between start and target on the X and Z axes.
 *       {@code 0} measures the fixed per-request setup cost (the search ends on the start node);
 *       very long paths can be measured via a CLI override, e.g. {@code -p distance=20000 -p
 *       walls=false}.
 *   <li>{@code walls}: an open field versus a deterministic wall grid with sparse gaps, which
 *       forces a wide search frontier instead of a straight diagonal walk.
 *   <li>{@code originOffset}: the same searches anchored near the world origin versus far outside
 *       the absolute coordinate range supported by engines up to 5.5.0; on those engines the far
 *       cells fail during setup, which is the expected outcome of the comparison.
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PathfinderEndToEndBenchmark {

  @Param({"0", "8", "100", "400"})
  private int distance;

  @Param({"false", "true"})
  private boolean walls;

  @Param({"0", "1500000000"})
  private long originOffset;

  private Pathfinder pathfinder;
  private PathPosition start;
  private PathPosition target;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(PathfinderEndToEndBenchmark.class.getSimpleName())
            .forks(1)
            .build();

    new Runner(opt).run();
  }

  @Setup
  public void setup() {
    int origin = (int) originOffset;
    NavigationPointProvider provider = (position, context) -> () -> true;

    /*
     * Keeps the search planar and, when walls are enabled, overlays a deterministic wall grid:
     * every 32nd X column is solid except where every 32nd Z row punches a gap. The pattern is a
     * pure function of the coordinates, so both sides of an A/B comparison search the identical
     * world.
     */
    ValidationProcessor worldValidator =
        context -> {
          PathPosition p = context.getCurrentPathPosition();
          if (p.getFlooredY() != 0) {
            return false;
          }
          if (!walls) {
            return true;
          }
          boolean wallColumn = Math.floorMod(p.getFlooredX() - origin, 32) == 16;
          boolean gapRow = Math.floorMod(p.getFlooredZ() - origin, 32) == 16;
          return !wallColumn || gapRow;
        };

    PathfinderConfiguration configuration =
        PathfinderConfiguration.builder()
            .provider(provider)
            .async(false)
            .fallback(false)
            .maxIterations(2_000_000)
            .nodeValidationProcessors(Collections.singletonList(worldValidator))
            .build();

    pathfinder = new AStarPathfinderFactory().createPathfinder(configuration);
    start = new PathPosition(origin, 0, origin);
    target = new PathPosition(origin + distance, 0, origin + distance);

    /*
     * Guard against measuring a broken scenario: a search that fails or hits the iteration limit
     * does different work than a successful one, which would silently skew the comparison.
     */
    PathfinderResult probe = pathfinder.findPath(start, target).resultBlocking();
    if (probe.getPathState() != PathState.FOUND) {
      throw new IllegalStateException("Scenario is not solvable: " + probe.getPathState());
    }
  }

  @Benchmark
  public void findPath(Blackhole bh) {
    PathfinderResult result = pathfinder.findPath(start, target).resultBlocking();
    bh.consume(result.getPathState());
    bh.consume(result.getPath().length());
  }
}
