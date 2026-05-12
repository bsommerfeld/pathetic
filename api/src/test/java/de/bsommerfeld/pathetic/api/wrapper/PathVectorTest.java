package de.bsommerfeld.pathetic.api.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class PathVectorTest {

  private static final double EPS = 1e-9;

  // -------------------------------------------------------------------------
  // Immutability contract - see CODE_REVIEW 2.7
  // PathVector lost its Cloneable + non-final fields. Mutator-style methods
  // (setX/setY/setZ, add, multiply, ...) must continue to return fresh
  // instances rather than mutate this.
  // -------------------------------------------------------------------------

  @Test
  void coordinateFieldsAreFinal() throws NoSuchFieldException {
    for (String name : new String[] {"x", "y", "z"}) {
      Field field = PathVector.class.getDeclaredField(name);
      assertTrue(
          Modifier.isFinal(field.getModifiers()),
          "PathVector." + name + " must be final - the class is a value type");
    }
  }

  @Test
  void notCloneableAndNoCloneMethod() {
    assertFalse(
        Cloneable.class.isAssignableFrom(PathVector.class),
        "PathVector must not implement Cloneable - the value type is immutable");
    for (Method method : PathVector.class.getDeclaredMethods()) {
      assertFalse(
          method.getName().equals("clone"),
          "PathVector.clone() must not exist - use the constructor or fluent operations");
    }
  }

  @Test
  void fluentSettersReturnFreshInstance() {
    PathVector original = PathVector.of(1, 2, 3);
    PathVector withX = original.setX(99);
    assertNotSame(original, withX);
    assertEquals(99, withX.getX(), EPS);
    assertEquals(1, original.getX(), EPS, "original must not be mutated");
  }

  @Test
  void arithmeticOperationsReturnFreshInstances() {
    PathVector a = PathVector.of(1, 2, 3);
    PathVector b = PathVector.of(10, 20, 30);
    assertNotSame(a, a.add(b));
    assertNotSame(a, a.subtract(b));
    assertNotSame(a, a.multiply(2));
    assertNotSame(a, a.divide(2));
    assertEquals(1, a.getX(), EPS, "source vector must remain unchanged after arithmetic");
  }

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
