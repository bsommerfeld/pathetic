package de.bsommerfeld.pathetic.api.pathing.processing;

/**
 * Represents a cost value used in pathfinding. This class ensures that cost values can be handled
 * consistently.
 */
public final class Cost {
  private final double value;

  /** A shared instance for zero cost. */
  public static final Cost ZERO = new Cost(0.0);

  /**
   * Private constructor to control instantiation, primarily through the static factory method.
   *
   * @param value The numerical cost.
   */
  private Cost(double value) {
    this.value = value;
  }

  /**
   * Returns the numerical cost value.
   *
   * @return The cost value.
   */
  public double getValue() {
    return value;
  }

  /**
   * Factory method to create a {@code Cost} instance.
   *
   * @param value The numerical cost.
   * @return A new {@code Cost} instance.
   */
  public static Cost of(double value) {
    return new Cost(value);
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
