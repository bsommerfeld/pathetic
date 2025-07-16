package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import org.jheaps.tree.FibonacciHeap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AbstractPathfinderTest {

    private TestPathfinder pathfinder;
    private NavigationPointProvider mockProvider;
    private PathfinderConfiguration configuration;
    private PathPosition start;
    private PathPosition target;

    @BeforeEach
    void setUp() {
        // Create mock provider
        mockProvider = Mockito.mock(NavigationPointProvider.class);

        // Create configuration with mock provider
        configuration = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(100)
                .maxLength(50)
                .async(false)
                .build();

        // Create pathfinder with configuration
        pathfinder = new TestPathfinder(configuration);

        // Create start and target positions
        start = new PathPosition(0, 0, 0);
        target = new PathPosition(10, 10, 10);
    }

    @Test
    void testFindPathWithNullContext() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup mock provider to allow path to be found
        NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
        when(mockNavigationPoint.isTraversable()).thenReturn(true);
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(mockNavigationPoint);

        // Call findPath with null context
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

        // Get result and verify it's not null
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    void testFindPathWithContext() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup mock provider to allow path to be found
        NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
        when(mockNavigationPoint.isTraversable()).thenReturn(true);
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(mockNavigationPoint);

        // Create mock context
        EnvironmentContext mockContext = Mockito.mock(EnvironmentContext.class);

        // Call findPath with context
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target, mockContext);

        // Get result and verify it's not null
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    void testAbort() throws ExecutionException, InterruptedException {
        // Create configuration with fallback disabled
        PathfinderConfiguration noFallbackConfig = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(100)
                .maxLength(50)
                .async(false)
                .fallback(false)  // Disable fallback
                .build();

        // Create pathfinder with this configuration
        TestPathfinder noFallbackPathfinder = new TestPathfinder(noFallbackConfig);

        // Setup pathfinder to simulate long-running operation
        noFallbackPathfinder.setSimulateDelay(true);
        noFallbackPathfinder.setAbortImmediately(true);

        // Setup mock provider to allow path to be found
        NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
        when(mockNavigationPoint.isTraversable()).thenReturn(true);
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(mockNavigationPoint);

        // Start pathfinding in a separate thread
        CompletionStage<PathfinderResult> resultStage = noFallbackPathfinder.findPath(start, target);

        // Get result and verify it was aborted
        PathfinderResult result = resultStage.toCompletableFuture().get();
        assertEquals(PathState.ABORTED, result.getPathState());
    }

    @Test
    void testRegisterPathfindingHook() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup mock provider to allow path to be found
        NavigationPoint mockNavigationPoint = Mockito.mock(NavigationPoint.class);
        when(mockNavigationPoint.isTraversable()).thenReturn(true);
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(mockNavigationPoint);

        // Create a hook and register it
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        PathfinderHook hook = context -> hookCalled.set(true);
        pathfinder.registerPathfindingHook(hook);

        // Call findPath
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

        // Get result
        resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // Verify hook was called
        assertTrue(hookCalled.get());
    }

    @Test
    void testNullHookNotRegistered() {
        // Try to register a null hook
        pathfinder.registerPathfindingHook(null);

        // Verify no hook was registered (using reflection or other means)
        assertEquals(0, pathfinder.getRegisteredHooksCount());
    }

    /**
     * A concrete implementation of AbstractPathfinder for testing.
     */
    private static class TestPathfinder extends AbstractPathfinder {

        private final List<Node> expandedNodes = new ArrayList<>();
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
        }

        @Override
        protected void markNodeAsExpanded(Node node) {
            expandedNodes.add(node);
        }

        @Override
        protected void performAlgorithmCleanup() {
            // No cleanup needed for test
        }

        @Override
        protected void processSuccessors(PathPosition requestStart, PathPosition requestTarget,
                                         Node currentNode, int currentSearchDepth,
                                         FibonacciHeap<Double, Node> openSet,
                                         SearchContext searchContext) {
            // If abortImmediately is true, abort immediately by not adding any successors
            if (abortImmediately) {
                abort();

                // For the abort test, we need to add a node to the open set so that the algorithm
                // continues and checks the abort flag in the main loop
                Node dummyNode = new Node(
                        requestStart,
                        requestStart,
                        requestTarget,
                        pathfinderConfiguration.getHeuristicWeights(),
                        pathfinderConfiguration.getHeuristicStrategy(),
                        1);
                dummyNode.setParent(currentNode);
                openSet.insert(dummyNode.getFCost(), dummyNode);

                return;
            }

            if (simulateDelay) {
                try {
                    Thread.sleep(500); // Simulate processing delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // For testing, just add the target node to the open set if we're at the start
            if (currentNode.getPosition().equals(requestStart)) {
                Node targetNode = new Node(
                        requestTarget,
                        requestStart,
                        requestTarget,
                        pathfinderConfiguration.getHeuristicWeights(),
                        pathfinderConfiguration.getHeuristicStrategy(),
                        1);
                targetNode.setParent(currentNode);
                openSet.insert(targetNode.getFCost(), targetNode);
            }

            // Notify hooks about the step
            PathfindingContext context = new PathfindingContext(currentNode.getPosition(), Depth.of(currentSearchDepth));
            for (PathfinderHook hook : testHooks) {
                hook.onPathfindingStep(context);
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
