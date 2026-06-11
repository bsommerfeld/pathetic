/**
 * Heuristic estimation for the A* search.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy} computes the H-cost
 * of a node from a {@link de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicContext} and the
 * base transition cost between two positions. {@link
 * de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies} exposes the bundled
 * implementations: {@code LINEAR} (sqrt-based, near-optimal in practice) and {@code SQUARED}
 * (faster, but its overestimation grows with distance, so paths can be suboptimal at range).
 * {@link de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights} weights the composite's
 * components (Manhattan, octile, perpendicular deviation, height difference).
 */
package de.bsommerfeld.pathetic.api.pathing.heuristic;
