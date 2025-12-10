package de.bsommerfeld.pathetic.api.pathing.processing;

import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;

/**
 * A processor that calculates a specific cost contribution for traversing to a node (PathPosition).
 * The costs returned by all registered {@code NodeCostProcessor}s for a given node transition are
 * typically summed up by the pathfinding pipeline along with the base transition cost.
 *
 * @see Processor
 */
public interface CostProcessor extends Processor {
  /**
   * Calculates the cost contribution of this processor for the transition to the current node
   * (PathPosition).
   *
   * @param context The evaluation context for the current node.
   * @return A {@link Cost} object representing the cost contribution. This value will be added to
   *     other costs for this transition.
   */
  Cost calculateCostContribution(EvaluationContext context);
}
