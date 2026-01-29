package de.bsommerfeld.pathetic.api.pathing.result;

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
