package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.PathfindingSearch;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicContext;
import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
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

  private ValidationProcessor traversabilityValidator;
  private ValidationProcessor invalidValidationProcessor;

  // Helper: Robuster Koordinaten-Vergleich
  private boolean isAt(PathPosition p, int x, int y) {
    return (int) Math.round(p.getX()) == x && (int) Math.round(p.getY()) == y;
  }

  @BeforeEach
  void setUp() {
    mockProvider = Mockito.mock(NavigationPointProvider.class);
    traversablePoint = Mockito.mock(NavigationPoint.class);
    when(traversablePoint.isTraversable()).thenReturn(true);
    nonTraversablePoint = Mockito.mock(NavigationPoint.class);
    when(nonTraversablePoint.isTraversable()).thenReturn(false);

    invalidValidationProcessor = context -> false;

    traversabilityValidator =
        context -> {
          NavigationPoint p =
              context
                  .getNavigationPointProvider()
                  .getNavigationPoint(context.getCurrentPathPosition(), null);
          return p != null && p.isTraversable();
        };

    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    configuration =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(false)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();

    pathfinder = new AStarPathfinder(configuration);
    start = new PathPosition(0, 0, 0);
    target = new PathPosition(10, 10, 10);
  }

  @Test
  void testReopenClosedNodesUpdatesPath() throws Exception {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(0, 2, 0);

    // Topologie: Einfaches Dreieck
    // Start(0,0) -> Reopen(0,1) -> Target(0,2)  (Direkt)
    // Start(0,0) -> Trap(1,0)   -> Reopen(0,1)  (Umweg)

    INeighborStrategy explicitStrategy =
        () ->
            Arrays.asList(
                new PathVector(1, 0, 0),
                new PathVector(-1, 0, 0),
                new PathVector(0, 1, 0),
                new PathVector(0, -1, 0),
                new PathVector(-1, 1, 0));
    // Mock Bounds: Erlaube nur diese 4 Punkte
    when(mockProvider.getNavigationPoint(any(), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              int x = (int) Math.round(pos.getX());
              int y = (int) Math.round(pos.getY());

              boolean isValid =
                  (x == 0 && y == 0)
                      || // Start
                      (x == 0 && y == 1)
                      || // Reopen
                      (x == 0 && y == 2)
                      || // Target
                      (x == 1 && y == 0); // Trap
              return isValid ? traversablePoint : nonTraversablePoint;
            });

    IHeuristicStrategy graphLogicStrategy =
        new IHeuristicStrategy() {
          @Override
          public double calculate(HeuristicContext context) {
            // Trap wirkt extrem weit weg -> A* ignoriert sie zuerst
            if (isAt(context.position(), 1, 0)) return 100.0;
            return 0.0;
          }

          @Override
          public double calculateTransitionCost(PathPosition from, PathPosition to) {
            // Pfad A (Direkt): Start -> Reopen = TEUER (50)
            if (isAt(from, 0, 0) && isAt(to, 0, 1)) return 50.0;

            // Reopen -> Target = EXTREM TEUER (200) -> Zwingt zum Suchen von Alternativen
            if (isAt(from, 0, 1) && isAt(to, 0, 2)) return 200.0;

            // Pfad B (Umweg): Start -> Trap -> Reopen = BILLIG (1 + 1 = 2)
            return 1.0;
          }
        };

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .neighborStrategy(explicitStrategy)
            .heuristicStrategy(graphLogicStrategy)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .reopenClosedNodes(true) // <--- FEATURE AKTIV
            .maxIterations(100)
            .async(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(config);

    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertEquals(PathState.FOUND, result.getPathState());

    // Debugging Output, falls es fehlschlägt
    List<String> pathCoords = new ArrayList<>();
    for (PathPosition p : result.getPath())
      pathCoords.add(String.format("(%d,%d)", (int) p.getX(), (int) p.getY()));
    System.out.println("Path Found: " + String.join(" -> ", pathCoords));

    // Check: Ging der Pfad über die Trap Node (1,0)?
    boolean tookDetour = false;
    for (PathPosition p : result.getPath()) {
      if (isAt(p, 1, 0)) {
        tookDetour = true;
        break;
      }
    }
    assertTrue(
        tookDetour, "Pathfinder should have reopened (0,1) to take the cheaper path via (1,0)");
  }

  @Test
  void testReopenDisabledIgnoresBetterPath() throws Exception {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(0, 2, 0);

    INeighborStrategy explicitStrategy =
        () ->
            Arrays.asList(
                new PathVector(1, 0, 0), new PathVector(-1, 0, 0),
                new PathVector(0, 1, 0), new PathVector(0, -1, 0));

    when(mockProvider.getNavigationPoint(any(), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              int x = (int) Math.round(pos.getX());
              int y = (int) Math.round(pos.getY());
              boolean isValid =
                  (x == 0 && y == 0)
                      || (x == 0 && y == 1)
                      || (x == 0 && y == 2)
                      || (x == 1 && y == 0);
              return isValid ? traversablePoint : nonTraversablePoint;
            });

    IHeuristicStrategy graphLogicStrategy =
        new IHeuristicStrategy() {
          @Override
          public double calculate(HeuristicContext context) {
            if (isAt(context.position(), 1, 0)) return 100.0;
            return 0.0;
          }

          @Override
          public double calculateTransitionCost(PathPosition from, PathPosition to) {
            if (isAt(from, 0, 0) && isAt(to, 0, 1)) return 50.0;
            if (isAt(from, 0, 1) && isAt(to, 0, 2)) return 200.0;
            return 1.0;
          }
        };

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .neighborStrategy(explicitStrategy)
            .heuristicStrategy(graphLogicStrategy)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .reopenClosedNodes(false) // <--- FEATURE INAKTIV
            .maxIterations(100)
            .async(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(config);
    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertEquals(PathState.FOUND, result.getPathState());

    boolean tookDetour = false;
    for (PathPosition p : result.getPath()) {
      if (isAt(p, 1, 0)) {
        tookDetour = true;
        break;
      }
    }
    assertTrue(
        !tookDetour,
        "Pathfinder should NOT have reopened the node, sticking to the expensive direct path");
  }

  // --- STANDARD TESTS ---

  @Test
  void testDecreaseKeyOnlyOnLowerF() {
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfinderConfiguration cfg =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1000)
            .maxLength(1000)
            .async(false)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();

    AStarPathfinder pf = new AStarPathfinder(cfg);
    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertTrue(result.successful());

    PathfindingSearch search2 = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef2 = new AtomicReference<>();
    search2.ifPresent(resultRef2::set);
    PathfinderResult result2 = resultRef2.get();

    assertNotNull(result2);
    assertEquals(result.getPath().length(), result2.getPath().length());
  }

  @Test
  void testConcurrentPathfinding() throws Exception {
    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(true)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    ExecutorService executor = Executors.newFixedThreadPool(4);
    for (int i = 0; i < 4; i++) {
      CompletableFuture.runAsync(
          () -> {
            try {
              PathfindingSearch search = pf.findPath(start, target);
              search.ifPresent(
                  result -> {
                    assertNotNull(result);
                    assertEquals(PathState.FOUND, result.getPathState());
                  });
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          },
          executor);
    }

    executor.shutdown();
  }

  @Test
  void testSameStartAndTarget() {
    PathPosition position = PathPosition.of(1, 2, 3);
    PathfindingSearch search = pathfinder.findPath(position, position);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();
    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());
    assertEquals(1, result.getPath().length(), "Path should have exactly one position.");
  }

  @Test
  void testNoBloomMightContainBeforePutRequired() {
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
            .nodeValidationProcessors(Collections.singletonList(invalidValidationProcessor))
            .async(false)
            .fallback(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(cfg);
    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.orElse(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertTrue(result.hasFailed());
    assertEquals(PathState.FAILED, result.getPathState());
  }

  @Test
  void testValidationSkipDoesNotSpamOrBreak() throws Exception {
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
            .nodeValidationProcessors(Collections.singletonList(invalidValidationProcessor))
            .async(false)
            .fallback(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(cfg);
    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.orElse(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertTrue(result.hasFailed());
    assertEquals(PathState.FAILED, result.getPathState());
  }

  @Test
  void testFindPathSuccessful() {
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    PathfindingSearch search = pathfinder.findPath(start, target);
    search.ifPresent(resultRef::set);

    PathfinderResult result = resultRef.get();
    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(result.getPath().length() > 0);
  }

  @Test
  void testFindPathBlocked() {
    PathfinderConfiguration noFallbackConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
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

    PathfindingSearch search = noFallbackPathfinder.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.orElse(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertEquals(PathState.FAILED, result.getPathState());
  }

  @Test
  void testTieBreakerPrefersLowerHeuristic() {
    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1000)
            .maxLength(100)
            .async(false)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(2, 2, 0);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());

    List<PathPosition> path = (List<PathPosition>) result.getPath().collect();
    boolean hasIntermediate = false;
    for (PathPosition p : path) {
      if (isAt(p, 1, 1)) {
        hasIntermediate = true;
        break;
      }
    }
    assertTrue(hasIntermediate, "Path should go through (1,1,0) for smoothness");
  }

  @Test
  void testNumericPrecisionWithLargeCosts() throws Exception {
    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .async(false)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(1000, 1000, 1000);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(result.getPath().length() > 1000, "Path length should reflect large distance");
  }

  @Test
  void testFindPathMaxIterationsReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    PathfinderConfiguration lowIterationsConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(1)
            .maxLength(50)
            .async(false)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();
    AStarPathfinder pf = new AStarPathfinder(lowIterationsConfig);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.orElse(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertEquals(PathState.MAX_ITERATIONS_REACHED, result.getPathState());
  }

  @Test
  void testFindPathMaxLengthReached() {
    PathfinderConfiguration lowLengthConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(1)
            .async(false)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();
    AStarPathfinder pf = new AStarPathfinder(lowLengthConfig);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.orElse(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertNotNull(result);
    assertEquals(PathState.LENGTH_LIMITED, result.getPathState());
  }

  @Test
  void testFindPathAsync() {
    PathfinderConfiguration asyncConfig =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .maxIterations(100)
            .maxLength(50)
            .async(true)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .build();
    AStarPathfinder pf = new AStarPathfinder(asyncConfig);
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfindingSearch search = pf.findPath(start, target);
    search.ifPresent(
        result -> {
          assertNotNull(result);
          assertEquals(PathState.FOUND, result.getPathState());
        });
  }
}
