package de.bsommerfeld.pathetic.example.processor;

import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;

public class SimpleValidationProcessor implements NodeValidationProcessor {

  @Override
  public boolean isValid(NodeEvaluationContext context) {
    return context.getPathfinderConfiguration().getProvider().getNavigationPoint(context.getCurrentPathPosition()).isTraversable();
  }
}
