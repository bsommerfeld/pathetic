package de.bsommerfeld.pathetic.api.pathing.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CostTest {

  /*
   * Cost.of rejects every non-finite operand at the boundary so pathfinding arithmetic stays
   * free of NaN / Infinity propagation.
   */
  @Test
  void rejectsPositiveInfinity() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Cost.of(Double.POSITIVE_INFINITY));
    assertTrue(ex.getMessage().contains("finite"));
  }

  @Test
  void rejectsNegativeInfinity() {
    assertThrows(IllegalArgumentException.class, () -> Cost.of(Double.NEGATIVE_INFINITY));
  }

  @Test
  void rejectsNaN() {
    assertThrows(IllegalArgumentException.class, () -> Cost.of(Double.NaN));
  }

  @Test
  void rejectsNegativeValue() {
    assertThrows(IllegalArgumentException.class, () -> Cost.of(-0.0001));
  }

  /* Boundary values that must be accepted: zero, smallest positive, largest finite, typical. */
  @Test
  void acceptsZero() {
    Cost zero = Cost.of(0.0);
    assertEquals(0.0, zero.value());
  }

  @Test
  void acceptsTinyPositive() {
    assertEquals(Double.MIN_VALUE, Cost.of(Double.MIN_VALUE).value());
  }

  @Test
  void acceptsLargeFinitePositive() {
    assertEquals(Double.MAX_VALUE, Cost.of(Double.MAX_VALUE).value());
  }

  @Test
  void acceptsTypicalPositive() {
    assertEquals(3.14, Cost.of(3.14).value());
  }

  /* Cost.ZERO singleton equals Cost.of(0.0). */
  @Test
  void zeroSingletonHasValueZero() {
    assertEquals(0.0, Cost.ZERO.value());
    assertEquals(Cost.ZERO, Cost.of(0.0));
  }

  /* Value semantics on equals/hashCode. */
  @Test
  void equalsAndHashCode() {
    Cost a = Cost.of(2.5);
    Cost b = Cost.of(2.5);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, Cost.of(2.6));
  }
}
