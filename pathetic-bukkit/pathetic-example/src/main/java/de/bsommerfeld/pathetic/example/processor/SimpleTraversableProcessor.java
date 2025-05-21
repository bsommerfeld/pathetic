package de.bsommerfeld.pathetic.example.processor;

import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidator;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;

public class SimpleTraversableProcessor implements NodeValidator {

  @Override
  public boolean isValid(NodeEvaluationContext context) {
    return context.getPathfinderConfiguration().getProvider().getNavigationPoint(context.getCurrentPathPosition()).isTraversable();
  }
}
