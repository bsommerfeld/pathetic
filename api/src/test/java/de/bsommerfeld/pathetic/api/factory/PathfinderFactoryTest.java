package de.bsommerfeld.pathetic.api.factory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class PathfinderFactoryTest {

  /*
   * The no-arg factory builds with an always-traversable provider and is therefore unsafe for
   * any real use; the @Deprecated marker must stay present so callers get a compiler warning.
   */
  @Test
  void noArgCreatePathfinderIsDeprecated() throws NoSuchMethodException {
    Method method = PathfinderFactory.class.getDeclaredMethod("createPathfinder");
    assertTrue(
        method.isAnnotationPresent(Deprecated.class),
        "createPathfinder() must remain @Deprecated - it builds with the default always-true provider");
  }
}
