package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import org.junit.jupiter.api.Test;

class PathfindingSessionTest {

  private static PathfindingSession sessionAt(double x, double y, double z) {
    return new PathfindingSession(
        PathfinderConfiguration.builder().build(), new PathPosition(x, y, z));
  }

  /*
   * Keys are relative to the search origin: the start itself packs to the zero offset and any
   * other position packs to its offset from the start, regardless of absolute magnitude.
   */
  @Test
  void packIsRelativeToOrigin() {
    PathfindingSession session = sessionAt(1_000_000_000, 100_000, -1_000_000_000);

    assertEquals(
        RegionKey.pack(0, 0, 0),
        session.pack(new PathPosition(1_000_000_000, 100_000, -1_000_000_000)));
    assertEquals(
        RegionKey.pack(3, -4, 5),
        session.pack(new PathPosition(1_000_000_003, 99_996, -999_999_995)));
  }

  @Test
  void sessionsWithDifferentOriginsProduceEqualKeysForEqualOffsets() {
    PathfindingSession near = sessionAt(0, 0, 0);
    PathfindingSession far = sessionAt(2_000_000_000, -1_000_000, -2_000_000_000);

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
    PathfindingSession session = sessionAt(1_000_000_000, 0, 0);

    assertTrue(session.isInRange(new PathPosition(1_000_000_001, 10, -10)));
    assertFalse(session.isInRange(new PathPosition(1_003_000_000, 0, 0)));
    assertFalse(session.isInRange(new PathPosition(1_000_000_000, 600_000, 0)));
  }

  /** Region bucketing must work in the origin-relative space for far absolute coordinates. */
  @Test
  void spatialDataIsCreatedForFarAbsoluteCoordinates() {
    PathfindingSession session = sessionAt(-2_000_000_000, 500_000, 2_000_000_000);

    assertNotNull(
        session.getOrCreateSpatialData(new PathPosition(-2_000_000_000, 500_000, 2_000_000_000)));
    assertNotNull(
        session.getOrCreateSpatialData(new PathPosition(-1_999_999_990, 500_010, 1_999_999_990)));
  }
}
