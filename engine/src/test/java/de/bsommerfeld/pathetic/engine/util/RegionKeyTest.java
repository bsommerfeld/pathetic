package de.bsommerfeld.pathetic.engine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegionKeyTest {

  private static final int MIN_XZ = -2097152;
  private static final int MAX_XZ = 2097151;
  private static final int MIN_Y = -524288;
  private static final int MAX_Y = 524287;

  @Test
  void boundaryValuesAreAccepted() {
    /* The extremes of each field's two's-complement range must pack without throwing. */
    RegionKey.pack(MIN_XZ, MIN_Y, MIN_XZ);
    RegionKey.pack(MAX_XZ, MAX_Y, MAX_XZ);
  }

  @Test
  void outOfRangeCoordinatesThrow() {
    assertThrows(IllegalArgumentException.class, () -> RegionKey.pack(MAX_XZ + 1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> RegionKey.pack(MIN_XZ - 1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> RegionKey.pack(0, 0, MAX_XZ + 1));
    assertThrows(IllegalArgumentException.class, () -> RegionKey.pack(0, MAX_Y + 1, 0));
    assertThrows(IllegalArgumentException.class, () -> RegionKey.pack(0, MIN_Y - 1, 0));
  }

  @Test
  void isInRangeMatchesPackAcceptance() {
    /* isInRange must accept exactly what pack accepts: the boundary values pass... */
    assertTrue(RegionKey.isInRange(MIN_XZ, MIN_Y, MIN_XZ));
    assertTrue(RegionKey.isInRange(MAX_XZ, MAX_Y, MAX_XZ));
    assertTrue(RegionKey.isInRange(0, 0, 0));

    /* ...and one past each boundary is rejected, mirroring outOfRangeCoordinatesThrow. */
    assertFalse(RegionKey.isInRange(MAX_XZ + 1, 0, 0));
    assertFalse(RegionKey.isInRange(MIN_XZ - 1, 0, 0));
    assertFalse(RegionKey.isInRange(0, 0, MAX_XZ + 1));
    assertFalse(RegionKey.isInRange(0, 0, MIN_XZ - 1));
    assertFalse(RegionKey.isInRange(0, MAX_Y + 1, 0));
    assertFalse(RegionKey.isInRange(0, MIN_Y - 1, 0));
  }

  @Test
  void negativeCoordinatesAreDistinctAndInjective() {
    /*
     * Negative coordinates (e.g. Minecraft 1.18 with y >= -64) must not alias onto positive keys.
     * Pack a dense block spanning negatives and assert every distinct position gets a unique key.
     */
    Set<Long> keys = new HashSet<>();
    int count = 0;
    for (int y = -64; y <= 319; y++) {
      for (int x = -40; x <= 40; x++) {
        for (int z = -40; z <= 40; z++) {
          keys.add(RegionKey.pack(x, y, z));
          count++;
        }
      }
    }
    assertEquals(count, keys.size(), "every distinct position must map to a unique key");
  }

  @Test
  void distinctFieldsDoNotCollide() {
    assertNotEquals(RegionKey.pack(1, 0, 0), RegionKey.pack(0, 0, 1));
    assertNotEquals(RegionKey.pack(0, -1, 0), RegionKey.pack(0, 1, 0));
    assertNotEquals(RegionKey.pack(-1, 0, 0), RegionKey.pack(0, 0, -1));
  }
}
