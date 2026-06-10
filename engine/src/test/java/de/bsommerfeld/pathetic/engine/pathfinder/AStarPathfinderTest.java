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
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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

    // Debug output in case the assertion fails
    List<String> pathCoords = new ArrayList<>();
    for (PathPosition p : result.getPath())
      pathCoords.add(String.format("(%d,%d)", (int) p.getX(), (int) p.getY()));
    System.out.println("Path Found: " + String.join(" -> ", pathCoords));

    // Check: did the path go through the trap node (1,0)?
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

  /*
   * Locks in that cost processors are evaluated at most once per EvaluationContext. The reopen
   * path computes a G-cost to justify reopening and must reuse exactly that value when the node
   * is processed as new; a second evaluation of the same context could diverge for stateful
   * processors and store a G-cost inconsistent with the reopen decision.
   */
  @Test
  void testReopenEvaluatesCostProcessorsOncePerContext() {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(0, 2, 0);

    INeighborStrategy explicitStrategy =
        () ->
            Arrays.asList(
                new PathVector(1, 0, 0),
                new PathVector(-1, 0, 0),
                new PathVector(0, 1, 0),
                new PathVector(0, -1, 0),
                new PathVector(-1, 1, 0));

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

    Set<EvaluationContext> seenContexts = Collections.newSetFromMap(new IdentityHashMap<>());
    AtomicInteger duplicateEvaluations = new AtomicInteger();
    CostProcessor countingProcessor =
        context -> {
          if (!seenContexts.add(context)) {
            duplicateEvaluations.incrementAndGet();
          }
          return Cost.ZERO;
        };

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .neighborStrategy(explicitStrategy)
            .heuristicStrategy(graphLogicStrategy)
            .nodeValidationProcessors(Collections.singletonList(traversabilityValidator))
            .costProcessor(Collections.singletonList(countingProcessor))
            .reopenClosedNodes(true)
            .maxIterations(100)
            .async(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(config);
    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(
        pathContains(result, 1, 0),
        "Sanity check failed: scenario must actually trigger a reopen");
    assertEquals(
        0,
        duplicateEvaluations.get(),
        "Cost processors must be evaluated at most once per EvaluationContext");
  }

  /*
   * Locks in that a reopen attempt vetoed by a validator does not lower the recorded closed-set
   * G-cost. Topology: N closes via A with G=10; B then offers G=5 but the B->N transition is
   * vetoed; C later offers G=7, which must still be able to reopen N against the original 10.
   *
   *      B(1,1) --x--+
   *       |           \
   *      S(0,0)-A(1,0)-N(2,0)-T(3,0)
   *       |           /
   *      C(1,-1)-----+
   */
  @Test
  void testVetoedReopenDoesNotLowerRecordedGCost() {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition target = new PathPosition(3, 0, 0);

    INeighborStrategy forwardStrategy =
        () ->
            Arrays.asList(
                new PathVector(1, 0, 0), new PathVector(1, 1, 0), new PathVector(1, -1, 0));

    when(mockProvider.getNavigationPoint(any(), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              int x = (int) Math.round(pos.getX());
              int y = (int) Math.round(pos.getY());
              boolean isValid =
                  (x == 0 && y == 0) // S
                      || (x == 1 && y == 0) // A
                      || (x == 1 && y == 1) // B
                      || (x == 1 && y == -1) // C
                      || (x == 2 && y == 0) // N
                      || (x == 3 && y == 0); // T
              return isValid ? traversablePoint : nonTraversablePoint;
            });

    /*
     * Heuristic values pin the expansion order S, A, N (closed via A, G=10), B (vetoed offer),
     * C (valid offer), reopened N, T. T's heuristic keeps it behind B and C until the cheaper
     * route through the reopened N updates it.
     */
    IHeuristicStrategy orderPinningStrategy =
        new IHeuristicStrategy() {
          @Override
          public double calculate(HeuristicContext context) {
            PathPosition p = context.position();
            if (isAt(p, 0, 0)) return 5.0; // S
            if (isAt(p, 1, 0)) return 0.0; // A
            if (isAt(p, 1, 1)) return 11.0; // B
            if (isAt(p, 1, -1)) return 13.0; // C
            if (isAt(p, 2, 0)) return 0.0; // N
            if (isAt(p, 3, 0)) return 4.0; // T
            return 999.0;
          }

          @Override
          public double calculateTransitionCost(PathPosition from, PathPosition to) {
            if (isAt(from, 1, 0) && isAt(to, 2, 0)) return 9.0; // A->N: G(N)=10
            if (isAt(from, 1, 1) && isAt(to, 2, 0)) return 4.0; // B->N: offer G=5 (vetoed)
            if (isAt(from, 1, -1) && isAt(to, 2, 0)) return 6.0; // C->N: offer G=7
            return 1.0;
          }
        };

    ValidationProcessor vetoBToN =
        context -> {
          PathPosition from = context.getPreviousPathPosition();
          PathPosition to = context.getCurrentPathPosition();
          if (from != null && isAt(from, 1, 1) && isAt(to, 2, 0)) {
            return false;
          }
          return true;
        };

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .neighborStrategy(forwardStrategy)
            .heuristicStrategy(orderPinningStrategy)
            .nodeValidationProcessors(Arrays.asList(traversabilityValidator, vetoBToN))
            .reopenClosedNodes(true)
            .maxIterations(100)
            .async(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(config);
    PathfindingSearch search = pf.findPath(start, target);
    AtomicReference<PathfinderResult> resultRef = new AtomicReference<>();
    search.ifPresent(resultRef::set);
    PathfinderResult result = resultRef.get();

    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(
        pathContains(result, 1, -1),
        "C's valid offer (G=7) must still reopen N; the vetoed offer (G=5) must not be recorded");
    assertTrue(
        !pathContains(result, 1, 0),
        "Path must not stay on the expensive A route once N is reopened via C");
  }

  // --- UPDATE-EXISTING-NODE TESTS ---

  /*
   * Shared topology for the three tests below:
   *
   *      B(0,1) ----+
   *       |          \
   *      S(0,0)       M(1,1) --- T(2,1)
   *       |          /
   *      A(1,0) ----+
   *
   * Heuristic values are pinned so that F(A) < F(B) < F(M-via-A). Order of
   * expansion is therefore S, then A, then B, then M. M enters the open set
   * first via A; B's expansion then meets an already-open M and triggers
   * updateExistingNode.
   */

  private void setupUpdateExistingNodeProvider() {
    when(mockProvider.getNavigationPoint(any(), any()))
        .thenAnswer(
            inv -> {
              PathPosition pos = inv.getArgument(0);
              int x = (int) Math.round(pos.getX());
              int y = (int) Math.round(pos.getY());
              boolean reachable =
                  (x == 0 && y == 0) // S
                      || (x == 1 && y == 0) // A
                      || (x == 0 && y == 1) // B
                      || (x == 1 && y == 1) // M
                      || (x == 2 && y == 1); // T
              return reachable ? traversablePoint : nonTraversablePoint;
            });
  }

  private INeighborStrategy cardinalNeighbors() {
    return () ->
        Arrays.asList(
            new PathVector(1, 0, 0),
            new PathVector(-1, 0, 0),
            new PathVector(0, 1, 0),
            new PathVector(0, -1, 0));
  }

  private IHeuristicStrategy updateExistingNodeStrategy(
      double aToMCost, double bToMCost) {
    return new IHeuristicStrategy() {
      @Override
      public double calculate(HeuristicContext context) {
        PathPosition p = context.position();
        if (isAt(p, 0, 0)) return 5.0; // S
        if (isAt(p, 1, 0)) return 1.0; // A — lowest h so popped first
        if (isAt(p, 0, 1)) return 2.0; // B — popped before M
        if (isAt(p, 1, 1)) return 2.0; // M — stays above F(B) until updated
        if (isAt(p, 2, 1)) return 0.0; // T
        return 999.0;
      }

      @Override
      public double calculateTransitionCost(PathPosition from, PathPosition to) {
        if (isAt(from, 1, 0) && isAt(to, 1, 1)) return aToMCost;
        if (isAt(from, 0, 1) && isAt(to, 1, 1)) return bToMCost;
        return 1.0;
      }
    };
  }

  private PathfinderResult runUpdateExistingNodeSearch(
      IHeuristicStrategy strategy, List<ValidationProcessor> validators) {
    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .neighborStrategy(cardinalNeighbors())
            .heuristicStrategy(strategy)
            .nodeValidationProcessors(validators)
            .reopenClosedNodes(false)
            .maxIterations(100)
            .async(false)
            .build();

    AStarPathfinder pf = new AStarPathfinder(config);
    PathfindingSearch search = pf.findPath(new PathPosition(0, 0, 0), new PathPosition(2, 1, 0));
    AtomicReference<PathfinderResult> ref = new AtomicReference<>();
    search.ifPresent(ref::set);
    return ref.get();
  }

  private boolean pathContains(PathfinderResult result, int x, int y) {
    for (PathPosition p : result.getPath()) {
      if (isAt(p, x, y)) return true;
    }
    return false;
  }

  /*
   * Covers the normal decrease-key branch of updateExistingNode: when a cheaper
   * predecessor is found, parent and gCost are updated and the heap reorders.
   * M enters via A with G=6, B's expansion then finds G=1.5 -> update wins.
   */
  @Test
  void testUpdateExistingNodeAdoptsCheaperPredecessor() {
    setupUpdateExistingNodeProvider();

    PathfinderResult result =
        runUpdateExistingNodeSearch(
            updateExistingNodeStrategy(5.0, 0.5),
            Collections.singletonList(traversabilityValidator));

    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(pathContains(result, 0, 1), "Path should route through B (cheaper predecessor)");
    assertTrue(!pathContains(result, 1, 0), "Path should not route through A");
  }

  /*
   * Covers the G-guard: when the second discovered path to M is not strictly
   * better, the existing predecessor is kept. M enters via A with G=1.5,
   * B's expansion offers G=6 -> guard rejects the swap.
   */
  @Test
  void testUpdateExistingNodeKeepsExistingWhenNotBetter() {
    setupUpdateExistingNodeProvider();

    PathfinderResult result =
        runUpdateExistingNodeSearch(
            updateExistingNodeStrategy(0.5, 5.0),
            Collections.singletonList(traversabilityValidator));

    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(pathContains(result, 1, 0), "Path should stay on A (already cheaper)");
    assertTrue(!pathContains(result, 0, 1), "Path should not adopt the worse B route");
  }

  /*
   * Covers the validator veto: even when G via B would be better, a validator
   * rejecting the B->M transition prevents the update. The original A route
   * stays in place.
   */
  @Test
  void testUpdateExistingNodeRespectsValidatorVeto() {
    setupUpdateExistingNodeProvider();

    ValidationProcessor vetoBToM =
        context -> {
          PathPosition from = context.getPreviousPathPosition();
          PathPosition to = context.getCurrentPathPosition();
          if (from != null && isAt(from, 0, 1) && isAt(to, 1, 1)) {
            return false;
          }
          return true;
        };

    PathfinderResult result =
        runUpdateExistingNodeSearch(
            updateExistingNodeStrategy(5.0, 0.5),
            Arrays.asList(traversabilityValidator, vetoBToM));

    assertEquals(PathState.FOUND, result.getPathState());
    assertTrue(pathContains(result, 1, 0), "Path must stay on A because B->M is vetoed");
    assertTrue(!pathContains(result, 0, 1), "Path must not adopt the vetoed B route");
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

  // --- ERROR-PATH TESTS ---

  private IHeuristicStrategy nanHeuristicStrategy() {
    return new IHeuristicStrategy() {
      @Override
      public double calculate(HeuristicContext context) {
        return Double.NaN;
      }

      @Override
      public double calculateTransitionCost(PathPosition from, PathPosition to) {
        return 1.0;
      }
    };
  }

  /*
   * A custom heuristic returning NaN must surface as a FAILED result (with the cause on stderr),
   * not as a raw exception escaping through the future - async callers observing only the result
   * path would otherwise never learn the search died.
   */
  @Test
  void testNaNHeuristicFailsSearchInsteadOfThrowing() {
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .heuristicStrategy(nanHeuristicStrategy())
            .maxIterations(100)
            .async(false)
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PrintStream originalErr = System.err;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
    PathfinderResult result;
    try {
      result = pf.findPath(start, target).resultBlocking();
    } finally {
      System.setErr(originalErr);
    }

    assertNotNull(result);
    assertEquals(PathState.FAILED, result.getPathState());
    String captured = buffer.toString(StandardCharsets.UTF_8);
    assertTrue(
        captured.contains("Non-finite F-cost"),
        "stderr should name the non-finite F-cost as the cause - got: " + captured);
  }

  @Test
  void testNaNHeuristicFailsAsyncSearchViaResultPath() {
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .heuristicStrategy(nanHeuristicStrategy())
            .maxIterations(100)
            .async(true)
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    PathfinderResult result;
    try {
      result = pf.findPath(start, target).resultBlocking();
    } finally {
      System.setErr(originalErr);
    }

    assertNotNull(result);
    assertEquals(PathState.FAILED, result.getPathState());
  }

  /*
   * A throwing custom extension (here: a validator) must likewise surface as FAILED instead of
   * escaping through the future.
   */
  @Test
  void testThrowingValidatorFailsSearchInsteadOfThrowing() {
    when(mockProvider.getNavigationPoint(any(PathPosition.class), any()))
        .thenReturn(traversablePoint);

    ValidationProcessor throwingValidator =
        context -> {
          throw new IllegalStateException("boom-validator");
        };

    PathfinderConfiguration config =
        PathfinderConfiguration.builder()
            .provider(mockProvider)
            .validationProcessors(Collections.singletonList(throwingValidator))
            .maxIterations(100)
            .async(false)
            .build();
    AStarPathfinder pf = new AStarPathfinder(config);

    PrintStream originalErr = System.err;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
    PathfinderResult result;
    try {
      result = pf.findPath(start, target).resultBlocking();
    } finally {
      System.setErr(originalErr);
    }

    assertNotNull(result);
    assertEquals(PathState.FAILED, result.getPathState());
    assertTrue(
        buffer.toString(StandardCharsets.UTF_8).contains("boom-validator"),
        "stderr should contain the original exception message");
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
