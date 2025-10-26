package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    configuration =
        PathfinderConfiguration.builder()
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
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfinderConfiguration cfg =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1000)
            .maxLength(1000)
            .async(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(cfg);

    // Act
    PathfinderResult res =
        pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);

    // Assert Baseline
    assertNotNull(res);
    assertTrue(res.successful());

    // Keine direkte Sicht auf Heap-Operationen möglich: Wir verifizieren stattdessen die
    // Invariante,
    // dass Updates ohne echte F-Verbesserung keine Inkonsistenz erzeugen.
    // Dazu triggern wir eine zweite Suche auf identischem Setup: Der deterministische Zustand
    // (Heuristik + Kostenmodell) darf nicht zu anderer Pfadlänge führen.
    PathfinderResult res2 =
        pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);
    assertNotNull(res2);
    assertEquals(res.getPath().length(), res2.getPath().length());
  }

  @Test
  void testConcurrentPathfinding() throws Exception {
    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(true)
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    ExecutorService executor = Executors.newFixedThreadPool(4);
    List<CompletableFuture<PathfinderResult>> futures = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      futures.add(
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              },
              executor));
    }

    for (CompletableFuture<PathfinderResult> future : futures) {
      PathfinderResult result = future.get(3, TimeUnit.SECONDS);
      assertNotNull(result);
      assertEquals(PathState.FOUND, result.getPathState());
    }
    executor.shutdown();
  }

  @Test
  void testSameStartAndTarget() throws Exception {
    PathfinderResult result =
        pathfinder.findPath(start, start).toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());
    assertEquals(1, result.getPath().length(), "Path should have exactly one position.");
  }

  @Test
  void testNoBloomMightContainBeforePutRequired() throws Exception {
    // Arrange: Traversable nur am Start, um frühes Schließen zu erzwingen
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              return pos.equals(start) ? traversablePoint : nonTraversablePoint;
            });

    PathfinderConfiguration cfg =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1)
            .nodeValidationProcessors(Collections.singletonList(invalidNodeValidationProcessor))
            .async(false)
            .fallback(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(cfg);

    // Act
    PathfinderResult res =
        pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);

    // Assert: Pfad sollte fehlschlagen (blockiert) – dabei wird die Closed-Set-Markierung
    // ausgeführt.
    // Der Test stellt sicher, dass kein mightContain vor put nötig war (idempotent),
    // indem wir schlicht die korrekte Terminierung erwarten.
    assertNotNull(res);
    assertTrue(res.hasFailed());
    assertEquals(PathState.FAILED, res.getPathState());
  }

  @Test
  void testValidationSkipDoesNotSpamOrBreak() throws Exception {
    // Arrange: Start traversable, alle anderen non-traversable
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              return pos.equals(start) ? traversablePoint : nonTraversablePoint;
            });

    PathfinderConfiguration cfg =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1)
            .maxLength(50)
            .nodeValidationProcessors(Collections.singletonList(invalidNodeValidationProcessor))
            .async(false)
            .fallback(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(cfg);

    // Act
    PathfinderResult res =
        pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);

    // Assert: sauberer Fehlschlag ohne Exceptions (Validierungen skippen Nachbarn still)
    assertNotNull(res);
    assertTrue(res.hasFailed());
    assertEquals(PathState.FAILED, res.getPathState());
    assertTrue(res.getPath().length() >= 0);
  }

  @Test
  void testFindPathSuccessful() throws ExecutionException, InterruptedException, TimeoutException {
    // Setup mock provider to allow path to be found
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

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

  @Test
  void testFindPathBlocked() throws ExecutionException, InterruptedException, TimeoutException {
    NodeValidationProcessor validationProcessor =
        context ->
            context
                .getNavigationPointProvider()
                .getNavigationPoint(context.getCurrentPathPosition())
                .isTraversable();

    PathfinderConfiguration noFallbackConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .nodeValidationProcessors(Collections.singletonList(validationProcessor))
            .async(false)
            .fallback(false)
            .build();
    AStarPathfinder noFallbackPathfinder = new AStarPathfinder(noFallbackConfig);

    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              return pos.equals(start) ? traversablePoint : nonTraversablePoint;
            });

    PathfinderResult result =
        noFallbackPathfinder.findPath(start, target).toCompletableFuture().get(1, TimeUnit.SECONDS);

    assertNotNull(result);
    assertEquals(PathState.FAILED, result.getPathState());
    assertTrue(result.getPath().length() >= 0);
  }

  @Test
  void testTieBreakerPrefersLowerHeuristic() throws Exception {
    // Setup a 2D grid where multiple paths have equal F-costs
    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1000)
            .maxLength(100)
            .async(false)
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(2, 2, 0);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfinderResult result =
        pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);
    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());

    for (PathPosition position : result.getPath()) {
      System.out.println(position);
    }

    // Verify path prefers straight line (e.g., (0,0) -> (1,1) -> (2,2)) over zig-zag
    List<PathPosition> path = (List<PathPosition>) result.getPath().collect();
    assertTrue(
        path.contains(new PathPosition(1, 1, 0)), "Path should go through (1,1,0) for smoothness");
  }

  @Test
  void testNumericPrecisionWithLargeCosts() throws Exception {
    PathfinderConfiguration config =
        PathfinderConfiguration.builder().provider(mockProvider).async(false).build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(1000, 1000, 1000);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfinderResult result =
        pf.findPath(start, target).toCompletableFuture().get(2, TimeUnit.SECONDS);
    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());

    // Verify path length is reasonable for large coordinates
    assertTrue(result.getPath().length() > 1000, "Path length should reflect large distance");
  }

  @Test
  void testFindPathMaxIterationsReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    // Create configuration with very low max iterations
    PathfinderConfiguration lowIterationsConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1) // Set very low to ensure we hit the limit
            .maxLength(50)
            .async(false)
            .build();

    // Create pathfinder with this configuration
    AStarPathfinder lowIterationsPathfinder = new AStarPathfinder(lowIterationsConfig);

    // Setup mock provider to allow path to be found but make it complex enough to exceed iterations
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    // Call findPath
    CompletionStage<PathfinderResult> resultStage = lowIterationsPathfinder.findPath(start, target);

    // Get result
    PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

    // Verify result
    assertNotNull(result);
    assertEquals(PathState.MAX_ITERATIONS_REACHED, result.getPathState());
  }

  @Test
  void testFindPathMaxLengthReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    // Create configuration with very low max length
    PathfinderConfiguration lowLengthConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(1) // Set very low to ensure we hit the limit
            .async(false)
            .build();

    // Create pathfinder with this configuration
    AStarPathfinder lowLengthPathfinder = new AStarPathfinder(lowLengthConfig);

    // Setup mock provider to allow path to be found
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

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
    PathfinderConfiguration asyncConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(true)
            .build();

    // Create pathfinder with this configuration
    AStarPathfinder asyncPathfinder = new AStarPathfinder(asyncConfig);

    // Setup mock provider to allow path to be found
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    // Call findPath
    CompletionStage<PathfinderResult> resultStage = asyncPathfinder.findPath(start, target);

    // Get result
    PathfinderResult result = resultStage.toCompletableFuture().get(1, TimeUnit.SECONDS);

    // Verify result
    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());
  }
}
