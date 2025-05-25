package de.bsommerfeld.pathetic.api.util;

/**
 * A functional interface representing a supplier that accepts a parameter and returns a result.
 *
 * @param <T> the type of the input to the supplier and the result
 */
@FunctionalInterface
public interface ParameterizedSupplier<T> {

  /**
   * Applies this supplier to the given argument.
   *
   * @param value the input argument
   * @return the supplier result
   */
  T accept(T value);
}
