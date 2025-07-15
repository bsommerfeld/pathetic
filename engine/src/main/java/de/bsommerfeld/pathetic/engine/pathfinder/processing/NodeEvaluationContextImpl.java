package de.bsommerfeld.pathetic.engine.pathfinder.processing;

import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;

import java.util.Objects;

public class NodeEvaluationContextImpl implements NodeEvaluationContext {

    private final SearchContext searchContext;
    private final Node engineNode;
    private final Node parentEngineNode;
    private final IHeuristicStrategy heuristicStrategy;

    /**
     * Creates a new NodeEvaluationContextImpl with the specified parameters.
     *
     * @param searchContext    The search context
     * @param engineNode       The current node being evaluated
     * @param parentEngineNode The parent node (can be null for the start node)
     */
    public NodeEvaluationContextImpl(
            SearchContext searchContext,
            Node engineNode,
            Node parentEngineNode,
            IHeuristicStrategy heuristicStrategy) {
        this.searchContext = Objects.requireNonNull(searchContext, "searchContext must not be null");
        this.engineNode = Objects.requireNonNull(engineNode, "engineNode must not be null");
        this.parentEngineNode = parentEngineNode; // parentEngineNode can be null for the start node
        this.heuristicStrategy = heuristicStrategy;
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

        // TODO: negative costs check

        return this.heuristicStrategy.calculateTransitionCost(from, to);
    }

    @Override
    public SearchContext getSearchContext() {
        return this.searchContext;
    }
}
