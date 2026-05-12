package de.bsommerfeld.pathetic.api.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathVectorTest {

  private static final double EPS = 1e-9;

  // -------------------------------------------------------------------------
  // computeDistance hardening - see CODE_REVIEW 4.5
  // -------------------------------------------------------------------------

  @Test
  void computeDistanceRejectsNullA() {
    PathVector b = PathVector.of(0, 0, 0);
    PathVector c = PathVector.of(1, 0, 0);
    assertThrows(NullPointerException.class, () -> PathVector.computeDistance(null, b, c));
  }

  @Test
  void computeDistanceRejectsNullB() {
    PathVector a = PathVector.of(0, 1, 0);
    PathVector c = PathVector.of(1, 0, 0);
    assertThrows(NullPointerException.class, () -> PathVector.computeDistance(a, null, c));
  }

  @Test
  void computeDistanceRejectsNullC() {
    PathVector a = PathVector.of(0, 1, 0);
    PathVector b = PathVector.of(0, 0, 0);
    assertThrows(NullPointerException.class, () -> PathVector.computeDistance(a, b, null));
  }

  @Test
  void computeDistanceDegeneratesWhenBEqualsC() {
    // Line through B and C degenerates to a point -> distance is plain Euclidean A-to-B.
    PathVector a = PathVector.of(3, 4, 0);
    PathVector b = PathVector.of(0, 0, 0);
    PathVector c = PathVector.of(0, 0, 0);
    assertEquals(5.0, PathVector.computeDistance(a, b, c), EPS);
  }

  @Test
  void computeDistanceReturnsZeroWhenPointOnLine() {
    PathVector a = PathVector.of(2, 0, 0);
    PathVector b = PathVector.of(0, 0, 0);
    PathVector c = PathVector.of(5, 0, 0);
    assertTrue(PathVector.computeDistance(a, b, c) < EPS);
  }

  @Test
  void computeDistanceForOffAxisPoint() {
    // Point at (0, 3, 0), line along x-axis -> perpendicular distance is 3.
    PathVector a = PathVector.of(0, 3, 0);
    PathVector b = PathVector.of(0, 0, 0);
    PathVector c = PathVector.of(10, 0, 0);
    assertEquals(3.0, PathVector.computeDistance(a, b, c), EPS);
  }
}
