package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.engine.Node;

/**
 * Algorithm-independent per-search state handed to {@link AbstractPathfinder}'s main loop. It bundles
 * the open set (priority queue) and the closed set behind a small protocol so the loop never names a
 * concrete algorithm's data structures and never needs a side channel (such as a {@code ThreadLocal})
 * to reach them: the state is created once per search via {@link
 * AbstractPathfinder#createSearchState(de.bsommerfeld.pathetic.api.wrapper.PathPosition, int)}, lives
 * as a local on the calling stack, and is passed explicitly to every template method. Concurrent
 * searches on a single pathfinder are therefore isolated by the call stack alone.
 *
 * <p>Implementations are not thread-safe; one instance belongs to exactly one running search.
 * Algorithm-specific operations (id assignment, closed-set G-cost bookkeeping, decrease-key) live on
 * the concrete subtype and are reached by the pathfinder that owns the matching state type.
 */
interface SearchState {

  /** Returns whether the open set still holds nodes to expand. */
  boolean hasOpenNodes();

  /**
   * Inserts a node into the open set with the given heap key, assigning it identity if it is seen for
   * the first time. Used by the loop for the start node.
   */
  void insert(Node node, double heapKey);

  /** Removes and returns the open-set node with the lowest heap key. */
  Node extractBest();

  /** Moves the given node into the closed set. */
  void markExpanded(Node node);
}
