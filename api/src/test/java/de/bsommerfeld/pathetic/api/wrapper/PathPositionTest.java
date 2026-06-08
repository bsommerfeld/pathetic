package de.bsommerfeld.pathetic.api.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class PathPositionTest {

  private static final double EPS = 1e-9;

  /*
   * Mutator-style methods (setX/setY/setZ, add, subtract, floor, mid, midPoint, ...) must return
   * fresh instances rather than mutate this. Coordinate fields are final, class is not Cloneable.
   */
  @Test
  void coordinateFieldsAreFinal() throws NoSuchFieldException {
    for (String name : new String[] {"x", "y", "z"}) {
      Field field = PathPosition.class.getDeclaredField(name);
      assertTrue(
          Modifier.isFinal(field.getModifiers()),
          "PathPosition." + name + " must be final - the class is a value type");
    }
  }

  @Test
  void notCloneableAndNoCloneMethod() {
    assertFalse(
        Cloneable.class.isAssignableFrom(PathPosition.class),
        "PathPosition must not implement Cloneable - the value type is immutable");
    for (Method method : PathPosition.class.getDeclaredMethods()) {
      assertFalse(
          method.getName().equals("clone"),
          "PathPosition.clone() must not exist - use the constructor or fluent operations");
    }
  }

  @Test
  void fluentSettersReturnFreshInstance() {
    PathPosition original = new PathPosition(1, 2, 3);
    PathPosition withX = original.setX(99);
    assertNotSame(original, withX);
    assertEquals(99, withX.getX(), EPS);
    assertEquals(1, original.getX(), EPS, "original must not be mutated");
  }

  @Test
  void arithmeticOperationsReturnFreshInstances() {
    PathPosition a = new PathPosition(1.5, 2.5, 3.5);
    assertNotSame(a, a.add(1, 1, 1));
    assertNotSame(a, a.add(new PathVector(1, 1, 1)));
    assertNotSame(a, a.subtract(1, 1, 1));
    assertNotSame(a, a.subtract(new PathVector(1, 1, 1)));
    assertNotSame(a, a.floor());
    assertNotSame(a, a.mid());
    assertNotSame(a, a.midPoint(new PathPosition(0, 0, 0)));
    assertEquals(1.5, a.getX(), EPS, "source position must remain unchanged");
  }

  /* Value semantics: equals/hashCode are based on floored coordinates. */
  @Test
  void equalsUsesFlooredCoordinates() {
    PathPosition a = new PathPosition(1.0, 2.0, 3.0);
    PathPosition b = new PathPosition(1.9, 2.4, 3.7);
    assertEquals(a, b, "positions in the same block (after flooring) must compare equal");
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void notEqualForDifferentBlocks() {
    assertNotEquals(new PathPosition(0, 0, 0), new PathPosition(1, 0, 0));
    assertNotEquals(new PathPosition(0, 0, 0), new PathPosition(0, 1, 0));
    assertNotEquals(new PathPosition(0, 0, 0), new PathPosition(0, 0, 1));
  }

  @Test
  void notEqualToOtherTypes() {
    assertNotEquals(new PathPosition(0, 0, 0), "(0, 0, 0)");
    assertNotEquals(new PathPosition(0, 0, 0), null);
  }

  /*
   * distance uses exact Math.sqrt, not the former bit-hack approximation. A 3-4-5 triangle must
   * resolve to exactly 5.0; the old approximation drifted by ~1e-6 and would miss the tight delta.
   */
  @Test
  void distanceIsExact() {
    PathPosition origin = new PathPosition(0, 0, 0);
    assertEquals(5.0, origin.distance(new PathPosition(3, 0, 4)), 0.0);
  }

  /* distance to self is exactly 0.0; the old approximation returned a tiny non-zero value. */
  @Test
  void distanceToSelfIsZero() {
    PathPosition p = new PathPosition(7, 11, 13);
    assertEquals(0.0, p.distance(p), 0.0);
  }
}
