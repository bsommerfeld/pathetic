package de.bsommerfeld.pathetic.api.pathing.heuristic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OctileHeuristicStrategyTest {

  private static final double EPS = 1e-9;

  private final OctileHeuristicStrategy octile = new OctileHeuristicStrategy();

  /* True optimal cost between two cells in obstacle-free 3D with 26-directional Euclidean moves. */
  private static double octileOptimal(int dx, int dy, int dz) {
    int[] a = {Math.abs(dx), Math.abs(dy), Math.abs(dz)};
    Arrays.sort(a);
    return (Math.sqrt(3) - Math.sqrt(2)) * a[0] + (Math.sqrt(2) - 1.0) * a[1] + a[2];
  }

  private double h(PathPosition position, PathPosition target, HeuristicWeights weights) {
    return octile.calculate(new HeuristicContext(position, position, target, weights));
  }

  private double h(int dx, int dy, int dz, HeuristicWeights weights) {
    return h(new PathPosition(0, 0, 0), new PathPosition(dx, dy, dz), weights);
  }

  @Test
  void defaultWeightsMatchTheObstacleFreeOptimumExactly() {
    for (int dx = -12; dx <= 12; dx += 3) {
      for (int dy = -7; dy <= 7; dy++) {
        for (int dz = -12; dz <= 12; dz += 4) {
          assertEquals(
              octileOptimal(dx, dy, dz),
              h(dx, dy, dz, HeuristicWeights.DEFAULT_WEIGHTS),
              EPS,
              "h must equal the free-space optimum at (" + dx + "," + dy + "," + dz + ")");
        }
      }
    }
  }

  @Test
  void isConsistentAcrossEveryUnitStep() {
    PathPosition target = new PathPosition(7, -3, 11);
    for (int x = -2; x <= 2; x++) {
      for (int y = -2; y <= 2; y++) {
        for (int z = -2; z <= 2; z++) {
          PathPosition node = new PathPosition(x, y, z);
          double hNode = h(node, target, HeuristicWeights.DEFAULT_WEIGHTS);
          for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
              for (int oz = -1; oz <= 1; oz++) {
                if (ox == 0 && oy == 0 && oz == 0) continue;
                PathPosition next = new PathPosition(x + ox, y + oy, z + oz);
                double step = octile.calculateTransitionCost(node, next);
                double hNext = h(next, target, HeuristicWeights.DEFAULT_WEIGHTS);
                assertTrue(
                    hNode <= step + hNext + EPS,
                    "consistency violated from " + node + " to " + next);
              }
            }
          }
        }
      }
    }
  }

  @Test
  void octileWeightScalesTheEstimateLinearly() {
    double base = h(9, 4, -6, HeuristicWeights.DEFAULT_WEIGHTS);
    double inflated = h(9, 4, -6, HeuristicWeights.create(1.0, 1.5, 1.0, 1.0));
    assertEquals(base * 1.5, inflated, EPS);
  }

  @Test
  void ignoresAllWeightsExceptOctile() {
    double base = h(9, 4, -6, HeuristicWeights.create(0, 1, 0, 0));
    double noisy = h(9, 4, -6, HeuristicWeights.create(5, 1, 7, 3));
    assertEquals(base, noisy, EPS);
  }

  @Test
  void farApartAbsoluteCoordinatesDoNotOverflow() {
    PathPosition position = new PathPosition(Integer.MIN_VALUE + 1, 0, 0);
    PathPosition target = new PathPosition(Integer.MAX_VALUE - 1, 0, 0);
    double expected = (double) (Integer.MAX_VALUE - 1) - (Integer.MIN_VALUE + 1);
    assertEquals(expected, h(position, target, HeuristicWeights.DEFAULT_WEIGHTS), 1.0);
  }

  @Test
  void transitionCostIsEuclidean() {
    PathPosition from = new PathPosition(0, 0, 0);
    assertEquals(1.0, octile.calculateTransitionCost(from, new PathPosition(1, 0, 0)), EPS);
    assertEquals(Math.sqrt(2), octile.calculateTransitionCost(from, new PathPosition(1, 1, 0)), EPS);
    assertEquals(Math.sqrt(3), octile.calculateTransitionCost(from, new PathPosition(1, 1, 1)), EPS);
  }
}
