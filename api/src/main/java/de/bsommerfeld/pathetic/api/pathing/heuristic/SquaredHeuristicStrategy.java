package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

public class SquaredHeuristicStrategy implements IHeuristicStrategy {

    @Override
    public double calculate(HeuristicContext heuristicContext) {
        HeuristicWeights heuristicWeights = heuristicContext.heuristicWeights();
        PathPosition position = heuristicContext.position();
        PathPosition start = heuristicContext.startPosition();
        PathPosition target = heuristicContext.targetPosition();

        double manhattan = position.manhattanDistance(target);
        double manhattanDistanceSquared = manhattan * manhattan;

        double octile = position.octileDistance(target);
        double octileDistanceSquared = octile * octile;

        double perpendicularDistanceSquared = calculatePerpendicularDistance(position, start, target);

        double heightDiff = position.getFlooredY() - target.getFlooredY();
        double heightDifferenceSquared = heightDiff * heightDiff;

        double manhattanWeight = heuristicWeights.getManhattanWeight();
        double octileWeight = heuristicWeights.getOctileWeight();
        double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
        double heightWeight = heuristicWeights.getHeightWeight();

        return (manhattanDistanceSquared * manhattanWeight)
                + (octileDistanceSquared * octileWeight)
                + (perpendicularDistanceSquared * perpendicularWeight)
                + (heightDifferenceSquared * heightWeight);
    }

    @Override
    public double calculateTransitionCost(PathPosition from, PathPosition to) {
        return to.distanceSquared(from);
    }

    /**
     * Calculates the squared perpendicular distance from the current node's position to the straight line segment
     * defined by the start and target nodes.
     *
     * <p>This metric is used as a component of the main heuristic to penalize nodes that stray far
     * from the direct path. The calculation uses vector mathematics. Squaring the distance avoids costly {@code sqrt}
     * operations and maintains consistency with the other squared metrics in the heuristic.
     *
     * @return The squared perpendicular distance of the current node from the start-target line. If the start and
     * target are nearly identical, it returns the squared distance to the start node.
     */
    private double calculatePerpendicularDistance(PathPosition position, PathPosition start, PathPosition target) {
        PathVector currentVec = position.toVector();
        PathVector startVec = start.toVector();
        PathVector targetVec = target.toVector();

        PathVector lineVec = targetVec.subtract(startVec);

        // The squared length of the line vector is calculated using the dot product of the vector with
        // itself, which is equivalent to v.length² and more efficient.
        double lineVecLengthSq = lineVec.dot(lineVec);
        if (lineVecLengthSq < 1e-9) {
            // Avoid division by zero if start and target are almost identical.
            // The "line" is a point, so the perpendicular distance is simply the distance to that point.
            return position.distanceSquared(start);
        }

        PathVector startToCurrentVec = currentVec.subtract(startVec);

        // The squared length of the cross product of two vectors is equal to the squared area of the
        // parallelogram they form. By dividing this by the squared length of the base vector (lineVec),
        // we get the squared perpendicular height (the distance we're looking for).
        PathVector crossProduct = startToCurrentVec.getCrossProduct(lineVec);
        double crossProductLengthSq = crossProduct.dot(crossProduct);

        return crossProductLengthSq / lineVecLengthSq;
    }
}
