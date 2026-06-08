package de.bsommerfeld.pathetic.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberUtilsTest {

  /*
   * sqrt now delegates to the exact JDK intrinsic. It must match Math.sqrt across the range,
   * including 0.0 where the former bit-hack approximation produced a tiny non-zero value.
   */
  @Test
  @SuppressWarnings("deprecation")
  void sqrtMatchesMathSqrt() {
    for (double x : new double[] {0.0, 1e-6, 0.5, 1.0, 2.0, 25.0, 1e6, 1e12}) {
      assertEquals(Math.sqrt(x), NumberUtils.sqrt(x), 0.0, "sqrt mismatch at " + x);
    }
  }
}
