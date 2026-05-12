package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.MoreExecutors;
import de.bsommerfeld.pathetic.api.pathing.PathfindingSearch;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
  void testFindPathWithNullContext() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    PathfindingSearch search = pathfinder.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
  }

  @Test
  void testFindPathWithContext() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    EnvironmentContext mockContext = Mockito.mock(EnvironmentContext.class);

    PathfindingSearch search = pathfinder.findPath(start, target, mockContext);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
  }

  @Test
  void testRegisterPathfindingHook() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    AtomicBoolean hookCalled = new AtomicBoolean(false);
    PathfinderHook hook = context -> hookCalled.set(true);
    pathfinder.registerPathfindingHook(hook);

    PathfindingSearch search = pathfinder.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertTrue(hookCalled.get());
  }

  /*
   * Hooks are snapshotted once per search: all hooks of a single iteration observe the same
   * PathfindingContext instance (cheaper than allocating one per hook), and hooks registered
   * after a search has started do not apply to that search.
   */
  @Test
  void allHooksInOneIterationShareTheSameContextInstance() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    AtomicReference<Object> seenByA = new AtomicReference<>();
    AtomicReference<Object> seenByB = new AtomicReference<>();
    pathfinder.registerPathfindingHook(ctx -> seenByA.compareAndSet(null, ctx));
    pathfinder.registerPathfindingHook(ctx -> seenByB.compareAndSet(null, ctx));

    pathfinder.findPath(start, target).resultBlocking();

    assertNotNull(seenByA.get());
    assertNotNull(seenByB.get());
    assertSame(
        seenByA.get(),
        seenByB.get(),
        "all hooks of the same iteration must receive the same context instance");
  }

  @Test
  void hooksRegisteredAfterSearchStartDoNotApplyToThatSearch() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    AtomicBoolean lateHookSawCurrentSearch = new AtomicBoolean(false);
    PathfinderHook earlyHook =
        ctx -> {
          // Register a second hook DURING the first iteration of the current search.
          pathfinder.registerPathfindingHook(c -> lateHookSawCurrentSearch.set(true));
        };
    pathfinder.registerPathfindingHook(earlyHook);

    pathfinder.findPath(start, target).resultBlocking();

    assertEquals(
        false,
        lateHookSawCurrentSearch.get(),
        "a hook registered mid-search must not be invoked for the running search");

    // Sanity check: it IS invoked for the next search.
    AtomicBoolean nextSearchSeen = new AtomicBoolean(false);
    pathfinder.registerPathfindingHook(c -> nextSearchSeen.set(true));
    pathfinder.findPath(start, target).resultBlocking();
    assertTrue(nextSearchSeen.get());
  }

  @Test
  void testNullHookNotRegistered() {
    pathfinder.registerPathfindingHook(null);
    assertEquals(0, pathfinder.getRegisteredHooksCount());
  }

  /*
   * The 2-arg findPath overload passes null as the EnvironmentContext. The Pathfinder.findPath
   * JavaDoc and SearchContext.getEnvironmentContext both declare the value nullable, so the null
   * must actually reach processors as null. Locks in that contract end-to-end; a future
   * engine-internal dereference would fail loudly here instead of in user code.
   */
  @Test
  void findPathWithoutContextDeliversNullEnvironmentContextToProcessors() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    AtomicReference<Boolean> seenContextWasNull = new AtomicReference<>();
    ValidationProcessor recorder =
        new ValidationProcessor() {
          @Override
          public boolean isValid(EvaluationContext context) {
            seenContextWasNull.compareAndSet(
                null, context.getSearchContext().getEnvironmentContext() == null);
            return true;
          }
        };

    configuration =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(10)
            .async(false)
            .validationProcessors(Collections.singletonList(recorder))
            .build();
    pathfinder = new TestPathfinder(configuration);

    // 2-arg overload delegates findPath(start, target, null) internally.
    pathfinder.findPath(start, target).resultBlocking();
    assertTrue(
        Boolean.TRUE.equals(seenContextWasNull.get()),
        "Processor must observe EnvironmentContext as null when no context was supplied");
  }

  @Test
  void findPathWithContextDeliversIdentityToProcessors() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    EnvironmentContext supplied = Mockito.mock(EnvironmentContext.class);
    AtomicReference<EnvironmentContext> seenContext = new AtomicReference<>();
    ValidationProcessor recorder =
        new ValidationProcessor() {
          @Override
          public boolean isValid(EvaluationContext context) {
            seenContext.compareAndSet(null, context.getSearchContext().getEnvironmentContext());
            return true;
          }
        };

    configuration =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(10)
            .async(false)
            .validationProcessors(Collections.singletonList(recorder))
            .build();
    pathfinder = new TestPathfinder(configuration);

    pathfinder.findPath(start, target, supplied).resultBlocking();
    assertSame(
        supplied,
        seenContext.get(),
        "Processor must receive the exact EnvironmentContext instance supplied to findPath");
  }

  /*
   * When a processor's finalizeSearch throws, the pathfinder must swallow the exception (the
   * main result is already known) AND emit the full stack trace to stderr, not just the message.
   */
  @Test
  void finalizerExceptionPrintsFullStackTraceToStderr() {
    NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
    when(mockNavigationPoint.isTraversable()).thenReturn(true);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(mockNavigationPoint);

    ValidationProcessor throwingOnFinalize =
        new ValidationProcessor() {
          @Override
          public boolean isValid(EvaluationContext context) {
            return true;
          }

          @Override
          public void finalizeSearch(SearchContext context) {
            throw new RuntimeException("boom-finalize");
          }
        };

    configuration =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(10)
            .async(false)
            .validationProcessors(Collections.singletonList(throwingOnFinalize))
            .build();
    pathfinder = new TestPathfinder(configuration);

    PrintStream originalErr = System.err;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
    try {
      // Search must complete without the finalizer exception propagating.
      PathfindingSearch search = pathfinder.findPath(start, target);
      AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
      search.ifPresent(resultRef::set);
      // resultRef may be null when state is not FOUND/FALLBACK; the important contract is
      // that the call did not throw despite the finalizer's RuntimeException.
    } finally {
      System.setErr(originalErr);
    }

    String captured = buffer.toString(StandardCharsets.UTF_8);
    assertTrue(
        captured.contains("RuntimeException"),
        "stderr should contain the exception class - got: " + captured);
    assertTrue(
        captured.contains("boom-finalize"),
        "stderr should contain the exception message - got: " + captured);
    assertTrue(
        captured.contains("at "),
        "stderr should contain a stack trace frame (e.printStackTrace), not just the message");
  }

  @Test
  void testNonSharedExecutorPool() {
    final ExecutorService spyedExecutorService = Mockito.spy(MoreExecutors.newDirectExecutorService());
    configuration = PathfinderConfiguration.builder()
      .provider(mockProvider)
      .maxIterations(100)
      .maxLength(50)
      .async(true)
      .executorService(spyedExecutorService)
      .build();

    final Optional<PathfinderResult> result = new TestPathfinder(configuration).findPath(start, target).result();
    assertTrue(result.isPresent());
    verify(spyedExecutorService, times(1)).execute(any());
  }

  /** A concrete implementation of AbstractPathfinder for testing. */
  private static class TestPathfinder extends AbstractPathfinder {

    private final List<Node> expandedNodes = new ArrayList<>();
    // Simple test map to simulate what PathfindingSession does in AStarPathfinder
    private final Map<Long, Node> testNodeMap = new HashMap<>();

    private final Set<PathfinderHook> testHooks = new HashSet<>();
    private boolean simulateDelay = false;

    public TestPathfinder(PathfinderConfiguration pathfinderConfiguration) {
      super(pathfinderConfiguration);
    }

    @Override
    protected void initializeSearch() {
      expandedNodes.clear();
      testNodeMap.clear();
    }

    // --- NEUE ABSTRAKTE METHODEN IMPLEMENTIEREN ---

    @Override
    protected void insertStartNode(Node node, double fCost, MinHeap openSet) {
      long packedPos = RegionKey.pack(node.getPosition());
      openSet.insertOrUpdate(packedPos, fCost);
      testNodeMap.put(packedPos, node);
    }

    @Override
    protected Node extractBestNode(MinHeap openSet) {
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
        MinHeap openSet, // Typ angepasst!
        SearchContext searchContext) {
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
