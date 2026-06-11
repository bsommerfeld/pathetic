package de.bsommerfeld.pathetic.engine.pathfinder.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.QuaternaryPrimitiveMinHeap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuaternaryPrimitiveMinHeapTest {

  private MinHeap heap;

  @BeforeEach
  void setUp() {
    // Start small to force resizing logic to trigger early in tests
    heap = new QuaternaryPrimitiveMinHeap(4);
  }

  @Test
  void testSimpleInsertAndExtract() {
    heap.insertOrUpdate(10L, 100.0);
    heap.insertOrUpdate(20L, 50.0); // Smaller cost
    heap.insertOrUpdate(30L, 75.0);

    assertEquals(3, heap.size());
    assertFalse(heap.isEmpty());

    // Expected order: 20 (50.0), 30 (75.0), 10 (100.0)
    assertEquals(20L, heap.extractMin());
    assertEquals(30L, heap.extractMin());
    assertEquals(10L, heap.extractMin());

    assertTrue(heap.isEmpty());
  }

  @Test
  void testDecreaseKey() {
    // A=100, B=200
    heap.insertOrUpdate(1L, 100.0);
    heap.insertOrUpdate(2L, 200.0);

    // B is more expensive, so A comes first
    assertEquals(100.0, heap.cost(1L));

    // Update B to 50 (cheaper than A)
    heap.insertOrUpdate(2L, 50.0);

    // Now B must come first
    assertEquals(50.0, heap.cost(2L));
    assertEquals(2L, heap.extractMin());
    assertEquals(1L, heap.extractMin());
  }

  /*
   * Covers the heap-level mechanism that updateExistingNode's nudging branch relies on:
   * when the recomputed F-key collapses to within 1 ulp of the stored key, the pathfinder
   * forces a reorder by writing `oldKey - Math.ulp(oldKey)`. This test pins that a 1-ulp
   * decrease is accepted by insertOrUpdate and the affected entry rises to the top.
   */
  @Test
  void testInsertOrUpdateAcceptsUlpSizedDecrease() {
    heap.insertOrUpdate(1L, 100.0);
    heap.insertOrUpdate(2L, 100.0);

    // Both entries share the same cost — extraction order is undefined.
    // Nudging node 2L down by one ulp must make it strictly cheaper and pop first.
    double oldCost = heap.cost(2L);
    heap.insertOrUpdate(2L, oldCost - Math.ulp(oldCost));

    assertTrue(heap.cost(2L) < oldCost, "Heap must accept a 1-ulp decrease");
    assertEquals(2L, heap.extractMin(), "Nudged node should be popped before the equal-cost peer");
    assertEquals(1L, heap.extractMin());
  }

  @Test
  void testUpdateWithHigherCostIgnored() {
    heap.insertOrUpdate(1L, 100.0);

    // Attempt to increase cost (should be ignored in Dijkstra/A*)
    heap.insertOrUpdate(1L, 150.0);

    assertEquals(100.0, heap.cost(1L));
  }

  @Test
  void testResizing() {
    // Heap initial capacity is 4 (see setUp)
    // We insert 100 elements
    for (int i = 0; i < 100; i++) {
      heap.insertOrUpdate(i, 1000 - i); // Insert in reverse order
    }

    assertEquals(100, heap.size());

    // Check if sorting is correct
    // i=99 -> Cost=901 (Minimum)
    // i=0  -> Cost=1000 (Maximum)

    for (int i = 99; i >= 0; i--) {
      assertEquals((long) i, heap.extractMin());
    }
  }

  @Test
  void testClear() {
    heap.insertOrUpdate(1L, 10.0);
    heap.clear();
    assertTrue(heap.isEmpty());
    assertEquals(0, heap.size());
    assertFalse(heap.contains(1L));

    // Reuse after clear
    heap.insertOrUpdate(2L, 5.0);
    assertEquals(2L, heap.extractMin());
  }

  @Test
  void testExtractEmptyThrows() {
    assertThrows(NoSuchElementException.class, () -> heap.extractMin());
  }

  /*
   * Extracting the last element and then extracting again must throw instead of returning the
   * stale root and driving the size negative.
   */
  @Test
  void testExtractAfterDrainThrows() {
    heap.insertOrUpdate(1L, 10.0);
    assertEquals(1L, heap.extractMin());

    assertThrows(NoSuchElementException.class, () -> heap.extractMin());
    assertEquals(0, heap.size());

    // The heap must remain usable after the rejected extraction.
    heap.insertOrUpdate(2L, 5.0);
    assertEquals(2L, heap.extractMin());
  }

  @Test
  void testInsertNaNCostThrows() {
    /*
     * NaN compares false against everything, which would stall siftUp/siftDown and permanently
     * corrupt the ordering. The heap must reject it at the boundary instead of silently storing it.
     */
    assertThrows(
        IllegalArgumentException.class, () -> heap.insertOrUpdate(1L, Double.NaN));

    // The rejected insert must leave the heap untouched.
    assertTrue(heap.isEmpty());
    assertFalse(heap.contains(1L));
  }

  @Test
  void testInsertInfiniteCostIsAccepted() {
    // Infinity is fully ordered; it must be accepted and simply sort last.
    heap.insertOrUpdate(1L, Double.POSITIVE_INFINITY);
    heap.insertOrUpdate(2L, 10.0);

    assertEquals(2L, heap.extractMin());
    assertEquals(1L, heap.extractMin());
  }

  /*
   * Dense-id contract: ids index an array directly, so sparse 64-bit keys (negative after int
   * truncation, or larger than an int) must be rejected loudly instead of aliasing distinct
   * nodes onto the same slot.
   */
  @Test
  void testSparseIdsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> heap.insertOrUpdate(-1L, 10.0));
    assertThrows(
        IllegalArgumentException.class, () -> heap.insertOrUpdate(1L << 33, 10.0));
    assertThrows(
        IllegalArgumentException.class, () -> heap.insertOrUpdate(Long.MIN_VALUE, 10.0));

    assertTrue(heap.isEmpty());
  }

  @Test
  void testContainsAndCostTolerateOutOfContractIds() {
    heap.insertOrUpdate(5L, 10.0);

    assertFalse(heap.contains(-1L));
    assertFalse(heap.contains(1L << 40));
    assertEquals(Double.MAX_VALUE, heap.cost(-1L));
    assertEquals(Double.MAX_VALUE, heap.cost(1L << 40));
  }

  @Test
  void testContains() {
    heap.insertOrUpdate(55L, 10.0);
    assertTrue(heap.contains(55L));
    assertFalse(heap.contains(99L));

    heap.extractMin();
    assertFalse(heap.contains(55L));
  }

  /** Fuzz test for sorting and resizing integrity against Java's PriorityQueue. */
  @Test
  void testFuzzSortingIntegrity() {
    PriorityQueue<NodeWrapper> reference =
        new PriorityQueue<>(Comparator.comparingDouble(n -> n.cost));
    Random rand = new Random(42);

    // 1. Fill both with random data
    for (int i = 0; i < 5000; i++) {
      long id = i; // Unique ID to avoid duplicate key issues in simple comparison
      double cost = rand.nextDouble();

      heap.insertOrUpdate(id, cost);
      reference.add(new NodeWrapper(id, cost));
    }

    assertEquals(reference.size(), heap.size());

    // 2. Extract all and compare order
    while (!reference.isEmpty()) {
      long heapId = heap.extractMin();
      NodeWrapper refNode = reference.poll();

      assertEquals(refNode.id, heapId, "Order mismatch! Heap failed sorting integrity.");
    }

    assertTrue(heap.isEmpty());
  }

  /*
   * Interleaved decrease-key and extraction fuzzing in the dense-id regime the engine uses:
   * ids 0..N with random updates, validated against a brute-force scan of the expected minimum.
   */
  @Test
  void testFuzzDecreaseKeyAndExtractIntegrity() {
    Random rand = new Random(123456);
    int uniqueIds = 500;
    double[] expected = new double[uniqueIds];
    Arrays.fill(expected, Double.NaN);

    for (int i = 0; i < 20_000; i++) {
      int id = rand.nextInt(uniqueIds);
      double cost = rand.nextDouble() * 1000;

      heap.insertOrUpdate(id, cost);
      if (Double.isNaN(expected[id]) || cost < expected[id]) {
        expected[id] = cost;
      }

      if (i % 7 == 0 && !heap.isEmpty()) {
        long extracted = heap.extractMin();
        double minExpected = Double.MAX_VALUE;
        for (double value : expected) {
          if (!Double.isNaN(value) && value < minExpected) minExpected = value;
        }
        assertEquals(
            minExpected,
            expected[(int) extracted],
            "Extracted node must carry the minimum tracked cost");
        expected[(int) extracted] = Double.NaN;
      }
    }
  }

  // Helper class for comparison with PriorityQueue (Java 8 replacement for record)
  public static class NodeWrapper {
    final long id;
    final double cost;

    public NodeWrapper(long id, double cost) {
      this.id = id;
      this.cost = cost;
    }
  }
}
