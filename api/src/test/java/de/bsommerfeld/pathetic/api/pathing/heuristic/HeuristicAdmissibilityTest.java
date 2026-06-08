package de.bsommerfeld.pathetic.api.pathing.heuristic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import org.junit.jupiter.api.Test;

/*
 * Pins the documented (non-)admissibility contract of the bundled heuristics so the JavaDoc and the
 * code cannot silently drift apart. With the default weights neither strategy is strictly
 * admissible, because the composite sums overlapping metrics. The key, documented distinction is the
 * SHAPE of the overestimation: bounded and distance-independent for LINEAR (Weighted-A*-like),
 * unbounded and distance-growing for SQUARED (greedy).
 */
class HeuristicAdmissibilityTest {

  private static final double EPS = 1e-9;

  /* True optimal cost between two cells in obstacle-free 3D with 26-directional Euclidean moves. */
  private static double octileOptimal(int dx, int dy, int dz) {
    int[] a = {Math.abs(dx), Math.abs(dy), Math.abs(dz)};
    java.util.Arrays.sort(a);
    return (Math.sqrt(3) - Math.sqrt(2)) * a[0] + (Math.sqrt(2) - 1.0) * a[1] + a[2];
  }

  private static double hAt(IHeuristicStrategy strategy, int dx, int dy, int dz) {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(dx, dy, dz);
    return strategy.calculate(
        new HeuristicContext(start, start, target, HeuristicWeights.DEFAULT_WEIGHTS));
  }

  @Test
  void linearOverestimatesButByABoundedDistanceIndependentFactor() {
    LinearHeuristicStrategy linear = new LinearHeuristicStrategy();

    /* Inadmissible with default weights: it overestimates the true optimum. */
    assertTrue(hAt(linear, 10, 0, 0) > octileOptimal(10, 0, 0));

    /* The factor is bounded and scale-invariant: identical ratio at distance 10 and 100. */
    double ratio10 = hAt(linear, 10, 0, 0) / octileOptimal(10, 0, 0);
    double ratio100 = hAt(linear, 100, 0, 0) / octileOptimal(100, 0, 0);
    assertEquals(ratio10, ratio100, EPS, "LINEAR overestimation must not grow with distance");
  }

  @Test
  void squaredOverestimationGrowsWithoutBound() {
    SquaredHeuristicStrategy squared = new SquaredHeuristicStrategy();

    double ratio10 = hAt(squared, 10, 0, 0) / octileOptimal(10, 0, 0);
    double ratio100 = hAt(squared, 100, 0, 0) / octileOptimal(100, 0, 0);

    /* Unlike LINEAR, the SQUARED overestimation factor increases as the target gets farther. */
    assertTrue(
        ratio100 > ratio10 * 5,
        "SQUARED overestimation must grow with distance (got " + ratio10 + " -> " + ratio100 + ")");
  }
}
