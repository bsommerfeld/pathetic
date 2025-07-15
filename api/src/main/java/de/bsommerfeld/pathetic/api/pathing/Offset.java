package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.wrapper.PathVector;

import java.util.stream.Stream;

@Deprecated
public enum Offset {
    VERTICAL_AND_HORIZONTAL(
            new PathVector[]{
                    new PathVector(1, 0, 0),
                    new PathVector(-1, 0, 0),
                    new PathVector(0, 0, 1),
                    new PathVector(0, 0, -1),
                    new PathVector(0, 1, 0),
                    new PathVector(0, -1, 0)
            }),

    DIAGONAL(
            new PathVector[]{
                    new PathVector(-1, 0, -1),
                    new PathVector(-1, 0, 0),
                    new PathVector(-1, 0, 1),
                    new PathVector(0, 0, -1),
                    new PathVector(0, 0, 0),
                    new PathVector(0, 0, 1),
                    new PathVector(1, 0, -1),
                    new PathVector(1, 0, 0),
                    new PathVector(1, 0, 1),
                    new PathVector(-1, 1, -1),
                    new PathVector(-1, 1, 0),
                    new PathVector(-1, 1, 1),
                    new PathVector(0, 1, -1),
                    new PathVector(0, 1, 0),
                    new PathVector(0, 1, 1),
                    new PathVector(1, 1, -1),
                    new PathVector(1, 1, 0),
                    new PathVector(1, 1, 1),
                    new PathVector(-1, -1, -1),
                    new PathVector(-1, -1, 0),
                    new PathVector(-1, -1, 1),
                    new PathVector(0, -1, -1),
                    new PathVector(0, -1, 0),
                    new PathVector(0, -1, 1),
                    new PathVector(1, -1, -1),
                    new PathVector(1, -1, 0),
                    new PathVector(1, -1, 1)
            }),

    MERGED(
            Stream.concat(Stream.of(DIAGONAL.vectors), Stream.of(VERTICAL_AND_HORIZONTAL.vectors))
                    .toArray(PathVector[]::new));

    private final PathVector[] vectors;

    Offset(PathVector[] vectors) {
        this.vectors = vectors;
    }

    public PathVector[] getVectors() {
        return vectors;
    }
}
