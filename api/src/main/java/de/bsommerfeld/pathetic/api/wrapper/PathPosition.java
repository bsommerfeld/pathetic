package de.bsommerfeld.pathetic.api.wrapper;

import de.bsommerfeld.pathetic.api.util.NumberUtils;
import java.util.Objects;

/**
 * Represents a position. This class encapsulates the coordinates (x, y, z) of a point in the
 * pathfinding environment and provides methods for manipulating and comparing positions.
 */
public class PathPosition implements Cloneable {

  private double x;
  private double y;
  private double z;

  /**
   * Constructs a {@code PathPosition} with the specified coordinates.
   *
   * @param x The x-coordinate of the position.
   * @param y The y-coordinate of the position.
   * @param z The z-coordinate of the position.
   */
  public PathPosition(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Interpolates between this position and another position based on a given progress value.
   *
   * @param other The other position to interpolate towards.
   * @param progress The interpolation progress, typically between 0.0 (this position) and 1.0
   *     (other position).
   * @return A new {@code PathPosition} representing the interpolated point.
   */
  public PathPosition interpolate(PathPosition other, double progress) {
    double x = NumberUtils.interpolate(this.x, other.x, progress);
    double y = NumberUtils.interpolate(this.y, other.y, progress);
    double z = NumberUtils.interpolate(this.z, other.z, progress);
    return new PathPosition(x, y, z);
  }

  /**
   * Checks if this position and another position are within the same block in the environment.
   *
   * @param otherPosition The other position to compare with.
   * @return {@code true} if both positions share the same block coordinates (floored x, y, z
   *     values), {@code false} otherwise.
   */
  public boolean isInSameBlock(PathPosition otherPosition) {
    return this.getFlooredX() == otherPosition.getFlooredX()
        && this.getFlooredY() == otherPosition.getFlooredY()
        && this.getFlooredZ() == otherPosition.getFlooredZ();
  }

  /**
   * Calculates the Manhattan distance between this position and another position. Manhattan
   * distance is the sum of the absolute differences of their coordinates.
   *
   * @param otherPosition The other position to calculate the distance to.
   * @return The Manhattan distance between the two positions.
   */
  public int manhattanDistance(PathPosition otherPosition) {
    return Math.abs(this.getFlooredX() - otherPosition.getFlooredX())
        + Math.abs(this.getFlooredY() - otherPosition.getFlooredY())
        + Math.abs(this.getFlooredZ() - otherPosition.getFlooredZ());
  }

  /**
   * Calculates the Octile distance between this position and another position. Octile distance is a
   * more accurate approximation of diagonal distance in a grid-based environment compared to
   * Manhattan distance.
   *
   * @param otherPosition The other position to calculate the distance to.
   * @return The Octile distance between the two positions.
   */
  public double octileDistance(PathPosition otherPosition) {

    double dx = Math.abs(this.x - otherPosition.x);
    double dy = Math.abs(this.y - otherPosition.y);
    double dz = Math.abs(this.z - otherPosition.z);

    double smallest = Math.min(Math.min(dx, dz), dy);
    double highest = Math.max(Math.max(dx, dz), dy);
    double mid = Math.max(Math.min(dx, dz), Math.min(Math.max(dx, dz), dy));

    double D1 = 1;
    double D2 = 1.4142135623730951;
    double D3 = 1.7320508075688772;

    return (D3 - D2) * smallest + (D2 - D1) * mid + D1 * highest;
  }

  /**
   * Calculates the squared distance between this position and another position.
   *
   * @param otherPosition The other position to calculate the distance to.
   * @return The squared distance between the two positions.
   */
  public double distanceSquared(PathPosition otherPosition) {
    return NumberUtils.square(this.x - otherPosition.x)
        + NumberUtils.square(this.y - otherPosition.y)
        + NumberUtils.square(this.z - otherPosition.z);
  }

  /**
   * Calculates the Euclidean distance between this position and another position.
   *
   * @param otherPosition The other position to calculate the distance to.
   * @return The Euclidean distance between the two positions.
   */
  public double distance(PathPosition otherPosition) {
    return NumberUtils.sqrt(this.distanceSquared(otherPosition));
  }

  /**
   * Creates a new {@code PathPosition} with the same coordinates as this one, but with the
   * x-coordinate set to the given value.
   *
   * @param x The new x-coordinate.
   * @return A new {@code PathPosition} with the updated x-coordinate.
   */
  public PathPosition setX(double x) {
    return new PathPosition(x, this.y, this.z);
  }

  /**
   * Creates a new {@code PathPosition} with the same coordinates as this one, but with the
   * y-coordinate set to the given value.
   *
   * @param y The new y-coordinate.
   * @return A new {@code PathPosition} with the updated y-coordinate.
   */
  public PathPosition setY(double y) {
    return new PathPosition(this.x, y, this.z);
  }

  /**
   * Creates a new {@code PathPosition} with the same coordinates as this one, but with the
   * z-coordinate set to the given value.
   *
   * @param z The new z-coordinate.
   * @return A new {@code PathPosition} with the updated z-coordinate.
   */
  public PathPosition setZ(double z) {
    return new PathPosition(this.x, this.y, z);
  }

  /**
   * Returns the x-coordinate of the block this position is located in. This is equivalent to
   * flooring the x-coordinate.
   *
   * @return The floored x-coordinate.
   */
  public int getFlooredX() {
    return (int) Math.floor(this.x);
  }

  /**
   * Returns the y-coordinate of the block this position is located in. This is equivalent to
   * flooring the y-coordinate.
   *
   * @return The floored y-coordinate.
   */
  public int getFlooredY() {
    return (int) Math.floor(this.y);
  }

  /**
   * Returns the z-coordinate of the block this position is located in. This is equivalent to
   * flooring the z-coordinate.
   *
   * @return The floored z-coordinate.
   */
  public int getFlooredZ() {
    return (int) Math.floor(this.z);
  }

  /**
   * Creates a new {@code PathPosition} by adding the given values to the coordinates of this
   * position.
   *
   * @param x The value to add to the x-coordinate.
   * @param y The value to add to the y-coordinate.
   * @param z The value to add to the z-coordinate.
   * @return A new {@code PathPosition} with the added values.
   */
  public PathPosition add(final double x, final double y, final double z) {
    return new PathPosition(this.x + x, this.y + y, this.z + z);
  }

  /**
   * Creates a new {@code PathPosition} by adding the components of the given vector to the
   * coordinates of this position.
   *
   * @param vector The vector to add.
   * @return A new {@code PathPosition} with the added vector components.
   */
  public PathPosition add(final PathVector vector) {
    return add(vector.getX(), vector.getY(), vector.getZ());
  }

  /**
   * Creates a new {@code PathPosition} by subtracting the given values from the coordinates of this
   * position.
   *
   * @param x The value to subtract from the x-coordinate.
   * @param y The value to subtract from the y-coordinate.
   * @param z The value to subtract from the z-coordinate.
   * @return A new {@code PathPosition} with the subtracted values.
   */
  public PathPosition subtract(final double x, final double y, final double z) {
    return new PathPosition(this.x - x, this.y - y, this.z - z);
  }

  /**
   * Creates a new {@code PathPosition} by subtracting the components of the given vector from the
   * coordinates of this position.
   *
   * @param vector The vector to subtract.
   * @return A new {@code PathPosition} with the subtracted vector components.
   */
  public PathPosition subtract(final PathVector vector) {
    return subtract(vector.getX(), vector.getY(), vector.getZ());
  }

  /**
   * Creates a new {@link PathVector} from the coordinates of this position.
   *
   * @return A new {@code PathVector} representing this position's coordinates.
   */
  public PathVector toVector() {
    return new PathVector(this.x, this.y, this.z);
  }

  /**
   * Creates a new {@code PathPosition} with the coordinates floored to the nearest integer values.
   *
   * @return A new {@code PathPosition} with floored coordinates.
   */
  public PathPosition floor() {
    return new PathPosition(this.getFlooredX(), this.getFlooredY(), this.getFlooredZ());
  }

  /**
   * Creates a new {@code PathPosition} with the coordinates set to the center of the block they are
   * in.
   *
   * @return A new {@code PathPosition} at the center of the block.
   */
  public PathPosition mid() {
    return new PathPosition(
        this.getFlooredX() + 0.5, this.getFlooredY() + 0.5, this.getFlooredZ() + 0.5);
  }

  /**
   * Calculates the midpoint between this position and another position.
   *
   * @param end The other position to calculate the midpoint with.
   * @return A new {@code PathPosition} representing the midpoint.
   */
  public PathPosition midPoint(PathPosition end) {
    return new PathPosition((this.x + end.x) / 2, (this.y + end.y) / 2, (this.z + end.z) / 2);
  }

  @Override
  public PathPosition clone() {

    final PathPosition clone;
    try {
      clone = (PathPosition) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException("Superclass messed up", ex);
    }

    clone.x = this.x;
    clone.y = this.y;
    clone.z = this.z;
    return clone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PathPosition that = (PathPosition) o;
    return x == that.x && y == that.y && z == that.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z);
  }

  /**
   * Returns the x-coordinate of this position.
   *
   * @return The x-coordinate.
   */
  public double getX() {
    return this.x;
  }

  /**
   * Returns the y-coordinate of this position.
   *
   * @return The y-coordinate.
   */
  public double getY() {
    return this.y;
  }

  /**
   * Returns the z-coordinate of this position.
   *
   * @return The z-coordinate.
   */
  public double getZ() {
    return this.z;
  }

  public String toString() {
    return ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ")";
  }
}
