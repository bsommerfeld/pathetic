package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext;
import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.result.PathImpl;
import de.bsommerfeld.pathetic.engine.result.PathfinderResultImpl;
import de.bsommerfeld.pathetic.engine.util.ErrorLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jheaps.tree.FibonacciHeap;

/**
 * The AbstractPathfinder class provides a skeletal implementation of the Pathfinder interface and
 * defines the common behavior for all pathfinding algorithms. It provides a default implementation
 * for determining the offset and snapshot manager based on the pathing rule set.
 *
 * <p>This class now operates in a tick-wise manner, meaning that the pathfinding process progresses
 * incrementally, with each "tick" representing a small step in the algorithm's execution. At each
 * tick, the algorithm evaluates nodes, updates the priority queue, and checks for conditions such
 * as reaching the target or encountering an abort signal.
 */
abstract class AbstractPathfinder implements Pathfinder {

  protected static final Set<PathPosition> EMPTY_LINKED_HASHSET =
      Collections.unmodifiableSet(new LinkedHashSet<>(0));

  private static final ExecutorService PATHING_EXECUTOR = Executors.newWorkStealingPool();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(PATHING_EXECUTOR::shutdown));
  }

  private final Set<PathfinderHook> pathfinderHooks = new HashSet<>();

  protected final PathfinderConfiguration pathfinderConfiguration;

  private volatile boolean aborted;

  protected AbstractPathfinder(PathfinderConfiguration pathfinderConfiguration) {
    this.pathfinderConfiguration = pathfinderConfiguration;
  }

  @Override
  public CompletionStage<PathfinderResult> findPath(PathPosition start, PathPosition target) {
    if (shouldSkipPathing(start, target)) {
      return CompletableFuture.completedFuture(
          new PathfinderResultImpl(
              PathState.INITIALLY_FAILED, new PathImpl(start, target, EMPTY_LINKED_HASHSET)));
    }

    return initiatePathing(start, target);
  }

  /** Give the pathfinder the final shot */
  @Override
  public void abort() {
    this.aborted = true;
  }

  @Override
  public void registerPathfindingHook(PathfinderHook hook) {
    pathfinderHooks.add(hook);
  }

  private boolean shouldSkipPathing(PathPosition start, PathPosition target) {
    return !isSameEnvironment(start, target) || isSameBlock(start, target);
  }

  private boolean isSameEnvironment(PathPosition start, PathPosition target) {
    return start.getPathEnvironment().equals(target.getPathEnvironment());
  }

  private boolean isSameBlock(PathPosition start, PathPosition target) {
    return start.isInSameBlock(target);
  }

  private CompletionStage<PathfinderResult> initiatePathing(
      PathPosition start, PathPosition target) {
    return pathfinderConfiguration.isAsync()
        ? CompletableFuture.supplyAsync(
                () -> executePathingAndCleanupFilters(start, target), PATHING_EXECUTOR)
            .exceptionally(throwable -> handleException(start, target, throwable))
        : initiateSyncPathing(start, target);
  }

  private PathfinderResult executePathing(PathPosition start, PathPosition target) {
    try {
      Node startNode = createStartNode(start, target);
      FibonacciHeap<Double, Node> nodeQueue = new FibonacciHeap<>();
      nodeQueue.insert(startNode.getFCost(), startNode);

      Depth depth = Depth.of(1);
      Node fallbackNode = startNode;

      while (!nodeQueue.isEmpty()
          && depth.getValue() <= pathfinderConfiguration.getMaxIterations()) {

        pathfinderHooks.forEach(hook -> hook.onPathfindingStep(new PathfindingContext(depth)));

        if (isAborted()) return abortedPathing(fallbackNode);

        Node currentNode = nodeQueue.deleteMin().getValue();
        fallbackNode = currentNode;

        if (hasReachedLengthLimit(currentNode)) {
          return new PathfinderResultImpl(PathState.LENGTH_LIMITED, fetchRetracedPath(currentNode));
        }

        if (currentNode.isTarget()) {
          return new PathfinderResultImpl(PathState.FOUND, fetchRetracedPath(currentNode));
        }

        tick(start, target, currentNode, depth, nodeQueue);
      }

      aborted = false; // just in case

      return backupPathfindingOrFailure(depth, start, target, fallbackNode);
    } catch (Exception e) {
      throw ErrorLogger.logFatalErrorWithStacktrace("Failed to find path", e);
    }
  }

  private PathfinderResult abortedPathing(Node fallbackNode) {
    aborted = false;
    return new PathfinderResultImpl(PathState.ABORTED, fetchRetracedPath(fallbackNode));
  }

  private boolean isAborted() {
    return aborted;
  }

  private CompletionStage<PathfinderResult> initiateSyncPathing(
      PathPosition start, PathPosition target) {
    try {
      return CompletableFuture.completedFuture(executePathingAndCleanupFilters(start, target));
    } catch (Exception e) {
      throw ErrorLogger.logFatalError("Failed to find path sync", e);
    }
  }

  private PathfinderResult executePathingAndCleanupFilters(
      PathPosition start, PathPosition target) {
    PathfinderResult pathfinderResult = executePathing(start, target);
    cleanup();
    return pathfinderResult;
  }

  private PathfinderResult handleException(
      PathPosition start, PathPosition target, Throwable throwable) {
    ErrorLogger.logFatalError("Failed to find path async", throwable);
    return new PathfinderResultImpl(
        PathState.FAILED, new PathImpl(start, target, EMPTY_LINKED_HASHSET));
  }

  private Node createStartNode(PathPosition start, PathPosition target) {
    return new Node(
        start.floor(),
        start.floor(),
        target.floor(),
        pathfinderConfiguration.getHeuristicWeights(),
        0);
  }

  private boolean hasReachedLengthLimit(Node currentNode) {
    return pathfinderConfiguration.getMaxLength() != 0
        && currentNode.getDepth() > pathfinderConfiguration.getMaxLength();
  }

  /** If the pathfinder has failed to find a path, it will try to still give a result. */
  private PathfinderResult backupPathfindingOrFailure(
      Depth depth, PathPosition start, PathPosition target, Node fallbackNode) {

    Optional<PathfinderResult> maxIterationsResult = maxIterationsReached(depth, fallbackNode);
    if (maxIterationsResult.isPresent()) {
      return maxIterationsResult.get();
    }

    Optional<PathfinderResult> fallbackResult = fallback(fallbackNode);
    return fallbackResult.orElseGet(
        () ->
            new PathfinderResultImpl(
                PathState.FAILED, new PathImpl(start, target, EMPTY_LINKED_HASHSET)));
  }

  private Optional<PathfinderResult> maxIterationsReached(Depth depth, Node fallbackNode) {
    if (depth.getValue() > pathfinderConfiguration.getMaxIterations())
      return Optional.of(
          new PathfinderResultImpl(
              PathState.MAX_ITERATIONS_REACHED, fetchRetracedPath(fallbackNode)));
    return Optional.empty();
  }

  private Optional<PathfinderResult> fallback(Node fallbackNode) {
    if (pathfinderConfiguration.isFallback())
      return Optional.of(
          new PathfinderResultImpl(PathState.FALLBACK, fetchRetracedPath(fallbackNode)));
    return Optional.empty();
  }

  private Path fetchRetracedPath(Node node) {
    if (node.getParent() == null)
      return new PathImpl(
          node.getStart(), node.getTarget(), Collections.singletonList(node.getPosition()));

    List<PathPosition> path = tracePathFromNode(node);
    return new PathImpl(node.getStart(), node.getTarget(), path);
  }

  private List<PathPosition> tracePathFromNode(Node endNode) {
    List<PathPosition> path = new ArrayList<>();
    Node currentNode = endNode;

    while (currentNode != null) {
      path.add(currentNode.getPosition());
      currentNode = currentNode.getParent();
    }

    Collections.reverse(path); // Reverse the path to get the correct order
    return path;
  }

  /**
   * @deprecated Will be realized in a better way in the future
   */
  @Deprecated
  protected abstract void cleanup();

  /** The tick method is called to tick the pathfinding algorithm. */
  protected abstract void tick(
      PathPosition start,
      PathPosition target,
      Node currentNode,
      Depth depth,
      FibonacciHeap<Double, Node> nodeQueue);
}
