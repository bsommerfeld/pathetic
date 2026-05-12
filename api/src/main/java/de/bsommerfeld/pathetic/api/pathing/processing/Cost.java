package de.bsommerfeld.pathetic.api.pathing.processing;

/**
 * Represents a cost value used in pathfinding. This class ensures that cost values can be handled
 * consistently.
 */
public final class Cost {
  /** A shared instance for zero cost. */
  public static final Cost ZERO = new Cost(0.0);

  private final double value;

  /**
   * Private constructor to control instantiation, primarily through the static factory method.
   *
   * @param value The numerical cost.
   */
  private Cost(double value) {
    this.value = value;
  }

  /**
   * Factory method to create a {@code Cost} instance.
   *
   * <p>The value must be a finite non-negative number. {@code NaN}, {@code +Infinity},
   * {@code -Infinity}, and negative values are rejected at the boundary so that downstream
   * pathfinding arithmetic (G-cost accumulation, F-cost, heap keys, tie-breakers) cannot inherit
   * non-finite operands.
   *
   * @param value The numerical cost.
   * @return A new {@code Cost} instance.
   * @throws IllegalArgumentException if {@code value} is NaN, infinite, or negative.
   */
  public static Cost of(double value) {
    if (!Double.isFinite(value) || value < 0)
      throw new IllegalArgumentException("Cost must be a finite non-negative number, was " + value);

    return new Cost(value);
  }

  /**
   * Returns the numerical cost value.
   *
   * @return The cost value.
   * @deprecated Use {@link #value()} instead.
   */
  @Deprecated
  public double getValue() {
    return value;
  }

  /**
   * Returns the numerical cost value.
   *
   * @return The cost value.
   */
  public double value() {
    return this.value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Cost cost = (Cost) o;
    return Double.compare(cost.value, value) == 0;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(value);
  }

  @Override
  public String toString() {
    return "Cost{" + "value=" + value + '}';
  }
}
