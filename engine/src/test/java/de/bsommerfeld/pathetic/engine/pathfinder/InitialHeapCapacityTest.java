package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AbstractPathfinder#computeInitialHeapCapacity}.
 *
 * <p>The heap capacity per search must scale with manhattan distance and branching factor, capped
 * by {@code maxIterations} and floored to a sensible minimum.
 */
class InitialHeapCapacityTest {

  private static final int MAX_ITERATIONS = 5000;
  private static final int CARDINAL_BRANCHING = 6;
  private static final int DIAGONAL_BRANCHING = 26;

  @Test
  void tinySearchHitsFloor() {
    // distance=2, branching=6 -> 12, below the 32 floor
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0), new PathPosition(2, 0, 0), CARDINAL_BRANCHING, MAX_ITERATIONS);
    assertEquals(32, capacity, "tiny searches must use the MIN_INITIAL_HEAP_CAPACITY floor");
  }

  @Test
  void zeroDistanceHitsFloor() {
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(5, 5, 5),
            new PathPosition(5, 5, 5),
            CARDINAL_BRANCHING,
            MAX_ITERATIONS);
    assertEquals(32, capacity);
  }

  @Test
  void mediumCardinalSearchScalesWithDistanceAndBranching() {
    // distance=100, branching=6 -> 600
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0),
            new PathPosition(100, 0, 0),
            CARDINAL_BRANCHING,
            MAX_ITERATIONS);
    assertEquals(600, capacity);
  }

  @Test
  void mediumDiagonalSearchScalesWithDistanceAndBranching() {
    // manhattan(0,0,0 -> 100,0,0) = 100, branching=26 -> 2600
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0),
            new PathPosition(100, 0, 0),
            DIAGONAL_BRANCHING,
            MAX_ITERATIONS);
    assertEquals(2600, capacity);
  }

  @Test
  void manhattanDistanceIsSumOfAxisDifferences() {
    // manhattan(0,0,0 -> 30,40,50) = 120, branching=6 -> 720
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0),
            new PathPosition(30, 40, 50),
            CARDINAL_BRANCHING,
            MAX_ITERATIONS);
    assertEquals(720, capacity);
  }

  @Test
  void manhattanUsesFlooredCoordinates() {
    // Fractional coordinates floor to ints before subtraction.
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0.7, 0.0, 0.0),
            new PathPosition(10.4, 0.0, 0.0),
            CARDINAL_BRANCHING,
            MAX_ITERATIONS);
    // floor(0.7)=0, floor(10.4)=10 -> manhattan=10 -> 60
    assertEquals(60, capacity);
  }

  @Test
  void longSearchIsCappedAtMaxIterations() {
    // manhattan=1000, branching=26 -> 26000, capped at 5000
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0),
            new PathPosition(1000, 0, 0),
            DIAGONAL_BRANCHING,
            MAX_ITERATIONS);
    assertEquals(MAX_ITERATIONS, capacity);
  }

  @Test
  void respectsCustomMaxIterations() {
    int customMax = 100;
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0),
            new PathPosition(50, 0, 0),
            CARDINAL_BRANCHING,
            customMax); // 300 estimated -> capped at 100
    assertEquals(customMax, capacity);
  }

  @Test
  void branchingFloorOfOneIsApplied() {
    // Degenerate branching=0 should be treated as 1 (otherwise estimate would collapse to 0).
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0), new PathPosition(100, 0, 0), 0, MAX_ITERATIONS);
    // manhattan=100, branching coerced to 1 -> 100
    assertEquals(100, capacity);
  }

  @Test
  void doesNotOverflowOnLargeWorlds() {
    // RegionKey allows X/Z up to +-33M. With branching=26, naive int math (33M * 26) overflows.
    // Long arithmetic in the formula must keep the value sane and cap at maxIterations.
    int capacity =
        AbstractPathfinder.computeInitialHeapCapacity(
            new PathPosition(0, 0, 0),
            new PathPosition(33_000_000, 0, 0),
            DIAGONAL_BRANCHING,
            MAX_ITERATIONS);
    assertEquals(MAX_ITERATIONS, capacity);
    assertTrue(capacity > 0, "result must remain non-negative despite huge inputs");
  }
}
