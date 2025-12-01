package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeCostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.Processor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.PrimitiveMinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.NodeEvaluationContextImpl;
import de.bsommerfeld.pathetic.engine.pathfinder.processing.SearchContextImpl;
import de.bsommerfeld.pathetic.engine.result.PathImpl;
import de.bsommerfeld.pathetic.engine.result.PathfinderResultImpl;
import de.bsommerfeld.pathetic.engine.util.ErrorLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

  private static final double TIE_BREAKER_WEIGHT = 1e-6;
  private static final ExecutorService PATHING_EXECUTOR_SERVICE =
      Executors.newWorkStealingPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(AbstractPathfinder::shutdownExecutor));
  }

  protected final PathfinderConfiguration pathfinderConfiguration;
  protected final NavigationPointProvider navigationPointProvider;
  protected final List<NodeValidationProcessor> nodeValidationProcessors;
  protected final List<NodeCostProcessor> nodeCostProcessors;
  protected final INeighborStrategy neighborStrategy;

  private final Set<PathfinderHook> pathfinderHooks = Collections.synchronizedSet(new HashSet<>());

  private volatile boolean abortRequested = false;

  protected AbstractPathfinder(PathfinderConfiguration pathfinderConfiguration) {
    this.pathfinderConfiguration =
        Objects.requireNonNull(pathfinderConfiguration, "pathfinderConfiguration must not be null");

    this.navigationPointProvider =
        Objects.requireNonNull(
            pathfinderConfiguration.getProvider(),
            "NavigationPointProvider from configuration has to be set.");

    this.nodeValidationProcessors = pathfinderConfiguration.getNodeValidationProcessors();
    this.nodeCostProcessors = pathfinderConfiguration.getNodeCostProcessors();
    this.neighborStrategy = pathfinderConfiguration.getNeighborStrategy();
  }

  private static void shutdownExecutor() {
    PATHING_EXECUTOR_SERVICE.shutdown();
    try {
      if (!PATHING_EXECUTOR_SERVICE.awaitTermination(5, TimeUnit.SECONDS)) {
        PATHING_EXECUTOR_SERVICE.shutdownNow();
      }
    } catch (InterruptedException e) {
      PATHING_EXECUTOR_SERVICE.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public CompletionStage<PathfinderResult> findPath(
      PathPosition start, PathPosition target, EnvironmentContext environmentContext) {
    Objects.requireNonNull(start, "start PathPosition must not be null");
    Objects.requireNonNull(target, "target PathPosition must not be null");
    this.abortRequested = false; // Reset abort flag for new search
    return initiatePathing(start, target, environmentContext);
  }

  /**
   * Requests the current pathfinding operation to abort. The abortion is cooperative and might not
   * be immediate.
   */
  @Override
  public void abort() {
    this.abortRequested = true;
  }

  @Override
  public void registerPathfindingHook(PathfinderHook hook) {
    if (hook != null) {
      this.pathfinderHooks.add(hook);
    }
  }

  private CompletionStage<PathfinderResult> initiatePathing(
      PathPosition start, PathPosition target, EnvironmentContext environmentContext) {
    final PathPosition effectiveStart = start.floor();
    final PathPosition effectiveTarget = target.floor();

    if (pathfinderConfiguration.isAsync()) {
      return CompletableFuture.supplyAsync(
              () -> executePathingAlgorithm(effectiveStart, effectiveTarget, environmentContext),
              PATHING_EXECUTOR_SERVICE)
          .exceptionally(throwable -> handlePathingException(start, target, throwable));
    } else {
      try {
        return CompletableFuture.completedFuture(
            executePathingAlgorithm(effectiveStart, effectiveTarget, environmentContext));
      } catch (Exception e) {
        // Synchronous execution exceptions are wrapped to be consistent with async reporting
        return CompletableFuture.completedFuture(handlePathingException(start, target, e));
      }
    }
  }

  /**
   * Core pathfinding execution logic.
   *
   * @param start The effective (e.g., floored) start position.
   * @param target The effective (e.g., floored) target position.
   * @return The result of the pathfinding operation.
   */
  private PathfinderResult executePathingAlgorithm(
      PathPosition start, PathPosition target, EnvironmentContext environmentContext) {
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

      final NodeEvaluationContext startNodeContext =
          new NodeEvaluationContextImpl(
              searchContext, startNode, null, pathfinderConfiguration.getHeuristicStrategy());

      if (this.nodeValidationProcessors != null && !this.nodeValidationProcessors.isEmpty()) {
        final boolean isStartNodeInvalid =
            this.nodeValidationProcessors.stream()
                .anyMatch(validator -> !validator.isValid(startNodeContext));

        if (isStartNodeInvalid) {
          return new PathfinderResultImpl(
              PathState.FAILED, new PathImpl(start, target, EMPTY_PATH_POSITIONS));
        }
      }

      // TODO: bsommerfeld 24.11.2025: Magic number, we can turn this into a configurable later
      PrimitiveMinHeap openSet = new PrimitiveMinHeap(1024);

      double startKey;
      try {
        startKey = calculateHeapKey(startNode, startNode.getFCost());
      } catch (Throwable t) {
        startKey = startNode.getFCost();
      }

      insertStartNode(startNode, startKey, openSet);

      int currentDepth = 0;
      Node bestFallbackNode = startNode;

      while (!openSet.isEmpty() && currentDepth < pathfinderConfiguration.getMaxIterations()) {
        currentDepth++;

        if (this.abortRequested) {
          return createAbortedResult(start, target, bestFallbackNode);
        }

        Node currentNode = extractBestNode(openSet);
        markNodeAsExpanded(currentNode);

        final int finalCurrentDepth = currentDepth;
        pathfinderHooks.forEach(
            hook ->
                hook.onPathfindingStep(
                    new PathfindingContext(
                        currentNode.getPosition(), Depth.of(finalCurrentDepth))));

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

      return determinePostLoopResult(currentDepth, start, target, bestFallbackNode);
    } catch (Exception e) {
      ErrorLogger.logFatalErrorWithStacktrace("Pathfinding algorithm failed", e);
      return new PathfinderResultImpl(
          PathState.FAILED, new PathImpl(start, target, EMPTY_PATH_POSITIONS));
    } finally {
      List<Throwable> finalizeErrors = new ArrayList<>();
      for (Processor processor : processors) {
        try {
          processor.finalizeSearch(searchContext);
        } catch (Exception e) {
          finalizeErrors.add(e);
        }
      }
      if (!finalizeErrors.isEmpty()) {
        ErrorLogger.logFatalError("Errors during processor finalization: " + finalizeErrors, null);
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
   * Retrieves a combined list of all applicable processors for pathfinding. Combines processors
   * from both node validation and node cost categories, if available.
   *
   * @return A list of {@link Processor} instances that will participate in the pathfinding
   *     operation. The list can be empty if no processors are configured.
   */
  private List<Processor> getProcessors() {
    List<Processor> processors = new ArrayList<>();
    if (nodeValidationProcessors != null && !nodeValidationProcessors.isEmpty())
      processors.addAll(nodeValidationProcessors);
    if (nodeCostProcessors != null && !nodeCostProcessors.isEmpty())
      processors.addAll(nodeCostProcessors);
    return processors;
  }

  private PathfinderResult createAbortedResult(
      PathPosition start, PathPosition target, Node fallbackNode) {
    this.abortRequested = false;
    return new PathfinderResultImpl(
        PathState.ABORTED, reconstructPath(start, target, fallbackNode));
  }

  private PathfinderResult handlePathingException(
      PathPosition originalStart, PathPosition originalTarget, Throwable throwable) {
    ErrorLogger.logFatalError("Pathfinding execution failed (async or wrapped sync)", throwable);
    return new PathfinderResultImpl(
        PathState.FAILED, new PathImpl(originalStart, originalTarget, EMPTY_PATH_POSITIONS));
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
   * @param depthReached The maximum depth or iterations reached.
   * @param start The effective start position.
   * @param target The effective target position.
   * @param fallbackNode The best node found to use for a fallback path.
   * @return The determined {@link PathfinderResult}.
   */
  private PathfinderResult determinePostLoopResult(
      int depthReached, PathPosition start, PathPosition target, Node fallbackNode) {

    if (depthReached >= pathfinderConfiguration.getMaxIterations()) {
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
  protected abstract void insertStartNode(Node node, double fCost, PrimitiveMinHeap openSet);

  /**
   * Extracts the node with the lowest cost from the open set and retrieves the corresponding Node
   * object.
   */
  protected abstract Node extractBestNode(PrimitiveMinHeap openSet);

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
      PrimitiveMinHeap openSet,
      SearchContext searchContext);
}
