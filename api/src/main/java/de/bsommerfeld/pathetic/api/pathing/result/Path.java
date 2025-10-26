package de.bsommerfeld.pathetic.api.pathing.result;

import de.bsommerfeld.pathetic.api.util.ParameterizedSupplier;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.Collection;

/**
 * A Path is a sequence of positions that represents a path through a 3D space. The positions are
 * ordered such that the first position is the start of the path and the last position is the end of
 * the path. The path may contain additional positions between the start and end positions.
 */
public interface Path extends Iterable<PathPosition> {

  /**
   * The length of the Path compiled from the number of positions
   *
   * @return the length of the path
   */
  int length();

  /**
   * Interpolates the positions of this Path to a new Path with the given resolution.
   *
   * <p>The resulting path will have additional positions inserted between consecutive positions in
   * the original path such that no two consecutive positions are more than `resolution` blocks
   * apart. The interpolated positions are computed using linear interpolation, which means that the
   * resulting path will have a smooth curve that passes through each of the original positions.
   *
   * @param resolution The desired distance between consecutive positions in the resulting path, in
   *     blocks. A lower value will result in a higher resolution and a smoother curve, but will
   *     also increase the number of positions in the resulting path and possibly reduce
   *     performance.
   * @return a newly created Path with interpolated positions.
   */
  Path interpolate(double resolution);

  /**
   * Simplifies the path by removing intermediate positions based on the given epsilon value. The
   * start and end positions are always included in the simplified path.
   *
   * @param epsilon the epsilon value representing the fraction of positions to keep (should be in
   *     the range greater than 0.0 to 1.0, inclusive)
   * @return a simplified path containing a subset of positions from the original path
   * @throws IllegalArgumentException if epsilon is not in the range greater than 0.0 to 1.0,
   *     inclusive
   */
  Path simplify(double epsilon);

  /**
   * Joins this Path with the given Path.
   *
   * @param path which will be appended at the end.
   * @return {@link Path} the new Path
   */
  Path join(Path path);

  /**
   * Trims this Path to the given length.
   *
   * @param length the length to which the Path will be trimmed.
   * @return {@link Path} the new Path
   */
  Path trim(int length);

  /**
   * Mutates each of the positions in the path with the given consumer
   *
   * @param mutator the {@link ParameterizedSupplier} to mutate the positions with
   * @return {@link Path} the new Path
   */
  Path mutatePositions(ParameterizedSupplier<PathPosition> mutator);

  /**
   * Returns the start position of the path
   *
   * @return {@link PathPosition} The position of the start
   */
  PathPosition getStart();

  /**
   * Returns the target position of the path
   *
   * @return {@link PathPosition} The position of the target
   */
  PathPosition getEnd();

  /**
   * Returns a new Collection of the Path Positions of the path.
   *
   * @return {@link Collection} of the PathPositions
   */
  Collection<PathPosition> collect();
}
