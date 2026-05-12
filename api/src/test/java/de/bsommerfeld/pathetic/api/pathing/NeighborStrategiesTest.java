package de.bsommerfeld.pathetic.api.pathing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NeighborStrategiesTest {

  // -------------------------------------------------------------------------
  // Caching - see CODE_REVIEW 4.6
  // The strategy must return the same Iterable instance on repeated calls so
  // the A* hot loop performs zero per-iteration allocations.
  // -------------------------------------------------------------------------

  @Test
  void verticalAndHorizontalReturnsSameInstanceAcrossCalls() {
    Iterable<PathVector> a = NeighborStrategies.VERTICAL_AND_HORIZONTAL.getOffsets();
    Iterable<PathVector> b = NeighborStrategies.VERTICAL_AND_HORIZONTAL.getOffsets();
    assertSame(a, b, "getOffsets() must return the cached instance, not a fresh allocation");
  }

  @Test
  void diagonal3dReturnsSameInstanceAcrossCalls() {
    Iterable<PathVector> a = NeighborStrategies.DIAGONAL_3D.getOffsets();
    Iterable<PathVector> b = NeighborStrategies.DIAGONAL_3D.getOffsets();
    assertSame(a, b);
  }

  @Test
  void positionAwareCallDelegatesToCachedOffsets() {
    PathPosition any = new PathPosition(0, 0, 0);
    Iterable<PathVector> cached = NeighborStrategies.VERTICAL_AND_HORIZONTAL.getOffsets();
    Iterable<PathVector> viaPosition =
        NeighborStrategies.VERTICAL_AND_HORIZONTAL.getOffsets(any);
    assertSame(cached, viaPosition);
  }

  // -------------------------------------------------------------------------
  // Offset content - guard against accidental list edits
  // -------------------------------------------------------------------------

  @Test
  void verticalAndHorizontalHasSixCardinalOffsets() {
    Set<PathVector> offsets = collect(NeighborStrategies.VERTICAL_AND_HORIZONTAL.getOffsets());
    assertEquals(6, offsets.size());
    assertTrue(offsets.contains(new PathVector(1, 0, 0)));
    assertTrue(offsets.contains(new PathVector(-1, 0, 0)));
    assertTrue(offsets.contains(new PathVector(0, 1, 0)));
    assertTrue(offsets.contains(new PathVector(0, -1, 0)));
    assertTrue(offsets.contains(new PathVector(0, 0, 1)));
    assertTrue(offsets.contains(new PathVector(0, 0, -1)));
  }

  @Test
  void diagonal3dHas26OffsetsAndSkipsSelf() {
    Set<PathVector> offsets = collect(NeighborStrategies.DIAGONAL_3D.getOffsets());
    assertEquals(26, offsets.size(), "3x3x3 cube minus self");
    assertTrue(
        offsets.stream().noneMatch(v -> v.getX() == 0 && v.getY() == 0 && v.getZ() == 0),
        "self-vector (0,0,0) must not be emitted");
  }

  @Test
  void offsetListsAreImmutable() {
    // Lists are exposed via the Iterable contract. The backing structure must not be mutable,
    // otherwise a misbehaving caller could corrupt every subsequent search.
    Iterable<PathVector> offsets = NeighborStrategies.VERTICAL_AND_HORIZONTAL.getOffsets();
    if (offsets instanceof java.util.List) {
      java.util.List<PathVector> asList = (java.util.List<PathVector>) offsets;
      assertNotNull(asList);
      assertThrows(
          UnsupportedOperationException.class, () -> asList.add(new PathVector(99, 99, 99)));
    }
  }

  private static Set<PathVector> collect(Iterable<PathVector> iterable) {
    Set<PathVector> set = new HashSet<>();
    for (PathVector v : iterable) set.add(v);
    return set;
  }
}
