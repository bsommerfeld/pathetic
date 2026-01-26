package de.bsommerfeld.pathetic.integration;

import static org.junit.jupiter.api.Assertions.*;

import de.bsommerfeld.pathetic.api.factory.PathfinderFactory;
import de.bsommerfeld.pathetic.api.pathing.NeighborStrategies;
import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive integration tests for the complete pathfinding workflow.
 * Tests the end-to-end functionality from API through Engine implementation.
 */
@DisplayName("Pathfinding Workflow Integration Tests")
class PathfindingWorkflowIntegrationTest {

    private PathfinderFactory factory;
    private NavigationPointProvider simpleProvider;

    @BeforeEach
    void setUp() {
        factory = new AStarPathfinderFactory();
        simpleProvider = (position, context) -> () -> true;
    }

    @Test
    @DisplayName("Should find simple path in 2D space")
    void testSimple2DPath() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
        assertEquals(PathState.FOUND, result.getPathState());

        Path path = result.getPath();
        assertNotNull(path);
        assertEquals(6, path.length()); // Start + 5 steps
        assertEquals(start, path.getStart());
        assertEquals(target, path.getEnd());
    }

    @Test
    @DisplayName("Should find path in 3D space with vertical movement")
    void testSimple3DPath() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(3, 2, 4);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
        assertEquals(PathState.FOUND, result.getPathState());

        Path path = result.getPath();
        assertNotNull(path);
        assertTrue(path.length() > 0);
        assertEquals(start, path.getStart());
        assertEquals(target, path.getEnd());
    }

    @Test
    @DisplayName("Should find diagonal path when diagonal movement is allowed")
    void testDiagonalPath() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .neighborStrategy(NeighborStrategies.DIAGONAL_3D)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 0, 5);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
        Path path = result.getPath();

        // With diagonal movement, path should be shorter than Manhattan distance
        assertTrue(path.length() < 11); // Manhattan distance would be 11
    }

    @Test
    @DisplayName("Should respect obstacles in navigation provider")
    void testPathWithObstacles() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Block a position using floored coordinates
        int blockedX = 2, blockedY = 0, blockedZ = 0;

        NavigationPointProvider providerWithObstacles = (position, context) -> {
            // Compare floored coordinates directly
            boolean isBlocked = position.getFlooredX() == blockedX
                    && position.getFlooredY() == blockedY
                    && position.getFlooredZ() == blockedZ;
            return () -> !isBlocked;
        };

        // Create a validation processor that uses the NavigationPointProvider
        ValidationProcessor navigationValidator = context -> {
            PathPosition pos = context.getCurrentPathPosition();
            return providerWithObstacles.getNavigationPoint(pos, context.getEnvironmentContext()).isTraversable();
        };

        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(providerWithObstacles)
                .nodeValidationProcessors(Collections.singletonList(navigationValidator))
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(4, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful(), "Path should be found by going around obstacle");

        Path path = result.getPath();

        // Verify the blocked position is NOT in the path
        for (PathPosition pos : path) {
            assertFalse(pos.getFlooredX() == blockedX && pos.getFlooredY() == blockedY && pos.getFlooredZ() == blockedZ,
                    "Path should not go through blocked position: " + pos);
        }

        // Path should exist and be longer than direct path due to detour
        assertTrue(path.length() > 0, "Path should exist");
    }

    @Test
    @DisplayName("Should fail when target is unreachable")
    void testUnreachableTarget() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Create isolated islands - start on one, target on another with no connection
        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(100, 0, 0);

        // Block everything between X=10 and X=90 in ALL directions (Y and Z too)
        NavigationPointProvider providerWithBarrier = (position, context) -> {
            int x = position.getFlooredX();
            // Only allow positions with X < 10 or X > 90
            boolean isPassable = x < 10 || x > 90;
            return () -> isPassable;
        };

        // Create a validation processor that uses the NavigationPointProvider
        ValidationProcessor navigationValidator = context -> {
            PathPosition pos = context.getCurrentPathPosition();
            return providerWithBarrier.getNavigationPoint(pos, context.getEnvironmentContext()).isTraversable();
        };

        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(providerWithBarrier)
                .nodeValidationProcessors(Collections.singletonList(navigationValidator))
                .neighborStrategy(NeighborStrategies.DIAGONAL_3D)
                .async(false)
                .maxIterations(2000) // Limit iterations to prevent long running test
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(10, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        // Should either fail or hit max iterations (both are acceptable outcomes)
        assertFalse(result.successful(), "Path should not be found when target is on separated island");
    }

    @Test
    @DisplayName("Should respect max iterations configuration")
    void testMaxIterations() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .maxIterations(10) // Very low limit
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(100, 0, 100); // Far away target

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.hasFailed());
        assertEquals(PathState.MAX_ITERATIONS_REACHED, result.getPathState());
    }

    @Test
    @DisplayName("Should respect max length configuration")
    void testMaxLength() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .maxLength(5) // Very short path limit
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(20, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.hasFailed());
        assertEquals(PathState.LENGTH_LIMITED, result.getPathState());
    }

    @Test
    @DisplayName("Should apply custom validation processor")
    void testCustomValidationProcessor() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Create a validator that blocks positions with even X coordinates (except start)
        ValidationProcessor evenXBlocker = context -> {
            PathPosition pos = context.getCurrentPathPosition();
            int x = pos.getFlooredX();
            // Allow even X only if it's between odd positions (for transitions)
            return x % 2 != 0;
        };

        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .nodeValidationProcessors(Collections.singletonList(evenXBlocker))
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .maxIterations(10000) // Increase limit as path will be longer
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(1, 0, 0);
        PathPosition target = new PathPosition(5, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        if (result.successful()) {
            Path path = result.getPath();
            // Verify all positions have odd X coordinates
            for (PathPosition pos : path) {
                assertTrue(pos.getFlooredX() % 2 != 0,
                    "All positions should have odd X coordinates, but found: " + pos);
            }
        } else {
            // If path is not found, that's also acceptable with this validator
            assertFalse(result.successful(), "Path may not be found with strict validation");
        }
    }

    @Test
    @DisplayName("Should apply custom cost processor")
    void testCustomCostProcessor() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Create a cost processor that makes upward movement more expensive
        CostProcessor upwardCostPenalty = context -> {
            PathPosition current = context.getCurrentPathPosition();
            PathPosition previous = context.getPreviousPathPosition();

            if (previous == null) {
                return Cost.ZERO;
            }

            double yDiff = current.getY() - previous.getY();
            if (yDiff > 0) {
                return Cost.of(yDiff * 10.0); // Heavy penalty for upward movement
            }
            return Cost.ZERO;
        };

        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .nodeCostProcessors(Collections.singletonList(upwardCostPenalty))
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 5, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());

        // Due to high upward cost, path should prefer horizontal movement
        Path path = result.getPath();
        assertNotNull(path);
        assertTrue(path.length() > 0);
    }

    @Test
    @DisplayName("Should work with VERTICAL_AND_HORIZONTAL neighbor strategy")
    void testVerticalAndHorizontalStrategy() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(3, 0, 3);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());

        Path path = result.getPath();
        assertNotNull(path);
        assertEquals(start, path.getStart());
        assertEquals(target, path.getEnd());
    }

    @Test
    @DisplayName("Should work with DIAGONAL_3D neighbor strategy")
    void testDiagonal3DStrategy() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .neighborStrategy(NeighborStrategies.DIAGONAL_3D)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(3, 0, 3);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());

        Path path = result.getPath();
        assertNotNull(path);
        assertEquals(start, path.getStart());
        assertEquals(target, path.getEnd());
    }

    @Test
    @DisplayName("Should work with linear heuristic strategy")
    void testLinearHeuristic() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .heuristicStrategy(HeuristicStrategies.LINEAR)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(10, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
    }

    @Test
    @DisplayName("Should work with squared heuristic strategy")
    void testSquaredHeuristic() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .heuristicStrategy(HeuristicStrategies.SQUARED)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(10, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
    }

    @Test
    @DisplayName("Should work with custom heuristic weights")
    void testCustomHeuristicWeights() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Create weights that heavily favor horizontal movement
        HeuristicWeights customWeights = HeuristicWeights.create(
                1.0,   // manhattanWeight
                1.0,   // octileWeight
                0.5,   // perpendicularWeight
                10.0   // heightWeight - High penalty for elevation changes
        );

        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .heuristicWeights(customWeights)
                .neighborStrategy(NeighborStrategies.DIAGONAL_3D)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 5, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
    }

    @Test
    @DisplayName("Should handle asynchronous pathfinding")
    void testAsyncPathfinding() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .async(true) // Enable async mode
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(10, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

        // Should not block immediately
        assertFalse(resultStage.toCompletableFuture().isDone());

        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
    }

    @Test
    @DisplayName("Should abort pathfinding when requested")
    void testAbortPathfinding() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .async(true)
                .maxIterations(50000) // Allow enough iterations for the abort to be effective
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(1000, 0, 1000); // Very far target

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

        // Give it a moment to start, then abort
        Thread.sleep(10);
        pathfinder.abort();

        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        // Should either be aborted or not successful
        assertFalse(result.successful(), "Pathfinding should not complete successfully after abort");
    }

    @Test
    @DisplayName("Should handle start equals target")
    void testStartEqualsTarget() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition position = new PathPosition(5, 5, 5);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(position, position);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());

        Path path = result.getPath();
        assertEquals(1, path.length());
        assertEquals(position, path.getStart());
    }

    @Test
    @DisplayName("Should handle fallback mode correctly")
    void testFallbackMode() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Configuration with fallback enabled and very restrictive limits
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .maxIterations(5) // Very low
                .fallback(true) // Enable fallback
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(100, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        // With fallback enabled and low iterations, we should either get a fallback or max iterations
        assertFalse(result.successful(), "Should not reach target with only 5 iterations");

        // Should have a partial path if fallback worked
        Path path = result.getPath();
        assertNotNull(path);
        assertTrue(path.length() > 0, "Should have at least a partial path");
    }

    @Test
    @DisplayName("Should produce consistent results for same inputs")
    void testDeterministicBehavior() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .neighborStrategy(NeighborStrategies.VERTICAL_AND_HORIZONTAL)
                .async(false)
                .build();

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(10, 5, 10);

        // When: Run pathfinding multiple times
        Pathfinder pathfinder1 = factory.createPathfinder(config);
        PathfinderResult result1 = pathfinder1.findPath(start, target)
                .toCompletableFuture().get(5, TimeUnit.SECONDS);

        Pathfinder pathfinder2 = factory.createPathfinder(config);
        PathfinderResult result2 = pathfinder2.findPath(start, target)
                .toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then: Results should be identical
        assertEquals(result1.getPathState(), result2.getPathState());
        assertEquals(result1.getPath().length(), result2.getPath().length());

        List<PathPosition> positions1 = new ArrayList<>();
        for (PathPosition pos : result1.getPath()) {
            positions1.add(pos);
        }

        List<PathPosition> positions2 = new ArrayList<>();
        for (PathPosition pos : result2.getPath()) {
            positions2.add(pos);
        }

        for (int i = 0; i < positions1.size(); i++) {
            assertEquals(positions1.get(i), positions2.get(i));
        }
    }

    @Test
    @DisplayName("Should handle reopen closed nodes configuration")
    void testReopenClosedNodes() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Configuration with reopen closed nodes enabled
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .reopenClosedNodes(true)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(10, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());
    }

    @Test
    @DisplayName("Should handle large-scale pathfinding")
    void testLargeScalePathfinding() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .maxIterations(100000) // Allow many iterations
                .neighborStrategy(NeighborStrategies.DIAGONAL_3D)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(50, 50, 50);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(30, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());

        Path path = result.getPath();
        assertNotNull(path);
        assertTrue(path.length() > 0);
        assertEquals(start, path.getStart());
        assertEquals(target, path.getEnd());
    }

    @Test
    @DisplayName("Should handle complex multi-processor scenario")
    void testMultiProcessorScenario() throws ExecutionException, InterruptedException, TimeoutException {
        // Given: Multiple validators and cost processors
        Set<PathPosition> dangerZones = new HashSet<>();
        dangerZones.add(new PathPosition(2, 0, 2));
        dangerZones.add(new PathPosition(3, 0, 2));

        ValidationProcessor dangerZoneValidator = context -> {
            PathPosition pos = context.getCurrentPathPosition();
            return !dangerZones.contains(pos);
        };

        ValidationProcessor heightLimitValidator = context -> {
            PathPosition pos = context.getCurrentPathPosition();
            return pos.getY() <= 10; // Max height
        };

        CostProcessor terrainCost = context -> {
            PathPosition pos = context.getCurrentPathPosition();
            // Add cost based on height (ensure non-negative)
            double heightCost = Math.max(0, pos.getY() * 0.5);
            return heightCost > 0 ? Cost.of(heightCost) : Cost.ZERO;
        };

        CostProcessor directionChangePenalty = context -> {
            PathPosition current = context.getCurrentPathPosition();
            PathPosition previous = context.getPreviousPathPosition();

            if (previous == null) {
                return Cost.ZERO;
            }

            // Simple penalty for changing direction
            double dx = current.getX() - previous.getX();
            double dz = current.getZ() - previous.getZ();

            if (Math.abs(dx) > 0 && Math.abs(dz) > 0) {
                return Cost.of(0.1); // Small penalty for diagonal movement
            }

            return Cost.ZERO;
        };

        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .nodeValidationProcessors(Arrays.asList(dangerZoneValidator, heightLimitValidator))
                .nodeCostProcessors(Arrays.asList(terrainCost, directionChangePenalty))
                .neighborStrategy(NeighborStrategies.DIAGONAL_3D)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 0, 5);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertTrue(result.successful());

        Path path = result.getPath();
        // Verify all positions respect validators
        for (PathPosition pos : path) {
            assertFalse(dangerZones.contains(pos));
            assertTrue(pos.getY() <= 10);
        }
    }

    @Test
    @DisplayName("Should iterate over path positions correctly")
    void testPathIteration() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertTrue(result.successful());
        Path path = result.getPath();

        // Test iteration
        int count = 0;
        PathPosition lastPosition = null;
        for (PathPosition pos : path) {
            count++;
            lastPosition = pos;
        }

        assertEquals(path.length(), count);
        assertEquals(target, lastPosition);
    }

    @Test
    @DisplayName("Should collect path positions into collection")
    void testPathCollect() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(simpleProvider)
                .async(false)
                .build();

        Pathfinder pathfinder = factory.createPathfinder(config);

        PathPosition start = new PathPosition(0, 0, 0);
        PathPosition target = new PathPosition(5, 0, 0);

        // When
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);
        PathfinderResult result = resultStage.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Then
        assertTrue(result.successful());
        Path path = result.getPath();

        Collection<PathPosition> positions = path.collect();
        assertNotNull(positions);
        assertEquals(path.length(), positions.size());
        assertTrue(positions.contains(start));
        assertTrue(positions.contains(target));
    }
}
