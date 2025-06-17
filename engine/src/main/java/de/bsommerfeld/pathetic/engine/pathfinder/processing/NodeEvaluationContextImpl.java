package de.bsommerfeld.pathetic.engine.pathfinder.processing;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicMode;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import java.util.Objects;

public class NodeEvaluationContextImpl implements NodeEvaluationContext {

  private final SearchContext searchContext;
  private final Node engineNode;
  private final Node parentEngineNode;
  private final HeuristicMode heuristicMode;

  /**
   * Creates a new NodeEvaluationContextImpl with the specified parameters.
   *
   * @param searchContext The search context
   * @param engineNode The current node being evaluated
   * @param parentEngineNode The parent node (can be null for the start node)
   * @param heuristicMode The heuristic mode to use for calculations
   */
  public NodeEvaluationContextImpl(
      SearchContext searchContext,
      Node engineNode,
      Node parentEngineNode,
      HeuristicMode heuristicMode) {
    this.searchContext = Objects.requireNonNull(searchContext, "searchContext must not be null");
    this.engineNode = Objects.requireNonNull(engineNode, "engineNode must not be null");
    this.parentEngineNode = parentEngineNode; // parentEngineNode can be null for the start node
    this.heuristicMode = heuristicMode;
  }

  /**
   * Creates a new NodeEvaluationContextImpl with the specified parameters, using PERFORMANCE as the
   * default heuristic mode.
   *
   * @param searchContext The search context
   * @param engineNode The current node being evaluated
   * @param parentEngineNode The parent node (can be null for the start node)
   */
  public NodeEvaluationContextImpl(
      SearchContext searchContext, Node engineNode, Node parentEngineNode) {
    this(searchContext, engineNode, parentEngineNode, HeuristicMode.PERFORMANCE);
  }

  @Override
  public PathPosition getCurrentPathPosition() {
    return this.engineNode.getPosition();
  }

  @Override
  public PathPosition getPreviousPathPosition() {
    return this.parentEngineNode != null ? this.parentEngineNode.getPosition() : null;
  }

  @Override
  public int getCurrentNodeDepth() {
    return this.engineNode.getDepth();
  }

  @Override
  public double getCurrentNodeHeuristicValue() {
    return this.engineNode.getHeuristic().get();
  }

  @Override
  public double getPathCostToPreviousPosition() {
    if (this.parentEngineNode == null) {
      return 0.0;
    }
    return this.parentEngineNode.getGCost();
  }

  @Override
  public double getBaseTransitionCost() {
    if (this.parentEngineNode == null) {
      return 0.0;
    }

    PathPosition from = this.parentEngineNode.getPosition();
    PathPosition to = this.engineNode.getPosition();

    if (from.getFlooredX() != to.getFlooredX() && from.getFlooredZ() != to.getFlooredZ()) {
      return heuristicMode == HeuristicMode.PRECISION ? Math.sqrt(2) : 2.0; // Diagonal
    } else {
      return 1.0; // Horizontal/Vertical
    }
  }

  @Override
  public SearchContext getSearchContext() {
    return this.searchContext;
  }
}
