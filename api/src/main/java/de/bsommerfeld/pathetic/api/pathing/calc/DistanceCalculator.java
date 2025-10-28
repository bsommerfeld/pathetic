package de.bsommerfeld.pathetic.api.pathing.calc;

import de.bsommerfeld.pathetic.api.pathing.PathfindingProgress;

/**
 * Computes a distance metric for the current state of a pathfinding operation.
 *
 * <p>This functional interface is used by heuristic strategies to obtain a measurable value (the
 * metric) from the three positions encapsulated in a {@link PathfindingProgress} instance. The
 * generic type {@code <M>} represents the concrete metric type – typically {@code double} for
 * numeric distances, but it may be any type that suits the heuristic (e.g. {@code Double}, {@code
 * Vector}, or a custom metric object).
 *
 * <p>Implementations are expected to be stateless and side-effect-free, making them suitable for
 * reuse across multiple heuristic evaluations.
 *
 * @param <M> the type of the distance metric returned by this calculator
 * @since 5.3.3
 */
@FunctionalInterface
public interface DistanceCalculator<M> {

  /**
   * Calculates and returns the distance metric for the given pathfinding progress.
   *
   * @param progress the current pathfinding state containing start, current, and target positions;
   *     must not be {@code null}
   * @return the computed metric value; may be {@code null} only if explicitly documented by the
   *     implementation
   */
  M calculate(PathfindingProgress progress);
}
