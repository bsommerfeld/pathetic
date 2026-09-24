package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.NeighborStrategies;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;
import org.junit.jupiter.api.Test;

/*
 * Verifies the path-quality guarantees of the admissible OCTILE and MANHATTAN heuristics end to end:
 * on a planar maze with scattered obstacles, the engine's path cost must equal an independent
 * Dijkstra optimum with the default weights, and stay within the inflation factor when the weight
 * is raised.
 */
class AdmissibleHeuristicPathQualityTest {

  private static final int SIZE = 48;
  private static final double EPS = 1e-9;

  private static final int[][] ROUTES = {
    {1, 1, 46, 46}, {46, 2, 2, 45}, {3, 24, 44, 20}, {24, 1, 22, 46}, {10, 40, 40, 5}
  };

  /*
   * Deterministic obstacle field: a hashed ~22% scatter plus a few long walls with gaps. The
   * cells around every route endpoint are always kept free.
   */
  private static boolean blocked(int x, int z) {
    if (x < 0 || z < 0 || x >= SIZE || z >= SIZE) return true;
    for (int[] route : ROUTES) {
      boolean nearStart = Math.abs(x - route[0]) <= 1 && Math.abs(z - route[1]) <= 1;
      boolean nearTarget = Math.abs(x - route[2]) <= 1 && Math.abs(z - route[3]) <= 1;
      if (nearStart || nearTarget) return false;
    }
    if ((x == 12 || x == 30) && z % 16 != 7) return true;
    if (z == 20 && x > 5 && x < 40 && x % 11 != 3) return true;
    int h = x * 73856093 ^ z * 19349663;
    h ^= h >>> 13;
    h *= 0x5bd1e995;
    h ^= h >>> 15;
    return Math.floorMod(h, 100) < 22;
  }

  private static boolean free(int x, int z) {
    return !blocked(x, z);
  }

  private static AStarPathfinder pathfinder(
      IHeuristicStrategy heuristic, HeuristicWeights weights, INeighborStrategy neighbors) {
    ValidationProcessor world =
        context -> {
          PathPosition p = context.getCurrentPathPosition();
          return p.getFlooredY() == 0 && free(p.getFlooredX(), p.getFlooredZ());
        };
    PathfinderConfiguration configuration =
        PathfinderConfiguration.builder()
            .provider((position, context) -> () -> true)
            .async(false)
            .fallback(false)
            .maxIterations(200_000)
            .neighborStrategy(neighbors)
            .heuristicStrategy(heuristic)
            .heuristicWeights(weights)
            .nodeValidationProcessors(Collections.singletonList(world))
            .build();
    return new AStarPathfinder(configuration);
  }

  private static double pathCost(AStarPathfinder pathfinder, int[] route) {
    PathfinderResult result =
        pathfinder
            .findPath(new PathPosition(route[0], 0, route[1]), new PathPosition(route[2], 0, route[3]))
            .resultBlocking();
    assertEquals(PathState.FOUND, result.getPathState(), "route " + Arrays.toString(route));

    double cost = 0;
    PathPosition previous = null;
    for (PathPosition position : result.getPath()) {
      if (previous != null) {
        double dx = position.getFlooredX() - previous.getFlooredX();
        double dz = position.getFlooredZ() - previous.getFlooredZ();
        cost += Math.sqrt(dx * dx + dz * dz);
      }
      previous = position;
    }
    return cost;
  }

  /* Reference optimum on the same grid; diagonal moves only need the destination cell free. */
  private static double dijkstra(int[] route, boolean diagonal) {
    double[] dist = new double[SIZE * SIZE];
    Arrays.fill(dist, Double.POSITIVE_INFINITY);
    PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
    int source = route[0] * SIZE + route[1];
    int sink = route[2] * SIZE + route[3];
    dist[source] = 0;
    queue.add(new double[] {0, source});
    while (!queue.isEmpty()) {
      double[] entry = queue.poll();
      int cell = (int) entry[1];
      if (entry[0] > dist[cell]) continue;
      if (cell == sink) return entry[0];
      int x = cell / SIZE;
      int z = cell % SIZE;
      for (int ox = -1; ox <= 1; ox++) {
        for (int oz = -1; oz <= 1; oz++) {
          if (ox == 0 && oz == 0) continue;
          if (!diagonal && ox != 0 && oz != 0) continue;
          int nx = x + ox;
          int nz = z + oz;
          if (blocked(nx, nz)) continue;
          double next = entry[0] + ((ox != 0 && oz != 0) ? Math.sqrt(2) : 1.0);
          int neighbor = nx * SIZE + nz;
          if (next < dist[neighbor]) {
            dist[neighbor] = next;
            queue.add(new double[] {next, neighbor});
          }
        }
      }
    }
    throw new IllegalStateException("route not solvable: " + Arrays.toString(route));
  }

  @Test
  void defaultWeightsYieldOptimalPathsWithDiagonalMoves() {
    AStarPathfinder octile =
        pathfinder(
            HeuristicStrategies.OCTILE, HeuristicWeights.DEFAULT_WEIGHTS, NeighborStrategies.DIAGONAL_3D);
    for (int[] route : ROUTES) {
      assertEquals(dijkstra(route, true), pathCost(octile, route), EPS, Arrays.toString(route));
    }
  }

  @Test
  void manhattanYieldsOptimalPathsWithAxisMoves() {
    AStarPathfinder manhattan =
        pathfinder(
            HeuristicStrategies.MANHATTAN,
            HeuristicWeights.DEFAULT_WEIGHTS,
            NeighborStrategies.VERTICAL_AND_HORIZONTAL);
    for (int[] route : ROUTES) {
      assertEquals(
          dijkstra(route, false), pathCost(manhattan, route), EPS, Arrays.toString(route));
    }
  }

  @Test
  void raisedOctileWeightStaysWithinItsSuboptimalityBound() {
    double w = 1.5;
    AStarPathfinder weighted =
        pathfinder(
            HeuristicStrategies.OCTILE,
            HeuristicWeights.create(0, w, 0, 0),
            NeighborStrategies.DIAGONAL_3D);
    for (int[] route : ROUTES) {
      double optimum = dijkstra(route, true);
      double cost = pathCost(weighted, route);
      assertTrue(
          cost <= w * optimum + EPS,
          Arrays.toString(route) + ": cost " + cost + " exceeds " + w + " x " + optimum);
    }
  }

  @Test
  void neverLongerThanTheLinearDefault() {
    AStarPathfinder octile =
        pathfinder(
            HeuristicStrategies.OCTILE, HeuristicWeights.DEFAULT_WEIGHTS, NeighborStrategies.DIAGONAL_3D);
    AStarPathfinder linear =
        pathfinder(
            HeuristicStrategies.LINEAR, HeuristicWeights.DEFAULT_WEIGHTS, NeighborStrategies.DIAGONAL_3D);
    double octileTotal = 0;
    double linearTotal = 0;
    for (int[] route : ROUTES) {
      double octileCost = pathCost(octile, route);
      double linearCost = pathCost(linear, route);
      assertTrue(octileCost <= linearCost + EPS, Arrays.toString(route));
      octileTotal += octileCost;
      linearTotal += linearCost;
    }
    assertTrue(
        octileTotal < linearTotal,
        "the maze should make the greedy LINEAR default detour at least once");
  }
}
