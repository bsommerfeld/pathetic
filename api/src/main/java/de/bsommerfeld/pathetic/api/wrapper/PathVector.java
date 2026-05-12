package de.bsommerfeld.pathetic.api.wrapper;

import de.bsommerfeld.pathetic.api.util.NumberUtils;
import java.util.Objects;

/**
 * Represents a 3D vector within a pathfinding context. This class encapsulates the x, y, and z
 * components of a vector and provides methods for vector operations such as addition, subtraction,
 * dot product, cross product, and normalization.
 */
public class PathVector {

  private final double x;
  private final double y;
  private final double z;

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
   * Creates a new {@code PathVector} instance with the specified components.
   *
   * @param x The x-component of the vector.
   * @param y The y-component of the vector.
   * @param z The z-component of the vector.
   * @return A new {@code PathVector} instance.
   */
  public static PathVector of(double x, double y, double z) {
    return new PathVector(x, y, z);
  }

  /**
   * Computes the perpendicular distance from point {@code A} to the infinite line defined by points
   * {@code B} and {@code C}.
   *
   * <p>If {@code B} and {@code C} coincide (or are numerically indistinguishable), the line
   * degenerates to a single point and the Euclidean distance from {@code A} to {@code B} is
   * returned.
   *
   * @param A The point represented as a {@code PathVector}. Must not be {@code null}.
   * @param B The first reference point of the line represented as a {@code PathVector}. Must not be
   *     {@code null}.
   * @param C The second reference point of the line represented as a {@code PathVector}. Must not
   *     be {@code null}.
   * @return The perpendicular distance from {@code A} to the line through {@code B} and {@code C},
   *     or the distance from {@code A} to {@code B} if the line degenerates.
   * @throws NullPointerException if any argument is {@code null}.
   */
  public static double computeDistance(PathVector A, PathVector B, PathVector C) {
    Objects.requireNonNull(A, "A must not be null");
    Objects.requireNonNull(B, "B must not be null");
    Objects.requireNonNull(C, "C must not be null");

    double lineLength = C.distance(B);
    if (lineLength == 0.0 || !Double.isFinite(lineLength)) {
      // B and C coincide -> line degenerates to a point; fall back to point-to-point distance.
      return A.distance(B);
    }

    PathVector d = C.subtract(B).divide(lineLength);
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
