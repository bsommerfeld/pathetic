package de.bsommerfeld.pathetic.engine.result;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.util.NumberUtils;
import de.bsommerfeld.pathetic.api.util.ParameterizedSupplier;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Utility class providing post-processing operations on {@link Path} objects.
 *
 * <p>All methods are <strong>static</strong>, <strong>immutable</strong> (return new paths), and
 * use {@link ArrayDeque} internally for optimal performance when building sequences.
 *
 * <p>These operations are <em>not</em> part of core pathfinding but are essential for path
 * refinement, visualization, animation, or coordinate transformation.
 *
 * @see Path
 * @see PathPosition
 * @since 5.3.3
 * @deprecated Pathetic's future focus will rely heavily on finding paths, not modifying or
 *     post-processing them. This class is maintained for backward compatibility until a dedicated
 *     post-processing module is available.
 */
@Deprecated
public final class PathUtils {

  /** Private constructor to prevent instantiation. This is a utility class. */
  private PathUtils() {
    throw new AssertionError("Utility class - instantiation not allowed");
  }

  /**
   * Interpolates a path by inserting intermediate positions between each pair of consecutive
   * points.
   *
   * <p>For every segment from {@code start} to {@code end}, this method calculates the Euclidean
   * distance and inserts points such that no two consecutive points are farther apart than {@code
   * resolution}.
   *
   * <p>Useful for smooth movement, dense sampling, or visualization.
   *
   * @param path the original path to interpolate
   * @param resolution the maximum allowed distance between any two consecutive points in the result
   * @return a new path with interpolated positions
   * @throws IllegalArgumentException if {@code resolution <= 0}
   * @throws NullPointerException if {@code path} is {@code null}
   */
  public static Path interpolate(Path path, double resolution) {
    if (resolution <= 0) {
      throw new IllegalArgumentException("Resolution must be > 0");
    }

    Deque<PathPosition> result = new ArrayDeque<>();
    PathPosition previous = null;

    for (PathPosition current : path) {
      if (previous != null) {
        interpolateSegment(previous, current, resolution, result);
      }
      result.addLast(current);
      previous = current;
    }

    return buildPath(result);
  }

  /**
   * Simplifies a path by retaining only every nth point based on an epsilon value.
   *
   * <p>The stride is calculated as {@code max(1, round(1.0 / epsilon))}, meaning:
   *
   * <ul>
   *   <li>{@code epsilon = 1.0} → keep every point
   *   <li>{@code epsilon = 0.5} → keep every 2nd point
   *   <li>{@code epsilon = 0.1} → keep every 10th point
   * </ul>
   *
   * <p>This is a lightweight downsampling method. For geometric simplification (e.g.
   * Ramer-Douglas-Peucker), use a dedicated algorithm.
   *
   * @param path the path to simplify
   * @param epsilon a value in (0.0, 1.0] controlling sampling density
   * @return a new simplified path
   * @throws IllegalArgumentException if {@code epsilon} is not in (0.0, 1.0]
   */
  public static Path simplify(Path path, double epsilon) {
    validateEpsilon(epsilon);

    Deque<PathPosition> result = new ArrayDeque<>();
    int index = 0;
    int stride = Math.max(1, (int) Math.round(1.0 / epsilon));

    for (PathPosition pos : path) {
      if (index % stride == 0) {
        result.addLast(pos);
      }
      index++;
    }

    return buildPath(result);
  }

  /**
   * Concatenates two paths into a single continuous path.
   *
   * <p>The end of the first path is connected to the start of the second. If either path is empty,
   * the other is returned.
   *
   * @param first the first path segment
   * @param second the second path segment
   * @return a new path combining both
   * @throws NullPointerException if either argument is {@code null}
   */
  public static Path join(Path first, Path second) {
    if (first.length() == 0) return second;
    if (second.length() == 0) return first;

    Deque<PathPosition> result = new ArrayDeque<>();
    for (PathPosition p : first) result.addLast(p);
    for (PathPosition p : second) result.addLast(p);

    return buildPath(result);
  }

  /**
   * Truncates a path to a maximum number of positions.
   *
   * <p>If the path has fewer than {@code maxLength} positions, it is returned unchanged. Otherwise,
   * only the first {@code maxLength} positions are kept.
   *
   * @param path the path to trim
   * @param maxLength the maximum number of positions to retain
   * @return a new path with at most {@code maxLength} positions
   * @throws IllegalArgumentException if {@code maxLength <= 0}
   */
  public static Path trim(Path path, int maxLength) {
    if (maxLength <= 0) {
      throw new IllegalArgumentException("maxLength must be > 0");
    }
    if (path.length() <= maxLength) {
      return path;
    }

    Deque<PathPosition> result = new ArrayDeque<>();
    int count = 0;
    for (PathPosition p : path) {
      result.addLast(p);
      if (++count >= maxLength) break;
    }

    return buildPath(result);
  }

  /**
   * Applies a transformation function to every position in the path.
   *
   * <p>This is a functional map operation: each {@link PathPosition} is passed to the {@link
   * ParameterizedSupplier}, and the returned position is included in the new path.
   *
   * <p>Use cases:
   *
   * <ul>
   *   <li>Coordinate system conversion (world → screen)
   *   <li>Scaling, rotation, offset
   *   <li>Adding metadata (e.g. timestamp, color)
   *   <li>Path animation (e.g. oscillation)
   * </ul>
   *
   * @param path the original path
   * @param mutator a function that transforms one position into another
   * @return a new path with transformed positions
   * @throws NullPointerException if {@code mutator} is {@code null}
   */
  public static Path mutatePositions(Path path, ParameterizedSupplier<PathPosition> mutator) {
    Deque<PathPosition> result = new ArrayDeque<>(path.length());

    for (PathPosition pos : path) {
      result.addLast(mutator.accept(pos));
    }

    return buildPath(result);
  }

  // ===========================================================================
  // Private Helpers
  // ===========================================================================

  /** Inserts interpolated points between two positions. */
  private static void interpolateSegment(
      PathPosition start, PathPosition end, double resolution, Deque<PathPosition> result) {

    double distance = start.distance(end);
    int steps = (int) Math.ceil(distance / resolution);

    for (int i = 1; i < steps; i++) {
      double progress = (double) i / steps;
      result.addLast(interpolate(start, end, progress));
    }
  }

  /**
   * Interpolates between this position and another position based on a given progress value.
   *
   * @param progress The interpolation progress, typically between 0.0 (this position) and 1.0
   *     (other position).
   * @return A new {@code PathPosition} representing the interpolated point.
   */
  private static PathPosition interpolate(PathPosition pos1, PathPosition pos2, double progress) {
    double x = NumberUtils.interpolate(pos1.getX(), pos2.getX(), progress);
    double y = NumberUtils.interpolate(pos1.getY(), pos2.getY(), progress);
    double z = NumberUtils.interpolate(pos1.getZ(), pos2.getZ(), progress);
    return new PathPosition(x, y, z);
  }

  /**
   * Constructs a {@link Path} from a deque of positions.
   *
   * <p>Ensures the path is non-empty and uses the first and last elements as start/end.
   *
   * @param positions the sequence of positions
   * @return a new {@link PathImpl}
   * @throws IllegalArgumentException if positions is empty
   */
  private static Path buildPath(Deque<PathPosition> positions) {
    if (positions.isEmpty()) {
      throw new IllegalArgumentException("Cannot build path from empty position list");
    }
    PathImpl path = new PathImpl(positions.peekFirst(), positions.peekLast(), positions);
    return removeDuplicates(path);
  }

  /**
   * Removes duplicate consecutive positions while preserving order.
   *
   * <p>Only directly consecutive duplicates are removed. Comparison is done on exact coordinates
   * (with a small EPS), not via {@code equals()} or floored coordinates.
   */
  private static Path removeDuplicates(Path path) {
    final double EPS = 1e-12;

    Deque<PathPosition> result = new ArrayDeque<>();
    PathPosition last = null;

    for (PathPosition pos : path) {
      if (last == null || !samePoint(last, pos, EPS)) {
        result.addLast(pos);
        last = pos;
      }
    }

    return new PathImpl(result.peekFirst(), result.peekLast(), result);
  }

  private static boolean samePoint(PathPosition a, PathPosition b, double eps) {
    return Math.abs(a.getX() - b.getX()) <= eps
        && Math.abs(a.getY() - b.getY()) <= eps
        && Math.abs(a.getZ() - b.getZ()) <= eps;
  }

  /** Validates that epsilon is in the range (0.0, 1.0]. */
  private static void validateEpsilon(double epsilon) {
    if (epsilon <= 0.0 || epsilon > 1.0) {
      throw new IllegalArgumentException("Epsilon must be in (0.0, 1.0]");
    }
  }
}
