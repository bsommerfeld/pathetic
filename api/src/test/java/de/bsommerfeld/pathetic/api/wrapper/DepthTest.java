package de.bsommerfeld.pathetic.api.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class DepthTest {

  /* Depth is a value type: no mutating increment method, final backing field, final class. */
  @Test
  void hasNoIncrementMethod() throws Exception {
    for (Method method : Depth.class.getDeclaredMethods()) {
      assertNotEquals(
          "increment",
          method.getName(),
          "Depth.increment() must not exist - Depth is a value type and must stay immutable");
    }
  }

  @Test
  void valueFieldIsFinal() throws NoSuchFieldException {
    Field value = Depth.class.getDeclaredField("value");
    assertEquals(
        true,
        Modifier.isFinal(value.getModifiers()),
        "Depth.value must be final to enforce immutability");
  }

  @Test
  void classIsFinal() {
    assertEquals(
        true,
        Modifier.isFinal(Depth.class.getModifiers()),
        "Depth must be final - it is a value type, not an extension point");
  }

  /* Value semantics: equals/hashCode based on the numeric value, of() returns fresh instances. */
  @Test
  void factoryReturnsIndependentInstances() {
    Depth a = Depth.of(7);
    Depth b = Depth.of(7);
    assertNotSame(a, b, "of() should return a fresh instance per call");
    assertEquals(a, b, "instances with the same value must compare equal");
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void valueAccessorsReturnInitialValue() {
    Depth d = Depth.of(42);
    assertEquals(42, d.value());
    assertEquals(42, d.getValue());
  }

  @Test
  void differentValuesAreUnequal() {
    assertNotEquals(Depth.of(0), Depth.of(1));
    assertNotEquals(Depth.of(0).hashCode(), Depth.of(1).hashCode());
  }

  @Test
  void notEqualToNonDepthObject() {
    assertNotEquals(Depth.of(5), "5");
    assertNotEquals(Depth.of(5), null);
  }
}
