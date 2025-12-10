package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

/**
 * A functional interface to provide an offset as a {@link Iterable} consisting of {@link
 * PathVector}s.
 */
@FunctionalInterface
public interface INeighborStrategy {

  /**
   * Gets the collection of vectors to check for neighbor nodes. Used for static strategies where
   * neighbors are always the same (e.g. Grid).
   */
  Iterable<PathVector> getOffsets();

  /**
   * Gets the collection of vectors based on the current position. Default implementation delegates
   * to the static getOffsets() for backward compatibility.
   *
   * @param currentPosition The position for which to get neighbors.
   * @return An iterable of PathVectors.
   * @since 6.0.0
   */
  default Iterable<PathVector> getOffsets(PathPosition currentPosition) {
    return getOffsets();
  }
}
