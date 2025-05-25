package de.bsommerfeld.pathetic.api.wrapper;

import de.bsommerfeld.pathetic.api.util.NumberUtils;

/**
 * Represents a 3D vector within a pathfinding context. This class encapsulates the x, y, and z
 * components of a vector and provides methods for vector operations such as addition, subtraction,
 * dot product, cross product, and normalization.
 */
public class PathVector implements Cloneable {

  private double x;
  private double y;
  private double z;

  /**
   * Constructs a {@code PathVector} with the specified x, y, and z components.
   *
   * @param x The x-component of the vector.
   * @param y The y-component of the vector.
   * @param z The z-component of the vector.
   */
  public PathVector(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Computes the distance between a point and a line segment.
   *
   * @param A The point represented as a {@code PathVector}.
   * @param B The first endpoint of the line segment represented as a {@code PathVector}.
   * @param C The second endpoint of the line segment represented as a {@code PathVector}.
   * @return The distance between the point A and the line segment BC.
   */
  public static double computeDistance(PathVector A, PathVector B, PathVector C) {

    PathVector d = C.subtract(B).divide(C.distance(B));
    PathVector v = A.subtract(B);

    double t = v.dot(d);
    PathVector P = B.add(d.multiply(t));

    return P.distance(A);
  }

  /**
   * Calculates the dot product of this vector and another vector.
   *
   * @param otherVector The other vector to calculate the dot product with.
   * @return The dot product of the two vectors.
   */
  public double dot(PathVector otherVector) {
    return this.x * otherVector.x + this.y * otherVector.y + this.z * otherVector.z;
  }

  /**
   * Calculates the length (magnitude) of this vector.
   *
   * @return The length of the vector.
   */
  public double length() {
    return Math.sqrt(
        NumberUtils.square(this.x) + NumberUtils.square(this.y) + NumberUtils.square(this.z));
  }

  /**
   * Calculates the Euclidean distance between this vector and another vector.
   *
   * @param otherVector The other vector to calculate the distance to.
   * @return The distance between the two vectors.
   */
  public double distance(PathVector otherVector) {
    return Math.sqrt(
        NumberUtils.square(this.x - otherVector.x)
            + NumberUtils.square(this.y - otherVector.y)
            + NumberUtils.square(this.z - otherVector.z));
  }

  /**
   * Creates a new {@code PathVector} with the same y and z components as this vector, but with the
   * x-component set to the given value.
   *
   * @param x The new x-component.
   * @return A new {@code PathVector} with the updated x-component.
   */
  public PathVector setX(double x) {
    return new PathVector(x, this.y, this.z);
  }

  /**
   * Creates a new {@code PathVector} with the same x and z components as this vector, but with the
   * y-component set to the given value.
   *
   * @param y The new y-component.
   * @return A new {@code PathVector} with the updated y-component.
   */
  public PathVector setY(double y) {
    return new PathVector(this.x, y, this.z);
  }

  /**
   * Creates a new {@code PathVector} with the same x and y components as this vector, but with the
   * z-component set to the given value.
   *
   * @param z The new z-component.
   * @return A new {@code PathVector} with the updated z-component.
   */
  public PathVector setZ(double z) {
    return new PathVector(this.x, this.y, z);
  }

  /**
   * Creates a new {@code PathVector} by subtracting another vector from this vector.
   *
   * @param otherVector The vector to subtract from this vector.
   * @return A new {@code PathVector} representing the difference.
   */
  public PathVector subtract(PathVector otherVector) {
    return new PathVector(this.x - otherVector.x, this.y - otherVector.y, this.z - otherVector.z);
  }

  /**
   * Creates a new {@code PathVector} by multiplying this vector by a scalar value.
   *
   * @param value The scalar value to multiply by.
   * @return A new {@code PathVector} representing the scaled vector.
   */
  public PathVector multiply(double value) {
    return new PathVector(this.x * value, this.y * value, this.z * value);
  }

  /**
   * Creates a new {@code PathVector} by normalizing this vector. Normalization divides each
   * component of the vector by its magnitude, resulting in a unit vector (length of 1).
   *
   * @return A new {@code PathVector} representing the normalized vector.
   */
  public PathVector normalize() {
    double magnitude = this.length();
    return new PathVector(this.x / magnitude, this.y / magnitude, this.z / magnitude);
  }

  /**
   * Creates a new {@code PathVector} by dividing this vector by a scalar value.
   *
   * @param value The scalar value to divide by.
   * @return A new {@code PathVector} representing the divided vector.
   */
  public PathVector divide(double value) {
    return new PathVector(this.x / value, this.y / value, this.z / value);
  }

  /**
   * Creates a new {@code PathVector} by adding another vector to this vector.
   *
   * @param otherVector The vector to add to this vector.
   * @return A new {@code PathVector} representing the sum.
   */
  public PathVector add(PathVector otherVector) {
    return new PathVector(this.x + otherVector.x, this.y + otherVector.y, this.z + otherVector.z);
  }

  /**
   * Calculates the cross product of this vector and another vector.
   *
   * @param o The other vector to calculate the cross product with.
   * @return A new {@code PathVector} representing the cross product.
   */
  public PathVector getCrossProduct(PathVector o) {
    double x = this.y * o.getZ() - o.getY() * this.z;
    double y = this.z * o.getX() - o.getZ() * this.x;
    double z = this.x * o.getY() - o.getX() * this.y;
    return new PathVector(x, y, z);
  }

  @Override
  public PathVector clone() {
    final PathVector clone;
    try {
      clone = (PathVector) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException("Superclass messed up", ex);
    }

    clone.x = this.x;
    clone.y = this.y;
    clone.z = this.z;
    return clone;
  }

  /**
   * Returns the x-component of this vector.
   *
   * @return The x-component.
   */
  public double getX() {
    return this.x;
  }

  /**
   * Returns the y-component of this vector.
   *
   * @return The y-component.
   */
  public double getY() {
    return this.y;
  }

  /**
   * Returns the z-component of this vector.
   *
   * @return The z-component.
   */
  public double getZ() {
    return this.z;
  }

  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof PathVector)) return false;
    final PathVector other = (PathVector) o;
    if (!other.canEqual(this)) return false;
    if (Double.compare(this.getX(), other.getX()) != 0) return false;
    if (Double.compare(this.getY(), other.getY()) != 0) return false;
    if (Double.compare(this.getZ(), other.getZ()) != 0) return false;
    return true;
  }

  protected boolean canEqual(final Object other) {
    return other instanceof PathVector;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final long $x = Double.doubleToLongBits(this.getX());
    result = result * PRIME + (int) ($x >>> 32 ^ $x);
    final long $y = Double.doubleToLongBits(this.getY());
    result = result * PRIME + (int) ($y >>> 32 ^ $y);
    final long $z = Double.doubleToLongBits(this.getZ());
    result = result * PRIME + (int) ($z >>> 32 ^ $z);
    return result;
  }
}
