package de.bsommerfeld.pathetic.api.pathing.heuristic;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;

public class LinearHeuristicStrategy implements IHeuristicStrategy {

    @Override
    public double calculate(HeuristicContext heuristicContext) {
        HeuristicWeights heuristicWeights = heuristicContext.heuristicWeights();
        PathPosition position = heuristicContext.position();
        PathPosition start = heuristicContext.startPosition();
        PathPosition target = heuristicContext.targetPosition();

        final double manhattanWeight = heuristicWeights.getManhattanWeight();
        final double octileWeight = heuristicWeights.getOctileWeight();
        final double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
        final double heightWeight = heuristicWeights.getHeightWeight();

        final double manhattanDistance = position.manhattanDistance(target);
        final double octileDistance = position.octileDistance(target);
        final double perpendicularDistance = calculatePerpendicularDistance(position, start, target);
        final double heightDifference = Math.abs(position.getFlooredY() - target.getFlooredY());

        return (manhattanDistance * manhattanWeight)
                + (octileDistance * octileWeight)
                + (perpendicularDistance * perpendicularWeight)
                + (heightDifference * heightWeight);
    }

    @Override
    public double calculateTransitionCost(PathPosition from, PathPosition to) {
        return to.distance(from);
    }

    /**
     * Calculates the perpendicular distance from the current node's position to the straight line segment defined by
     * the start and target nodes.
     *
     * <p>This metric is used as a component of the main heuristic to penalize nodes that stray far
     * from the direct path. The calculation uses vector mathematics and returns a true linear distance via a final
     * square root operation. This is the main difference from the performance variant, which works with squared
     * distances to avoid costly {@code sqrt} operations.
     *
     * @return The perpendicular distance of the current node from the start-target line. If the start and target are
     * nearly identical, it returns the distance to the start node.
     */
    private double calculatePerpendicularDistance(PathPosition position, PathPosition start, PathPosition target) {
        // Create vectors for the involved positions
        final PathVector currentVec = position.toVector();
        final PathVector startVec = start.toVector();
        final PathVector targetVec = target.toVector();

        // Vector representing the line from start to target
        final PathVector lineVec = targetVec.subtract(startVec);

        // The squared length of the line vector is calculated using the dot product of the vector with
        // itself, which is equivalent to v.length² and more efficient.
        final double lineVecLengthSq = lineVec.dot(lineVec);

        // Edge case: If start and target are almost identical, the "line" is a point.
        // In this case, the perpendicular distance is simply the distance to that point.
        if (lineVecLengthSq < 1e-9) {
            return position.distance(start);
        }

        // Vector from start point to current position
        final PathVector startToCurrentVec = currentVec.subtract(startVec);

        // The cross product gives a vector perpendicular to the plane spanned by startToCurrentVec
        // and lineVec. Its length is proportional to the area of the parallelogram formed by the
        // vectors.
        final PathVector crossProduct = startToCurrentVec.getCrossProduct(lineVec);
        final double crossProductLengthSq = crossProduct.dot(crossProduct);

        // The formula for perpendicular distance d is: d = |startToCurrentVec x lineVec| / |lineVec|
        // Since we have squared lengths, the formula becomes: d² = crossProductLengthSq /
        // lineVecLengthSq
        // To get d, we take the square root of the entire expression.
        return Math.sqrt(crossProductLengthSq / lineVecLengthSq);
    }
}
