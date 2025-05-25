package de.bsommerfeld.pathetic.api.util;

/** Utility class for common number operations. */
public final class NumberUtils {

  private NumberUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Interpolates between two values based on the given progress.
   *
   * @param a the start value
   * @param b the end value
   * @param progress the interpolation progress (0.0 to 1.0)
   * @return the interpolated value
   */
  public static double interpolate(double a, double b, double progress) {
    return a + (b - a) * progress;
  }

  /**
   * Squares the given value.
   *
   * @param value the value to be squared
   * @return the squared value
   */
  public static double square(double value) {
    return value * value;
  }

  /**
   * Computes the square root of the given value using an approximation method.
   *
   * @param input the value to compute the square root of
   * @return the approximated square root
   */
  public static double sqrt(double input) {
    double sqrt =
        Double.longBitsToDouble((Double.doubleToLongBits(input) - (1L << 52) >> 1) + (1L << 61));
    double better = (sqrt + input / sqrt) / 2.0;

    return (better + input / better) / 2.0;
  }
}
