package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AStarSearchStateTest {

  /*
   * A small capacity hint so the growth path of the id-indexed arrays is exercised by the tests
   * that assign more ids than the initial capacity.
   */
  private static AStarSearchState sessionAt(double x, double y, double z) {
    return new AStarSearchState(
        PathfinderConfiguration.builder().provider((position, context) -> () -> true).build(),
        new PathPosition(x, y, z),
        16);
  }

  /*
   * Keys are relative to the search origin: the start itself packs to the zero offset and any
   * other position packs to its offset from the start, regardless of absolute magnitude.
   */
  @Test
  void packIsRelativeToOrigin() {
    AStarSearchState session = sessionAt(1_000_000_000, 100_000, -1_000_000_000);

    assertEquals(
        RegionKey.pack(0, 0, 0),
        session.pack(new PathPosition(1_000_000_000, 100_000, -1_000_000_000)));
    assertEquals(
        RegionKey.pack(3, -4, 5),
        session.pack(new PathPosition(1_000_000_003, 99_996, -999_999_995)));
  }

  @Test
  void sessionsWithDifferentOriginsProduceEqualKeysForEqualOffsets() {
    AStarSearchState near = sessionAt(0, 0, 0);
    AStarSearchState far = sessionAt(2_000_000_000, -1_000_000, -2_000_000_000);

    long nearKey = near.pack(new PathPosition(7, 8, 9));
    long farKey = far.pack(new PathPosition(2_000_000_007, -999_992, -1_999_999_991));

    assertEquals(nearKey, farKey, "equal offsets from the origin must produce equal keys");
  }

  /*
   * isInRange bounds the exploration radius around the origin, not absolute coordinates: a huge
   * absolute position right next to the origin is in range, a position farther from the origin
   * than the packable span is not.
   */
  @Test
  void isInRangeBoundsTheRadiusAroundTheOrigin() {
    AStarSearchState session = sessionAt(1_000_000_000, 0, 0);

    assertTrue(session.isInRange(new PathPosition(1_000_000_001, 10, -10)));
    assertFalse(session.isInRange(new PathPosition(1_003_000_000, 0, 0)));
    assertFalse(session.isInRange(new PathPosition(1_000_000_000, 600_000, 0)));
  }

  /*
   * Dense ids start at 0, increase by one per assignment, and stay stable; cells that never
   * entered the open set report NO_ID.
   */
  @Test
  void idAssignmentIsDenseAndStable() {
    AStarSearchState session = sessionAt(0, 0, 0);
    long keyA = session.pack(new PathPosition(1, 2, 3));
    long keyB = session.pack(new PathPosition(4, 5, 6));

    assertEquals(AStarSearchState.NO_ID, session.idOf(keyA));

    assertEquals(0, session.assignId(keyA));
    assertEquals(1, session.assignId(keyB));
    assertEquals(0, session.idOf(keyA));
    assertEquals(1, session.idOf(keyB));
  }

  @Test
  void openAndClosedStateAreIdIndexedAndGrow() {
    AStarSearchState session = sessionAt(0, 0, 0);
    Node node = Mockito.mock(Node.class);

    /* Assign past the initial capacity so the backing arrays must grow. */
    int lastId = -1;
    for (int i = 0; i < 5000; i++) {
      lastId = session.assignId(session.pack(new PathPosition(i, 0, 0)));
    }

    session.setOpenNode(lastId, node);
    assertSame(node, session.openNode(lastId));
    session.clearOpenNode(lastId);
    assertNull(session.openNode(lastId));

    assertFalse(session.isClosed(lastId));
    session.markClosed(lastId);
    assertTrue(session.isClosed(lastId));
  }

  /*
   * The recorded close-time G-cost backs the reopen comparison: NaN marks "never recorded" and
   * the array is only available when reopening is enabled.
   */
  @Test
  void closedGCostsDefaultToNaNAndAreRecordedPerId() {
    AStarSearchState session =
        new AStarSearchState(
            PathfinderConfiguration.builder()
                .provider((position, context) -> () -> true)
                .reopenClosedNodes(true)
                .build(),
            new PathPosition(0, 0, 0),
            16);

    int id = session.assignId(session.pack(new PathPosition(1, 0, 0)));

    assertTrue(Double.isNaN(session.closedGCost(id)));
    session.recordClosedGCost(id, 42.5);
    assertEquals(42.5, session.closedGCost(id));
  }
}
