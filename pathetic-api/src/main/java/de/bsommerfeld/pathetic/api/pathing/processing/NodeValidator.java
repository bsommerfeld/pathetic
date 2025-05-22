package de.bsommerfeld.pathetic.api.pathing.processing;

import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;

/**
 * A processor that validates whether a node (PathPosition) or the transition to it
 * is permissible during pathfinding.
 * If any validator returns {@code false} for a node, that node (or path segment)
 * will typically be excluded from the search.
 */
public interface NodeValidator extends Processor {
  /**
   * Checks if the current node (PathPosition) (or the transition to it from the previous node)
   * is valid according to the logic of this validator.
   *
   * @param context The evaluation context for the current node.
   * @return {@code true} if the node/transition is considered valid, {@code false} otherwise.
   */
  boolean isValid(NodeEvaluationContext context);
}
