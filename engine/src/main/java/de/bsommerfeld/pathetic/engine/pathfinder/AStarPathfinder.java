package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.EvaluationContextImpl;
import de.bsommerfeld.pathetic.engine.util.RegionKey;

/**
 * An A* pathfinding algorithm that uses a heuristic to guide the search toward the target. It
 * balances the actual cost from the start (G-cost) with the estimated cost to the target (H-cost).
 *
 * <p>This implementation uses:
 *
 * <ul>
 *   <li>A quaternary primitive min-heap for the open set (priority queue), keyed by dense node
 *       ids so decrease-key position tracking is a plain array access.
 *   <li>A single per-search hash map from packed position keys to dense ids; open-set nodes, the
 *       closed set, and recorded G-costs are all id-indexed arrays ({@link AStarSearchState}).
 * </ul>
 *
 * <p>Thread-safety: each pathfinding operation gets its own {@link AStarSearchState}, created as a
 * stack local in the base loop and passed explicitly into the template methods. Concurrent requests
 * on a single pathfinder are therefore isolated by the call stack; no per-search state is stored on
 * the pathfinder itself.
 *
 * <p>Exploration radius: node keys are packed relative to the search start (see {@link
 * RegionKey}), so absolute world coordinates are unconstrained. A single search can expand
 * positions up to roughly two million blocks (X/Z) and half a million blocks (Y) away from its
 * start; positions beyond that radius are treated as non-navigable.
 */
public final class AStarPathfinder extends AbstractPathfinder<AStarSearchState> {

  public AStarPathfinder(PathfinderConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected AStarSearchState createSearchState(PathPosition start, int expectedNodes) {
    return new AStarSearchState(pathfinderConfiguration, start, expectedNodes);
  }

  /**
   * Processes the successors of the current node, checking if they're in the open or closed set,
   * calculating costs, validating traversability, and updating the open set as needed.
   *
   * @param start The starting position of the pathfinding request.
   * @param target The target position of the pathfinding request.
   * @param currentNode The node being expanded.
   * @param state The per-search state holding the open and closed sets.
   * @param searchContext The context for the current search.
   */
  @Override
  protected void processSuccessors(
      PathPosition start,
      PathPosition target,
      Node currentNode,
      AStarSearchState state,
      SearchContext searchContext) {

    Iterable<PathVector> offsets = neighborStrategy.getOffsets(currentNode.getPosition());

    for (PathVector offset : offsets) {
      PathPosition neighborPos = currentNode.getPosition().add(offset);

      /*
       * Positions outside this search's exploration radius (see RegionKey) are simply not
       * navigable. Skipping them here lets searches at the edge of the radius degrade gracefully
       * instead of aborting the whole search when pack() rejects the relative coordinates.
       */
      if (!state.isInRange(neighborPos)) continue;

      long packedPos = state.pack(neighborPos);

      /*
       * The single hash lookup per neighbor: cells that never entered the open set have no id
       * and are therefore neither open nor closed. All further per-node state checks below are
       * id-indexed array accesses.
       */
      int id = state.idOf(packedPos);

      // Check if neighbor is in the open set
      if (id != AStarSearchState.NO_ID) {
        Node existing = state.openNode(id);
        if (existing != null) {
          updateExistingNode(existing, id, currentNode, searchContext, state);
          continue;
        }
      }

      /*
       * Node construction (and the heuristic computation inside it) is deferred until a branch
       * actually needs the node. Closed neighbors that are not reopened - the most common skip
       * with reopening disabled - therefore allocate nothing.
       */
      Node neighbor = null;

      /*
       * Reused across the reopen check and the "process as new node" branch below. The reopen
       * path falls through into new-node processing with identical context arguments, so a single
       * instance serves both and avoids a second allocation. Stays null when the neighbor is in
       * the closed set and reopening is disabled, so the common skip path allocates nothing.
       */
      EvaluationContext context = null;

      /*
       * G-cost computed during the reopen check, reused verbatim when the node is processed as a
       * new node below. CostProcessors are a public extension point and may be stateful, so
       * recomputing could yield a different value than the one that justified reopening - the
       * node would then enter the open set with a key inconsistent with the reopen decision.
       */
      double reopenGCost = 0.0;
      boolean reopening = false;

      // Check if neighbor is in the closed set
      if (id != AStarSearchState.NO_ID && state.isClosed(id)) {

        /*
         * This block handles the edge case where we find a path to a node,
         * that has already been fully processed (is in the closed set).
         *
         * Normally (with consistent heuristics), the first time,
         * we close a node, we have found the shortest path to it.
         *
         * However, if the heuristic is inconsistent (or weights change dynamically),
         * we might find a "shorter" path later.
         *
         * Since this functionality can have a performance impact (in comparison to the rest of the Pathfinder),
         * we hide this behind a configuration flag, so the user can decide whether this should be active.
         */

        if (pathfinderConfiguration.shouldReopenClosedNodes()) {
          double oldCost = state.closedGCost(id);

          neighbor = createNeighborNode(neighborPos, start, target, currentNode);

          if (hasCustomProcessors) {
            context =
                new EvaluationContextImpl(
                    searchContext,
                    neighbor,
                    currentNode,
                    pathfinderConfiguration.getHeuristicStrategy());
            reopenGCost = calculateGCost(context);
          } else {
            reopenGCost = calculateGCostFast(currentNode, neighborPos);
          }

          // Is this path significantly better?
          if (Double.isNaN(oldCost) || reopenGCost + Math.ulp(reopenGCost) < oldCost) {
            // Mark this node for reopening
            reopening = true;
          }
        }

        if (!reopening) continue;

        // Once we got here, the node will be processed as "new" node
        // and with that effectively reopened.
      }

      // Process as a new node
      if (neighbor == null) {
        neighbor = createNeighborNode(neighborPos, start, target, currentNode);
      }
      neighbor.setParent(currentNode);

      /*
       * --------------------------------------------------------------------------------
       * Calculates the G-cost for the new neighbor and adds it to the open set.
       *
       * This block handles three main tasks:
       *  1. Figures out the transition cost (G) from the current node to the neighbor.
       *  2. Computes the total estimated cost (F = G + H) for prioritization.
       *  3. Inserts the node into the open set with a small tie-breaker
       *     to make pathfinding smoother when multiple nodes have the same F-cost.
       *
       * What's the tie-breaker about?
       * -----------------------------
       * If multiple nodes have the same F-cost, A* might pick them randomly, which can
       * lead to jagged or inconsistent paths. To smooth things out, we give a tiny
       * advantage to nodes closer to the goal (smaller H). We do this by subtracting
       * a small value (TIE_BREAKER_WEIGHT * (H / (|F| + 1))) from F. This ensures
       * nodes closer to the goal get expanded first, without messing up correctness
       * or optimality—the bias is super small!
       *
       * Note on numerics:
       * -----------------
       * Comparisons use Math.ulp so the tolerance adapts to the magnitude of the values.
       * Non-finite F-costs (a NaN/Infinity heuristic or cost) are rejected inside
       * calculateHeapKey before they can reach the heap; the search loop reports that
       * as a FAILED result.
       * --------------------------------------------------------------------------------
       */
      double gCost;
      if (hasCustomProcessors) {
        if (context == null) {
          context =
              new EvaluationContextImpl(
                  searchContext, neighbor, currentNode, pathfinderConfiguration.getHeuristicStrategy());
        }
        if (!isValidByCustomProcessors(context)) {
          continue;
        }
        gCost = reopening ? reopenGCost : calculateGCost(context);
      } else {
        /*
         * No validation or cost processors: the neighbor is always valid and the G-cost reduces to
         * the parent's accumulated cost plus the base transition cost, so we skip allocating an
         * EvaluationContext for it.
         */
        gCost = reopening ? reopenGCost : calculateGCostFast(currentNode, neighborPos);
      }

      /*
       * Recorded only after validation so that a vetoed reopen attempt does not lower the stored
       * G-cost; otherwise a later, valid path with a cost between the vetoed and the stored value
       * could no longer reopen this node.
       */
      if (reopening) {
        state.recordClosedGCost(id, gCost);
      }

      neighbor.setGCost(gCost);
      double fCost = neighbor.getFCost();
      double heapKey = calculateHeapKey(neighbor, fCost);

      if (id == AStarSearchState.NO_ID) {
        id = state.assignId(packedPos);
      }
      state.openInsert(id, heapKey);
      state.setOpenNode(id, neighbor);
    }
  }

  private void updateExistingNode(
      Node existing,
      int nodeId,
      Node currentNode,
      SearchContext searchContext,
      AStarSearchState state) {

    EvaluationContext context =
        hasCustomProcessors
            ? new EvaluationContextImpl(
                searchContext, existing, currentNode, pathfinderConfiguration.getHeuristicStrategy())
            : null;

    double newG =
        hasCustomProcessors
            ? calculateGCost(context)
            : calculateGCostFast(currentNode, existing.getPosition());
    double tol = Math.ulp(Math.max(Math.abs(newG), Math.abs(existing.getGCost())));
    if (newG + tol >= existing.getGCost()) return;

    if (hasCustomProcessors && !isValidByCustomProcessors(context)) {
      return;
    }

    existing.setParent(currentNode);
    existing.setGCost(newG);

    double newF = existing.getFCost();
    double newKey = calculateHeapKey(existing, newF);

    double oldKey = state.openKey(nodeId);

    // We only call the heap once the key actually decreased
    if (newKey + Math.ulp(newKey) < oldKey) {
      // O(log n)
      state.openInsert(nodeId, newKey);
    }
    // edge-case handling
    else if (Math.abs(newKey - oldKey) <= Math.ulp(newKey)) {
      /*
       * Sometimes a tiny nudging helps to maintain consistency,
       * but usually insertOrUpdate catches that.
       *
       * Since our heap strictly checks <, we can force it here
       */
      state.openInsert(nodeId, oldKey - Math.ulp(oldKey));
    }
  }

  private Node createNeighborNode(
      PathPosition position, PathPosition start, PathPosition target, Node parent) {
    return new Node(
        position,
        start,
        target,
        pathfinderConfiguration.getHeuristicWeights(),
        pathfinderConfiguration.getHeuristicStrategy(),
        parent.getDepth() + 1);
  }

  private boolean isValidByCustomProcessors(EvaluationContext context) {
    for (ValidationProcessor validator : validationProcessors) {
      if (!validator.isValid(context)) {
        return false;
      }
    }
    return true;
  }

  /**
   * G-cost for a transition when no validation or cost processors are configured. Equivalent to
   * {@link #calculateGCost(EvaluationContext)} with empty processor lists - it is the parent's
   * accumulated G-cost plus the base transition cost - but computed without allocating an {@link
   * EvaluationContext}. Mirrors the base-cost handling of {@code EvaluationContextImpl}: non-finite
   * transition costs are rejected and negative costs are clamped to zero.
   */
  private double calculateGCostFast(Node parent, PathPosition to) {
    double baseCost =
        pathfinderConfiguration.getHeuristicStrategy().calculateTransitionCost(parent.getPosition(), to);
    if (Double.isNaN(baseCost) || Double.isInfinite(baseCost)) {
      throw new IllegalStateException(
          "Heuristic transition cost produced an invalid numeric value: " + baseCost);
    }
    if (baseCost < 0) {
      baseCost = 0;
    }
    return parent.getGCost() + baseCost;
  }

  private double calculateGCost(EvaluationContext context) {
    double baseCost = context.getBaseTransitionCost();
    double additionalCost = 0.0;

    for (CostProcessor processor : costProcessors) {
      Cost contribution = processor.calculateCostContribution(context);
      if (contribution != null) {
        additionalCost += contribution.value();
      }
    }

    double transitionCost = baseCost + additionalCost;
    if (transitionCost < 0) {
      transitionCost = 0;
    }
    return context.getPathCostToPreviousPosition() + transitionCost;
  }
}
