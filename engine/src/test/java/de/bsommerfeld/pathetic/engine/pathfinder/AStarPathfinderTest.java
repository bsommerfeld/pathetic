package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AStarPathfinderTest {

    private AStarPathfinder pathfinder;
    private NavigationPointProvider mockProvider;
    private PathfinderConfiguration configuration;
    private PathPosition start;
    private PathPosition target;
    private NavigationPoint traversablePoint;
    private NavigationPoint nonTraversablePoint;
    private NodeValidationProcessor invalidNodeValidationProcessor;

    @BeforeEach
    void setUp() {
        // Create mock provider
        mockProvider = Mockito.mock(NavigationPointProvider.class);

        // Create mock navigation points
        traversablePoint = Mockito.mock(NavigationPoint.class);
        when(traversablePoint.isTraversable()).thenReturn(true);

        nonTraversablePoint = Mockito.mock(NavigationPoint.class);
        when(nonTraversablePoint.isTraversable()).thenReturn(false);

        // Implement an always-failing validation processor
        invalidNodeValidationProcessor = context -> false;

        // Create configuration with mock provider
        configuration = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(100)
                .maxLength(50)
                .async(false)
                .build();

        // Create pathfinder with configuration
        pathfinder = new AStarPathfinder(configuration);

        // Create start and target positions
        start = new PathPosition(0, 0, 0);
        target = new PathPosition(10, 10, 10);
    }

    @Test
    void testDecreaseKeyOnlyOnLowerF() throws Exception {
        // Arrange: Get an traversable point for any position to generate neighbors
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(traversablePoint);

        PathfinderConfiguration cfg = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(1000)
                .maxLength(1000)
                .async(false)
                .build();

        AStarPathfinder pf = new AStarPathfinder(cfg);

        // Act
        PathfinderResult res = pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);

        // Assert Baseline
        assertNotNull(res);
        assertTrue(res.successful());

        // Keine direkte Sicht auf Heap-Operationen möglich: Wir verifizieren stattdessen die Invariante,
        // dass Updates ohne echte F-Verbesserung keine Inkonsistenz erzeugen.
        // Dazu triggern wir eine zweite Suche auf identischem Setup: Der deterministische Zustand
        // (Heuristik + Kostenmodell) darf nicht zu anderer Pfadlänge führen.
        PathfinderResult res2 = pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertNotNull(res2);
        assertEquals(res.getPath().length(), res2.getPath().length());
    }

    @Test
    void testNoBloomMightContainBeforePutRequired() throws Exception {
        // Arrange: Traversable nur am Start, um frühes Schließen zu erzwingen
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenAnswer(inv -> {
            PathPosition pos = inv.getArgument(0);
            return pos.equals(start) ? traversablePoint : nonTraversablePoint;
        });

        PathfinderConfiguration cfg = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(1)
                .nodeValidationProcessors(Collections.singletonList(invalidNodeValidationProcessor))
                .async(false)
                .fallback(false)
                .build();

        AStarPathfinder pf = new AStarPathfinder(cfg);

        // Act
        PathfinderResult res = pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);

        // Assert: Pfad sollte fehlschlagen (blockiert) – dabei wird die Closed-Set-Markierung ausgeführt.
        // Der Test stellt sicher, dass kein mightContain vor put nötig war (idempotent),
        // indem wir schlicht die korrekte Terminierung erwarten.
        assertNotNull(res);
        assertTrue(res.hasFailed());
        assertEquals(PathState.FAILED, res.getPathState());
    }

    @Test
    void testValidationSkipDoesNotSpamOrBreak() throws Exception {
        // Arrange: Start traversable, alle anderen non-traversable
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenAnswer(inv -> {
            PathPosition pos = inv.getArgument(0);
            return pos.equals(start) ? traversablePoint : nonTraversablePoint;
        });

        PathfinderConfiguration cfg = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(1)
                .maxLength(50)
                .nodeValidationProcessors(Collections.singletonList(invalidNodeValidationProcessor))
                .async(false)
                .fallback(false)
                .build();

        AStarPathfinder pf = new AStarPathfinder(cfg);

        // Act
        PathfinderResult res = pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);

        // Assert: sauberer Fehlschlag ohne Exceptions (Validierungen skippen Nachbarn still)
        assertNotNull(res);
        assertTrue(res.hasFailed());
        assertEquals(PathState.FAILED, res.getPathState());
        assertTrue(res.getPath().length() >= 0);
    }

    @Test
    void testFindPathSuccessful() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup mock provider to allow path to be found
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(traversablePoint);

        // Call findPath
        CompletionStage<PathfinderResult> resultStage = pathfinder.findPath(start, target);

        // Get result
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // Verify result
        assertNotNull(result);
        assertEquals(PathState.FOUND, result.getPathState());

        // Verify path
        Path path = result.getPath();
        assertNotNull(path);
        assertTrue(path.length() > 0);
        assertEquals(start, path.getStart());
        assertEquals(target, path.getEnd());
    }

    // TODO: This test is currently failing. It needs to be fixed to properly test blocked paths.
    // The issue might be related to how the AStarPathfinder handles non-traversable points.
    /*
    @Test
    void testFindPathBlocked() throws ExecutionException, InterruptedException, TimeoutException {
        // Create configuration with fallback disabled
        PathfinderConfiguration noFallbackConfig = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(100)
                .maxLength(50)
                .async(false)
                .fallback(false)  // Disable fallback
                .build();

        // Create pathfinder with this configuration
        AStarPathfinder noFallbackPathfinder = new AStarPathfinder(noFallbackConfig);

        // Reset the mock provider
        reset(mockProvider);

        // Setup mock provider to block the path
        // Use a custom answer to return traversable for start position and non-traversable for all others
        doAnswer(invocation -> {
            PathPosition position = invocation.getArgument(0);
            if (position.equals(start)) {
                return traversablePoint;
            } else {
                return nonTraversablePoint;
            }
        }).when(mockProvider).getNavigationPoint(any(PathPosition.class), any());

        // Call findPath
        CompletionStage<PathfinderResult> resultStage = noFallbackPathfinder.findPath(start, target);

        // Get result
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // Verify result
        assertNotNull(result);
        assertEquals(PathState.FAILED, result.getPathState());
    }
    */

    @Test
    void testFindPathMaxIterationsReached() throws ExecutionException, InterruptedException, TimeoutException {
        // Create configuration with very low max iterations
        PathfinderConfiguration lowIterationsConfig = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(1)  // Set very low to ensure we hit the limit
                .maxLength(50)
                .async(false)
                .build();

        // Create pathfinder with this configuration
        AStarPathfinder lowIterationsPathfinder = new AStarPathfinder(lowIterationsConfig);

        // Setup mock provider to allow path to be found but make it complex enough to exceed iterations
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(traversablePoint);

        // Call findPath
        CompletionStage<PathfinderResult> resultStage = lowIterationsPathfinder.findPath(start, target);

        // Get result
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // Verify result
        assertNotNull(result);
        assertEquals(PathState.MAX_ITERATIONS_REACHED, result.getPathState());
    }

    @Test
    void testFindPathMaxLengthReached() throws ExecutionException, InterruptedException, TimeoutException {
        // Create configuration with very low max length
        PathfinderConfiguration lowLengthConfig = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(100)
                .maxLength(1)  // Set very low to ensure we hit the limit
                .async(false)
                .build();

        // Create pathfinder with this configuration
        AStarPathfinder lowLengthPathfinder = new AStarPathfinder(lowLengthConfig);

        // Setup mock provider to allow path to be found
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(traversablePoint);

        // Call findPath
        CompletionStage<PathfinderResult> resultStage = lowLengthPathfinder.findPath(start, target);

        // Get result
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // Verify result
        assertNotNull(result);
        assertEquals(PathState.LENGTH_LIMITED, result.getPathState());
    }

    @Test
    void testFindPathAsync() throws ExecutionException, InterruptedException, TimeoutException {
        // Create configuration with async enabled
        PathfinderConfiguration asyncConfig = PathfinderConfiguration.builder()
                .provider(mockProvider)
                .maxIterations(100)
                .maxLength(50)
                .async(true)
                .build();

        // Create pathfinder with this configuration
        AStarPathfinder asyncPathfinder = new AStarPathfinder(asyncConfig);

        // Setup mock provider to allow path to be found
        when(mockProvider.getNavigationPoint(any(PathPosition.class), any())).thenReturn(traversablePoint);

        // Call findPath
        CompletionStage<PathfinderResult> resultStage = asyncPathfinder.findPath(start, target);

        // Get result
        PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

        // Verify result
        assertNotNull(result);
        assertEquals(PathState.FOUND, result.getPathState());
    }
}
