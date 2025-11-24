package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import de.bsommerfeld.pathetic.engine.pathfinder.heap.PrimitiveMinHeap;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AbstractPathfinderTest {

  private TestPathfinder pathfinder;
  private NavigationPointProvider mockProvider;
  private PathfinderConfiguration configuration;
  private PathPosition start;
  private PathPosition target;

  @BeforeEach
  void setUp() {
    mockProvider = Mockito.mock(NavigationPointProvider.class);

    configuration =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(false)
            .build();

    pathfinder = new TestPathfinder(configuration);

    start = new PathPosition(0, 0, 0);
    target = new PathPosition(10, 10, 10);
  }

  @Test
  void testFindPathWithNullContext()
      throws ExecutionException, InterruptedException, TimeoutException {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

    PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertNotNull(result);
  }

  @Test
  void testFindPathWithContext() throws ExecutionException, InterruptedException, TimeoutException {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    EnvironmentContext mockContext = Mockito.mock(EnvironmentContext.class);

    CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target, mockContext);

    PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertNotNull(result);
  }

  @Test
  void testAbort() throws ExecutionException, InterruptedException {
    PathfinderConfiguration noFallbackConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(false)
            .fallback(false)
            .build();

    TestPathfinder noFallbackPathfinder = new TestPathfinder(noFallbackConfig);

    noFallbackPathfinder.setSimulateDelay(true);
    noFallbackPathfinder.setAbortImmediately(true);

    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    CompletionStage<PathfinderResult> resultStage = noFallbackPathfinder.findPath(start, target);

    PathfinderResult result = resultStage.toCompletableFuture().get();
    assertEquals(PathState.ABORTED, result.getPathState());
  }

  @Test
  void testRegisterPathfindingHook()
      throws ExecutionException, InterruptedException, TimeoutException {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    AtomicBoolean hookCalled = new AtomicBoolean(false);
    PathfinderHook hook = context -> hookCalled.set(true);
    pathfinder.registerPathfindingHook(hook);

    CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

    resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

    assertTrue(hookCalled.get());
  }

  @Test
  void testNullHookNotRegistered() {
    pathfinder.registerPathfindingHook(null);
    assertEquals(0, pathfinder.getRegisteredHooksCount());
  }

  /** A concrete implementation of AbstractPathfinder for testing. */
  private static class TestPathfinder extends AbstractPathfinder {

    private final List<Node> expandedNodes = new ArrayList<>();
    // Simple test map to simulate what PathfindingSession does in AStarPathfinder
    private final Map<Long, Node> testNodeMap = new HashMap<>();

    private final Set<PathfinderHook> testHooks = new HashSet<>();
    private boolean simulateDelay = false;
    private boolean abortImmediately = false;

    public TestPathfinder(PathfinderConfiguration pathfinderConfiguration) {
      super(pathfinderConfiguration);
    }

    public void setAbortImmediately(boolean abortImmediately) {
      this.abortImmediately = abortImmediately;
    }

    @Override
    protected void initializeSearch() {
      expandedNodes.clear();
      testNodeMap.clear();
    }

    // --- NEUE ABSTRAKTE METHODEN IMPLEMENTIEREN ---

    @Override
    protected void insertStartNode(Node node, double fCost, PrimitiveMinHeap openSet) {
      long packedPos = RegionKey.pack(node.getPosition());
      openSet.insertOrUpdate(packedPos, fCost);
      testNodeMap.put(packedPos, node);
    }

    @Override
    protected Node extractBestNode(PrimitiveMinHeap openSet) {
      long packedPos = openSet.extractMin();
      Node node = testNodeMap.get(packedPos);
      testNodeMap.remove(packedPos);
      return node;
    }

    // ----------------------------------------------

    @Override
    protected void markNodeAsExpanded(Node node) {
      expandedNodes.add(node);
    }

    @Override
    protected void performAlgorithmCleanup() {
      testNodeMap.clear();
    }

    @Override
    protected void processSuccessors(
        PathPosition requestStart,
        PathPosition requestTarget,
        Node currentNode,
        PrimitiveMinHeap openSet, // Typ angepasst!
        SearchContext searchContext) {

      if (abortImmediately) {
        abort();

        // Dummy node add logic for abort test
        Node dummyNode =
            new Node(
                requestStart,
                requestStart,
                requestTarget,
                pathfinderConfiguration.getHeuristicWeights(),
                pathfinderConfiguration.getHeuristicStrategy(),
                1);
        dummyNode.setParent(currentNode);

        long packedDummy = RegionKey.pack(dummyNode.getPosition());
        openSet.insertOrUpdate(packedDummy, dummyNode.getFCost());
        testNodeMap.put(packedDummy, dummyNode);

        return;
      }

      if (simulateDelay) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      // Add target node if at start (Test Logic)
      if (currentNode.getPosition().equals(requestStart)) {
        Node targetNode =
            new Node(
                requestTarget,
                requestStart,
                requestTarget,
                pathfinderConfiguration.getHeuristicWeights(),
                pathfinderConfiguration.getHeuristicStrategy(),
                1);
        targetNode.setParent(currentNode);

        long packedTarget = RegionKey.pack(targetNode.getPosition());
        openSet.insertOrUpdate(packedTarget, targetNode.getFCost());
        testNodeMap.put(packedTarget, targetNode);
      }
    }

    public void setSimulateDelay(boolean simulateDelay) {
      this.simulateDelay = simulateDelay;
    }

    @Override
    public void registerPathfindingHook(PathfinderHook hook) {
      super.registerPathfindingHook(hook);
      if (hook != null) {
        testHooks.add(hook);
      }
    }

    public int getRegisteredHooksCount() {
      return testHooks.size();
    }
  }
}
