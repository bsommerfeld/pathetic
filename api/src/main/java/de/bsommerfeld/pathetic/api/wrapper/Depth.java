package de.bsommerfeld.pathetic.api.wrapper;

/**
 * Represents the depth of a pathfinding node or element. This class provides methods to get and
 * increment the depth value.
 */
public class Depth {

  private int value;

  private Depth(int value) {
    this.value = value;
  }

  /**
   * Creates a new {@code Depth} instance with the specified value.
   *
   * @param value The initial depth value.
   * @return A new {@code Depth} instance.
   */
  public static Depth of(int value) {
    return new Depth(value);
  }

  /** Increments the depth value by one. */
  public void increment() {
    value++;
  }

  /**
   * Returns the current depth value.
   *
   * @deprecated {@link #value()}
   * @return The current depth value.
   */
  @Deprecated
  public int getValue() {
    return this.value;
  }

  /**
   * Returns the current depth value.
   *
   * @return The current depth value.
   */
  public int value() {
    return this.value;
  }

  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof Depth)) return false;
    final Depth other = (Depth) o;
    if (!other.canEqual((Object) this)) return false;
    if (this.getValue() != other.getValue()) return false;
    return true;
  }

  protected boolean canEqual(final Object other) {
    return other instanceof Depth;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + this.getValue();
    return result;
  }
}
