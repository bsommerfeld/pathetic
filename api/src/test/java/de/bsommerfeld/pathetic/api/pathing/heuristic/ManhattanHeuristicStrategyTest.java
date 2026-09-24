package de.bsommerfeld.pathetic.api.pathing.heuristic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import org.junit.jupiter.api.Test;

class ManhattanHeuristicStrategyTest {

  private static final double EPS = 1e-9;

  private final ManhattanHeuristicStrategy manhattan = new ManhattanHeuristicStrategy();

  private double h(PathPosition position, PathPosition target, HeuristicWeights weights) {
    return manhattan.calculate(new HeuristicContext(position, position, target, weights));
  }

  private double h(int dx, int dy, int dz, HeuristicWeights weights) {
    return h(new PathPosition(0, 0, 0), new PathPosition(dx, dy, dz), weights);
  }

  @Test
  void defaultWeightsMatchTheAxisAlignedOptimumExactly() {
    for (int dx = -9; dx <= 9; dx += 3) {
      for (int dy = -4; dy <= 4; dy++) {
        for (int dz = -8; dz <= 8; dz += 4) {
          assertEquals(
              Math.abs(dx) + Math.abs(dy) + Math.abs(dz),
              h(dx, dy, dz, HeuristicWeights.DEFAULT_WEIGHTS),
              EPS);
        }
      }
    }
  }

  @Test
  void isConsistentAcrossEveryAxisStep() {
    PathPosition target = new PathPosition(7, -3, 11);
    int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    for (int x = -2; x <= 2; x++) {
      for (int y = -2; y <= 2; y++) {
        for (int z = -2; z <= 2; z++) {
          PathPosition node = new PathPosition(x, y, z);
          double hNode = h(node, target, HeuristicWeights.DEFAULT_WEIGHTS);
          for (int[] s : steps) {
            PathPosition next = new PathPosition(x + s[0], y + s[1], z + s[2]);
            double step = manhattan.calculateTransitionCost(node, next);
            double hNext = h(next, target, HeuristicWeights.DEFAULT_WEIGHTS);
            assertTrue(hNode <= step + hNext + EPS, "consistency violated from " + node);
          }
        }
      }
    }
  }

  @Test
  void manhattanWeightScalesTheEstimateLinearly() {
    double base = h(9, 4, -6, HeuristicWeights.DEFAULT_WEIGHTS);
    double inflated = h(9, 4, -6, HeuristicWeights.create(1.3, 1.0, 1.0, 1.0));
    assertEquals(base * 1.3, inflated, EPS);
  }

  @Test
  void ignoresAllWeightsExceptManhattan() {
    double base = h(9, 4, -6, HeuristicWeights.create(1, 0, 0, 0));
    double noisy = h(9, 4, -6, HeuristicWeights.create(1, 5, 7, 3));
    assertEquals(base, noisy, EPS);
  }

  @Test
  void farApartAbsoluteCoordinatesDoNotOverflow() {
    PathPosition position = new PathPosition(Integer.MIN_VALUE + 1, 0, 0);
    PathPosition target = new PathPosition(Integer.MAX_VALUE - 1, 0, 0);
    double expected = (double) (Integer.MAX_VALUE - 1) - (Integer.MIN_VALUE + 1);
    assertEquals(expected, h(position, target, HeuristicWeights.DEFAULT_WEIGHTS), 1.0);
  }
}
