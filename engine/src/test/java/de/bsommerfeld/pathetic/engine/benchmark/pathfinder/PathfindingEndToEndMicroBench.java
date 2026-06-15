package de.bsommerfeld.pathetic.engine.benchmark.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-JMH end-to-end timing harness for a full {@code findPath} call. JMH (see {@link
 * PathfinderEndToEndBenchmark}) gives the authoritative numbers; this exists to produce a quick,
 * reproducible before/after when the JVM's JMH forking is unavailable.
 *
 * <p>Run with: {@code ./mvnw -pl engine test-compile exec:java
 * -Dexec.mainClass=de.bsommerfeld.pathetic.engine.benchmark.pathfinder.PathfindingEndToEndMicroBench}
 */
public final class PathfindingEndToEndMicroBench {

  private PathfindingEndToEndMicroBench() {}

  public static void main(String[] args) {
    /* Open 3D world, no processors: exercises the no-processor fast path on a long greedy walk. */
    run("open-3d, no processors, d=64", openWorldNoProcessors(), 0, 64, 0, 64);

    /*
     * Planar wall grid with a validator: forces a wide search frontier, so the per-expanded-node
     * work (node creation, the open/closed bookkeeping) dominates - the case node flattening
     * targets.
     */
    run("walls planar, validator, d=128", wallWorld(), 0, 0, 128, 128);
    run("walls planar, validator, d=256", wallWorld(), 0, 0, 256, 256);
  }

  private static Pathfinder openWorldNoProcessors() {
    NavigationPointProvider provider = (position, context) -> () -> true;
    return new AStarPathfinderFactory()
        .createPathfinder(
            PathfinderConfiguration.builder()
                .provider(provider)
                .async(false)
                .fallback(false)
                .maxIterations(2_000_000)
                .build());
  }

  private static Pathfinder wallWorld() {
    NavigationPointProvider provider = (position, context) -> () -> true;
    ValidationProcessor worldValidator =
        context -> {
          PathPosition p = context.getCurrentPathPosition();
          if (p.getFlooredY() != 0) return false;
          boolean wallColumn = Math.floorMod(p.getFlooredX(), 32) == 16;
          boolean gapRow = Math.floorMod(p.getFlooredZ(), 32) == 16;
          return !wallColumn || gapRow;
        };
    return new AStarPathfinderFactory()
        .createPathfinder(
            PathfinderConfiguration.builder()
                .provider(provider)
                .async(false)
                .fallback(false)
                .maxIterations(2_000_000)
                .nodeValidationProcessors(Collections.singletonList(worldValidator))
                .build());
  }

  private static void run(String label, Pathfinder pf, int sx, int sy, int sz, int dxz) {
    PathPosition start = new PathPosition(sx, sy, sz);
    PathPosition target = new PathPosition(sx + dxz, sy, sz + dxz);

    PathfinderResult probe = pf.findPath(start, target).resultBlocking();
    if (probe.getPathState() != PathState.FOUND) {
      throw new IllegalStateException(label + ": scenario not solvable: " + probe.getPathState());
    }

    for (int w = 0; w < 10; w++) pf.findPath(start, target).resultBlocking();

    int reps = 50;
    long best = Long.MAX_VALUE;
    long total = 0;
    long checksum = 0;
    for (int r = 0; r < reps; r++) {
      long t0 = System.nanoTime();
      PathfinderResult result = pf.findPath(start, target).resultBlocking();
      long elapsed = System.nanoTime() - t0;
      checksum += result.getPath().length();
      total += elapsed;
      if (elapsed < best) best = elapsed;
    }

    /* Counted last so the per-step hook never burdens the timed loop above. */
    int expansions = countExpansions(pf, start, target);
    System.out.printf(
        "  %-34s  best=%6.3f ms  avg=%6.3f ms  expansions=%-7d (pathlen=%d)%n",
        label, best / 1e6, (total / (double) reps) / 1e6, expansions, checksum / reps);
  }

  /* One instrumented run to report frontier size; kept out of the timed loop to avoid skew. */
  private static int countExpansions(Pathfinder pf, PathPosition start, PathPosition target) {
    AtomicInteger steps = new AtomicInteger();
    PathfinderHook hook =
        new PathfinderHook() {
          @Override
          public void onPathfindingStep(PathfindingContext context) {
            steps.incrementAndGet();
          }
        };
    pf.registerPathfindingHook(hook);
    pf.findPath(start, target).resultBlocking();
    /* No unregister API; subsequent timed runs construct their own measurement separately. */
    return steps.get();
  }
}
