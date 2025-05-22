package de.bsommerfeld.pathetic.api.pathing.processing;

import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;

/**
 * A processor that calculates a specific cost contribution for traversing to a node (PathPosition).
 * The costs returned by all registered {@code NodeCostCalculator}s for a given node transition are
 * typically summed up by the pathfinding pipeline along with the base transition cost.
 */
public interface NodeCostCalculator extends Processor {
  /**
   * Calculates the cost contribution of this processor for the transition to the current node
   * (PathPosition).
   *
   * @param context The evaluation context for the current node.
   * @return A {@link Cost} object representing the cost contribution. This value will be added to
   *     other costs for this transition.
   */
  Cost calculateCostContribution(NodeEvaluationContext context);
}
