package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.PathfindingSearch;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.Processor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.PrimitiveMinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.EvaluationContextImpl;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.SearchContextImpl;
import de.bsommerfeld.pathetic.engine.result.PathImpl;
import de.bsommerfeld.pathetic.engine.result.PathfinderResultImpl;
import de.bsommerfeld.pathetic.engine.util.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides a skeletal implementation of the {@link Pathfinder} interface, defining common behavior
 * for pathfinding algorithms.
 *
 * <p>This pathfinder operates by iteratively processing nodes from an open set (priority queue)
 * until the target is reached or other termination conditions are met. It supports asynchronous
 * execution and customizable hooks for observing the pathfinding steps. The "tick-wise" nature
 * mentioned previously refers to each main loop iteration processing one node.
 */
public abstract class AbstractPathfinder implements Pathfinder {

  protected static final Set<PathPosition> EMPTY_PATH_POSITIONS =
      Collections.unmodifiableSet(new LinkedHashSet<>(0));
  private static final int MIN_INITIAL_HEAP_CAPACITY = 32;
  private static final double TIE_BREAKER_WEIGHT = 1e-6;

  protected final PathfinderConfiguration pathfinderConfiguration;
  protected final NavigationPointProvider navigationPointProvider;
  protected final List<ValidationProcessor> validationProcessors;
  protected final List<CostProcessor> costProcessors;
  protected final INeighborStrategy neighborStrategy;
  protected final ExecutorService executorService;

  private final Set<PathfinderHook> pathfinderHooks = Collections.synchronizedSet(new HashSet<>());

  protected AbstractPathfinder(PathfinderConfiguration pathfinderConfiguration) {
    this.pathfinderConfiguration =
        Objects.requireNonNull(pathfinderConfiguration, "pathfinderConfiguration must not be null");

    this.navigationPointProvider =
        Objects.requireNonNull(
            pathfinderConfiguration.getProvider(),
            "NavigationPointProvider from configuration has to be set.");

    this.validationProcessors = pathfinderConfiguration.getNodeValidationProcessors();
    this.costProcessors = pathfinderConfiguration.getNodeCostProcessors();
    this.neighborStrategy = pathfinderConfiguration.getNeighborStrategy();
    this.pathfinderHooks.addAll(pathfinderConfiguration.pathfindingHooks());
    /*
     * Only async configurations need an executor. Sync configurations are allowed to leave it
     * null so that the shared default thread pool is never instantiated when nobody will use it.
     */
    if (pathfinderConfiguration.isAsync()) {
      this.executorService =
          Objects.requireNonNull(
              pathfinderConfiguration.executorService(),
              "Executor service from configuration has not been set");
    } else {
      this.executorService = pathfinderConfiguration.executorService();
    }
  }

  /**
   * Pure formula behind {@link #estimateInitialHeapCapacity(PathPosition, PathPosition)}, exposed
   * package-private for unit testing. Returns the same value as the instance method when given the
   * branching factor and {@code maxIterations} the running search would see.
   */
  static int computeInitialHeapCapacity(
      PathPosition start, PathPosition target, int branching, int maxIterations) {
    int dx = Math.abs(start.getFlooredX() - target.getFlooredX());
    int dy = Math.abs(start.getFlooredY() - target.getFlooredY());
    int dz = Math.abs(start.getFlooredZ() - target.getFlooredZ());
    long manhattan = (long) dx + (long) dy + (long) dz;
    long estimated = manhattan * Math.max(1, branching);
    long bounded = Math.max(MIN_INITIAL_HEAP_CAPACITY, Math.min(estimated, maxIterations));
    return (int) bounded;
  }

  @Override
  public PathfindingSearch findPath(
      PathPosition start, PathPosition target, EnvironmentContext environmentContext) {
    Objects.requireNonNull(start, "start PathPosition must not be null");
    Objects.requireNonNull(target, "target PathPosition must not be null");
    return initiatePathing(start, target, environmentContext);
  }

  @Override
  public void registerPathfindingHook(PathfinderHook hook) {
    if (hook != null) {
      this.pathfinderHooks.add(hook);
    }
  }

  private PathfindingSearch initiatePathing(
      PathPosition start, PathPosition target, EnvironmentContext environmentContext) {

    final PathPosition effectiveStart = start.floor();
    final PathPosition effectiveTarget = target.floor();

    final AtomicBoolean abortFlag = new AtomicBoolean(false);

    CompletableFuture<PathfinderResult> future;
    if (pathfinderConfiguration.isAsync()) {
      future =
          CompletableFuture.supplyAsync(
              () ->
                  executePathingAlgorithm(
                      effectiveStart, effectiveTarget, environmentContext, abortFlag),
              executorService);
    } else {
      future =
          CompletableFuture.completedFuture(
              executePathingAlgorithm(
                  effectiveStart, effectiveTarget, environmentContext, abortFlag));
    }

    return new PathfindingSearchImpl(future, () -> abortFlag.set(true));
  }

  /**
   * Core pathfinding execution logic.
   *
   * @param start The effective (e.g., floored) start position.
   * @param target The effective (e.g., floored) target position.
   * @return The result of the pathfinding operation.
   */
  private PathfinderResult executePathingAlgorithm(
      PathPosition start,
      PathPosition target,
      EnvironmentContext environmentContext,
      AtomicBoolean abortFlag) {
    initializeSearch();

    SearchContext searchContext =
        new SearchContextImpl(
            start,
            target,
            this.pathfinderConfiguration,
            this.navigationPointProvider,
            environmentContext);

    List<Processor> processors = getProcessors();

    try {
      for (Processor processor : processors) {
        processor.initializeSearch(searchContext);
      }

      Node startNode = createStartNode(start, target);

      final EvaluationContext startNodeContext =
          new EvaluationContextImpl(
              searchContext, startNode, null, pathfinderConfiguration.getHeuristicStrategy());

      if (this.validationProcessors != null && !this.validationProcessors.isEmpty()) {
        final boolean isStartNodeInvalid =
            this.validationProcessors.stream()
                .anyMatch(validator -> !validator.isValid(startNodeContext));

        if (isStartNodeInvalid) {
          return new PathfinderResultImpl(
              PathState.FAILED, new PathImpl(start, target, EMPTY_PATH_POSITIONS));
        }
      }

      MinHeap openSet = new PrimitiveMinHeap(estimateInitialHeapCapacity(start, target));

      double startKey = calculateHeapKey(startNode, startNode.getFCost());
      insertStartNode(startNode, startKey, openSet);

      /*
       * Snapshot the registered hooks once per search so the hot loop never contends on the
       * synchronized set monitor and concurrent searches don't fight over the same lock. Hooks
       * registered after the snapshot is taken (via the deprecated registerPathfindingHook)
       * apply only to subsequent searches.
       */
      final List<PathfinderHook> hookSnapshot;
      synchronized (pathfinderHooks) {
        hookSnapshot =
            pathfinderHooks.isEmpty() ? Collections.emptyList() : new ArrayList<>(pathfinderHooks);
      }

      int iteration = 0;
      Node bestFallbackNode = startNode;

      while (!openSet.isEmpty() && iteration < pathfinderConfiguration.getMaxIterations()) {

        if (abortFlag.get()) {
          return new PathfinderResultImpl(
              PathState.ABORTED, new PathImpl(start, target, EMPTY_PATH_POSITIONS));
        }

        iteration++;

        Node currentNode = extractBestNode(openSet);
        markNodeAsExpanded(currentNode);

        if (!hookSnapshot.isEmpty()) {
          PathfindingContext hookContext =
              new PathfindingContext(currentNode.getPosition(), Depth.of(iteration));
          for (PathfinderHook hook : hookSnapshot) {
            hook.onPathfindingStep(hookContext);
          }
        }

        if (currentNode.getHeuristic() < bestFallbackNode.getHeuristic()) {
          bestFallbackNode = currentNode;
        }

        if (hasReachedPathLengthLimit(currentNode)) {
          return new PathfinderResultImpl(
              PathState.LENGTH_LIMITED, reconstructPath(start, target, currentNode));
        }

        if (currentNode.isTarget(target)) {
          return new PathfinderResultImpl(
              PathState.FOUND, reconstructPath(start, target, currentNode));
        }

        processSuccessors(start, target, currentNode, openSet, searchContext);
      }

      return determinePostLoopResult(iteration, start, target, bestFallbackNode);

    } finally {
      for (Processor processor : processors) {
        try {
          processor.finalizeSearch(searchContext);
        } catch (Exception e) {
          /*
           * Deliberately printStackTrace() instead of a logging framework: as a library we do not
           * pull in SLF4J / Log4j; consumers wrap their own logger around their Processors.
           */
          //noinspection CallToPrintStackTrace
          System.err.println("An exception occurred during pathfinding finalization:");
          e.printStackTrace();
        }
      }
      performAlgorithmCleanup();
    }
  }

  double calculateHeapKey(Node neighbor, double fCost) {
    double heuristic = neighbor.getHeuristic();
    double tieBreaker = TIE_BREAKER_WEIGHT * (heuristic / (Math.abs(fCost) + 1));
    double heapKey = fCost - tieBreaker;

    if (Double.isNaN(heapKey) || Double.isInfinite(heapKey)) {
      heapKey = fCost;
    }

    return heapKey;
  }

  /**
   * Estimates a heap capacity that fits the expected open-set peak for the upcoming search.
   *
   * <p>The estimate is {@code manhattanDistance(start, target) × branchingFactor}, capped by {@link
   * PathfinderConfiguration#getMaxIterations() maxIterations} (the algorithm cannot expand more
   * nodes than that anyway) and floored by {@link #MIN_INITIAL_HEAP_CAPACITY} to keep tiny searches
   * from allocating sub-cacheline arrays.
   *
   * <p>Manhattan distance is the strictest upper bound on the number of steps any traversal mode
   * (cardinal-only or diagonal) needs to cover. The branching factor (number of vectors returned by
   * the current {@link INeighborStrategy} at the start position) approximates how many open nodes
   * each expanded node contributes to the frontier. Together they capture a realistic peak for the
   * open set in well-behaved terrain. Pathological maze-like terrain can still exceed it, but only
   * up to the {@code maxIterations} ceiling, at which point the heap grows dynamically.
   */
  private int estimateInitialHeapCapacity(PathPosition start, PathPosition target) {
    int branching = Math.max(1, Iterables.size(neighborStrategy.getOffsets(start)));
    return computeInitialHeapCapacity(
        start, target, branching, pathfinderConfiguration.getMaxIterations());
  }

  /**
   * Retrieves a combined list of all applicable processors for pathfinding. Combines processors
   * from both node validation and node cost categories, if available.
   *
   * @return A list of {@link Processor} instances that will participate in the pathfinding
   *     operation. The list can be empty if no processors are configured.
   */
  private List<Processor> getProcessors() {
    List<Processor> processors = new ArrayList<>();
    if (validationProcessors != null && !validationProcessors.isEmpty())
      processors.addAll(validationProcessors);
    if (costProcessors != null && !costProcessors.isEmpty()) processors.addAll(costProcessors);
    return processors;
  }

  /**
   * Creates the initial {@link Node} for the start position.
   *
   * @param startPos The effective start position.
   * @param targetPos The effective target position.
   * @return The created start node.
   */
  protected Node createStartNode(PathPosition startPos, PathPosition targetPos) {
    return new Node(
        startPos,
        startPos,
        targetPos,
        pathfinderConfiguration.getHeuristicWeights(),
        pathfinderConfiguration.getHeuristicStrategy(),
        0); // Depth of start node is 0
  }

  /**
   * Checks if the current node's depth exceeds the configured maximum path length.
   *
   * @param currentNode The node to check.
   * @return {@code true} if the length limit is reached.
   */
  private boolean hasReachedPathLengthLimit(Node currentNode) {
    int maxLength = pathfinderConfiguration.getMaxLength();
    return maxLength > 0 && currentNode.getDepth() >= maxLength;
  }

  /**
   * Determines the pathfinding result when the main loop finishes without finding the target. This
   * could be due to reaching max iterations or the open set becoming empty.
   *
   * @param iterations The maximum iterations reached.
   * @param start The effective start position.
   * @param target The effective target position.
   * @param fallbackNode The best node found to use for a fallback path.
   * @return The determined {@link PathfinderResult}.
   */
  private PathfinderResult determinePostLoopResult(
      int iterations, PathPosition start, PathPosition target, Node fallbackNode) {

    if (iterations >= pathfinderConfiguration.getMaxIterations()) {
      return new PathfinderResultImpl(
          PathState.MAX_ITERATIONS_REACHED, reconstructPath(start, target, fallbackNode));
    }

    if (pathfinderConfiguration.isFallback()) {
      return new PathfinderResultImpl(
          PathState.FALLBACK, reconstructPath(start, target, fallbackNode));
    }

    return new PathfinderResultImpl(
        PathState.FAILED, new PathImpl(start, target, EMPTY_PATH_POSITIONS));
  }

  /**
   * Reconstructs the path by tracing back from the given end node to the start node.
   *
   * @param endNode The node from which to trace back.
   * @return The reconstructed {@link Path}.
   */
  protected Path reconstructPath(PathPosition start, PathPosition target, Node endNode) {
    if (endNode.getParent() == null && endNode.getDepth() == 0) {
      return new PathImpl(start, target, Collections.singletonList(endNode.getPosition()));
    }
    List<PathPosition> pathPositions = tracePathPositionsFromNode(endNode);
    return new PathImpl(start, target, pathPositions);
  }

  private List<PathPosition> tracePathPositionsFromNode(Node leafNode) {
    List<PathPosition> path = new ArrayList<>();
    Node currentNode = leafNode;
    while (currentNode != null) {
      path.add(currentNode.getPosition());
      currentNode = currentNode.getParent();
    }
    Collections.reverse(path);
    return path;
  }

  /** Inserts the start node into the open set and updates any internal mapping. */
  protected abstract void insertStartNode(Node node, double fCost, MinHeap openSet);

  /**
   * Extracts the node with the lowest cost from the open set and retrieves the corresponding Node
   * object.
   */
  protected abstract Node extractBestNode(MinHeap openSet);

  /**
   * Prepares the algorithm-specific initial setup required before executing the pathfinding logic.
   * This method is designed to be overridden by subclasses to implement their respective
   * initialization logic, such as setting up data structures, precomputing values, or resetting
   * internal state. It is called at the beginning of a pathfinding request.
   */
  protected abstract void initializeSearch();

  /**
   * Marks the given node as expanded (i.e., added to the "closed set"). Subclasses should implement
   * this to update their specific closed set mechanism.
   *
   * @param node The node that has been taken from the open set and is being expanded.
   */
  protected abstract void markNodeAsExpanded(Node node);

  /**
   * Abstract method for algorithm-specific cleanup, called after pathfinding execution. To be
   * implemented by subclasses like {@link AStarPathfinder}.
   */
  protected abstract void performAlgorithmCleanup();

  /**
   * Abstract method representing the core logic of processing successor nodes for a given {@code
   * currentNode}. Implementations (like A*) should:
   *
   * <ol>
   *   <li>Generate potential successor positions.
   *   <li>Create {@link Node} objects for these successors.
   *   <li>Validate these nodes (e.g., traversability, bounds, visited status). This is where
   *       processors will hook in.
   *   <li>Calculate their G and H costs. G-costs will be influenced by cost processors.
   *   <li>Add valid successor nodes with their F-costs to the {@code openSet}.
   * </ol>
   *
   * @param requestStart The original start {@link PathPosition} of the pathfinding request.
   * @param requestTarget The original target {@link PathPosition} of the pathfinding request.
   * @param currentNode The current {@link Node} being expanded.
   * @param openSet The priority queue (open set) to add new successor nodes to.
   */
  protected abstract void processSuccessors(
      PathPosition requestStart,
      PathPosition requestTarget,
      Node currentNode,
      MinHeap openSet,
      SearchContext searchContext);
}
