package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.wrapper.PathVector;

@FunctionalInterface
public interface INeighborStrategy {

    /**
     * Gets the collection of vectors to check for neighbor nodes.
     *
     * @return An iterable of PathVectors.
     */
    Iterable<PathVector> getOffsets();
}
