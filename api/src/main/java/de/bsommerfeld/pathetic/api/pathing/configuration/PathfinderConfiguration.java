package de.bsommerfeld.pathetic.api.pathing.configuration;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.NeighborStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Defines a set of configurable parameters that govern the behavior of the A* pathfinding
 * algorithm. By adjusting these parameters, you can fine-tune the pathfinding process to suit the
 * specific needs of your 3D environment.
 */
public class PathfinderConfiguration {

  /**
   * The maximum number of iterations allowed for the pathfinding algorithm. This acts as a
   * safeguard to prevent infinite loops in complex scenarios.
   *
   * <p>Default: 5000
   */
  private final int maxIterations;

  /**
   * The maximum permissible length of a calculated path (in positions). Use this to constrain long
   * searches that could impact performance. A value of 0 indicates no limit.
   */
  private final int maxLength;

  /**
   * Determines whether pathfinding calculations should be executed asynchronously in a separate
   * thread. This can improve responsiveness in the main thread, but may introduce synchronization
   * complexities.
   */
  private final boolean async;

  /**
   * If pathfinding fails, this parameter determines whether the algorithm should fall back to the
   * last successfully calculated path. This can help maintain progress, but might use an
   * uncompleted path.
   */
  private final boolean fallback;

  /**
   * The provider responsible for supplying navigation points to the pathfinding algorithm. This
   * provider determines how the pathfinder interacts with the environment and accesses information
   * about positions.
   *
   * @see NavigationPointProvider
   */
  private final NavigationPointProvider provider;

  /**
   * The set of weights used to calculate heuristics within the A* algorithm. These influence the
   * pathfinding priority for distance, elevation changes, smoothness, and diagonal movement.
   *
   * <p>Default: {@link HeuristicWeights#DEFAULT_WEIGHTS}
   */
  private final HeuristicWeights heuristicWeights;

  /**
   * A list of {@link ValidationProcessor}s to be used in the pathfinding pipeline. May be null or
   * empty if no validators are configured.
   */
  private final List<ValidationProcessor> validationProcessors;

  /**
   * A list of {@link CostProcessor}s to be used in the pathfinding pipeline. May be null or empty
   * if no calculators are configured.
   */
  private final List<CostProcessor> costProcessors;

  /**
   * The strategy that defines the set of vectors used to find neighbor nodes.
   *
   * <p>This field determines the directional offsets for exploring adjacent nodes from the current
   * node in the pathfinding algorithm. By providing a custom implementation of {@link
   * INeighborStrategy}, you can control movement patterns, such as allowing only cardinal
   * directions, including diagonals, or defining custom, non-uniform offsets.
   *
   * @see INeighborStrategy
   * @see NeighborStrategies
   */
  private final INeighborStrategy neighborStrategy;

  /*
   * The three fields below configured the bloom-filtered grid regions of earlier engines. The
   * bundled engine's closed set is id-indexed and no longer reads them; they are retained so
   * existing configurations keep building and copying unchanged.
   */

  /**
   * The size of grid cells used by the former bloom-filtered closed-set regions.
   *
   * <p>Default: 12
   */
  private final int gridCellSize;

  /**
   * The size of the Bloom filter used by the former bloom-filtered closed-set regions.
   *
   * <p>Default: 1000
   */
  private final int bloomFilterSize;

  /**
   * The false positive probability of the Bloom filter used by the former bloom-filtered
   * closed-set regions.
   *
   * <p>Default: 0.01 (1%)
   */
  private final double bloomFilterFpp;

  /** The strategy used to calculate the heuristic cost (H-cost) for each node. */
  private final IHeuristicStrategy heuristicStrategy;

  /**
   * Controls whether the pathfinder re-evaluates nodes already in the closed set. If enabled, it
   * allows updating the path to a closed node if a lower G-cost is found. Necessary for optimality
   * with non-monotonic heuristics or complex cost functions, but impacts performance.
   *
   * <p>Default: false
   *
   * @since 5.4.1
   */
  private final boolean reopenClosedNodes;

  /**
   * A list of {@link PathfinderHook}s to be called on every step of the pathfinding process. Hooks
   * can be used to modify the pathfinding process or collect data. May be null or empty if no hooks
   * are configured.
   */
  private final List<PathfinderHook> pathfindingHooks;

  /**
   * The executor service used by the pathfinder to schedule and execute pathfinding requests
   * when {@link #isAsync()} is {@code true}. May be {@code null} for sync-only configurations
   * that did not explicitly supply an executor - in that case no thread pool is allocated.
   */
  private final ExecutorService executorService;

  private PathfinderConfiguration(
      int maxIterations,
      int maxLength,
      boolean async,
      boolean fallback,
      NavigationPointProvider provider,
      HeuristicWeights heuristicWeights,
      List<ValidationProcessor> validationProcessors,
      List<CostProcessor> costProcessors,
      INeighborStrategy neighborStrategy,
      int gridCellSize,
      int bloomFilterSize,
      double bloomFilterFpp,
      IHeuristicStrategy heuristicStrategy,
      boolean reopenClosedNodes,
      List<PathfinderHook> pathfindingHooks,
      ExecutorService executorService) {
    this.maxIterations = maxIterations;
    this.maxLength = maxLength;
    this.async = async;
    this.fallback = fallback;
    this.provider = provider;
    this.heuristicWeights = heuristicWeights;
    this.validationProcessors = Collections.unmodifiableList(validationProcessors);
    this.costProcessors = Collections.unmodifiableList(costProcessors);
    this.neighborStrategy = neighborStrategy;
    this.gridCellSize = gridCellSize;
    this.bloomFilterSize = bloomFilterSize;
    this.bloomFilterFpp = bloomFilterFpp;
    this.heuristicStrategy = heuristicStrategy;
    this.reopenClosedNodes = reopenClosedNodes;
    this.pathfindingHooks = Collections.unmodifiableList(pathfindingHooks);
    this.executorService = executorService;
  }

  /**
   * Creates a deep copy of the given {@link PathfinderConfiguration}.
   *
   * <p>The returned instance is independent of the source for every field that can sensibly be
   * cloned:
   *
   * <ul>
   *   <li>Primitives ({@code maxIterations}, {@code maxLength}, {@code gridCellSize},
   *       {@code bloomFilterSize}, {@code bloomFilterFpp}, the boolean flags) are copied by value.
   *   <li>{@link HeuristicWeights} is immutable and shared safely.
   *   <li>The processor lists ({@link #getNodeValidationProcessors()},
   *       {@link #getNodeCostProcessors()}) and {@link #pathfindingHooks()} are copied
   *       element-wise into fresh {@link ArrayList} instances. Subsequent mutations on the
   *       source's underlying lists (e.g. via a reference still held by the caller) do not leak
   *       into the copy.
   * </ul>
   *
   * <p>The following fields hold user-supplied interface implementations or external services and
   * are intentionally shared by reference: {@link NavigationPointProvider}, individual
   * {@link ValidationProcessor} / {@link CostProcessor} / {@link PathfinderHook} instances,
   * {@link INeighborStrategy}, {@link IHeuristicStrategy}, and {@link ExecutorService}. They have
   * no generic clone contract, and a "deep copy" of an executor or a custom traversability
   * provider would either be impossible or semantically wrong.
   *
   * @param pathfinderConfiguration The {@link PathfinderConfiguration} to copy.
   * @return A new {@link PathfinderConfiguration} instance independent of the source's mutable
   *     collection state.
   */
  public static PathfinderConfiguration deepCopy(PathfinderConfiguration pathfinderConfiguration) {
    PathfinderConfigurationBuilder builder =
        builder()
            .maxIterations(pathfinderConfiguration.maxIterations)
            .maxLength(pathfinderConfiguration.maxLength)
            .async(pathfinderConfiguration.async)
            .fallback(pathfinderConfiguration.fallback)
            .provider(pathfinderConfiguration.provider)
            .heuristicWeights(pathfinderConfiguration.heuristicWeights)
            .validationProcessors(new ArrayList<>(pathfinderConfiguration.validationProcessors))
            .costProcessor(new ArrayList<>(pathfinderConfiguration.costProcessors))
            .neighborStrategy(pathfinderConfiguration.neighborStrategy)
            .gridCellSize(pathfinderConfiguration.gridCellSize)
            .bloomFilterSize(pathfinderConfiguration.bloomFilterSize)
            .bloomFilterFpp(pathfinderConfiguration.bloomFilterFpp)
            .heuristicStrategy(pathfinderConfiguration.heuristicStrategy)
            .reopenClosedNodes(pathfinderConfiguration.reopenClosedNodes)
            .pathfindingHooks(new ArrayList<>(pathfinderConfiguration.pathfindingHooks));
    if (pathfinderConfiguration.executorService != null) {
      builder.executorService(pathfinderConfiguration.executorService);
    }
    return builder.build();
  }

  public static PathfinderConfigurationBuilder builder() {
    return new PathfinderConfigurationBuilder();
  }

  public int getMaxIterations() {
    return this.maxIterations;
  }

  public int getMaxLength() {
    return this.maxLength;
  }

  public boolean isAsync() {
    return this.async;
  }

  public boolean isFallback() {
    return this.fallback;
  }

  public NavigationPointProvider getProvider() {
    return provider;
  }

  public HeuristicWeights getHeuristicWeights() {
    return this.heuristicWeights;
  }

  public List<CostProcessor> getNodeCostProcessors() {
    return costProcessors;
  }

  public List<ValidationProcessor> getNodeValidationProcessors() {
    return validationProcessors;
  }

  public INeighborStrategy getNeighborStrategy() {
    return neighborStrategy;
  }

  /**
   * @deprecated Marked for removal. The bundled A* engine no longer buckets its closed set into
   *     bloom-filtered grid regions; per-node state is id-indexed instead. The value is unused by
   *     the bundled engine and will be removed in a future release.
   */
  @Deprecated
  public int getGridCellSize() {
    return gridCellSize;
  }

  /**
   * @deprecated Marked for removal. The bundled A* engine no longer uses bloom filters for its
   *     closed set; per-node state is id-indexed instead. The value is unused by the bundled
   *     engine and will be removed in a future release.
   */
  @Deprecated
  public int getBloomFilterSize() {
    return bloomFilterSize;
  }

  /**
   * @deprecated Marked for removal. The bundled A* engine no longer uses bloom filters for its
   *     closed set; per-node state is id-indexed instead. The value is unused by the bundled
   *     engine and will be removed in a future release.
   */
  @Deprecated
  public double getBloomFilterFpp() {
    return bloomFilterFpp;
  }

  public IHeuristicStrategy getHeuristicStrategy() {
    return heuristicStrategy;
  }

  public boolean shouldReopenClosedNodes() {
    return reopenClosedNodes;
  }

  public List<PathfinderHook> pathfindingHooks() {
    return pathfindingHooks;
  }

  public ExecutorService executorService() {
    return executorService;
  }

  @Override
  public String toString() {
    return "PathfinderConfiguration{"
        + "maxIterations="
        + maxIterations
        + ", maxLength="
        + maxLength
        + ", async="
        + async
        + ", fallback="
        + fallback
        + ", provider="
        + provider
        + ", heuristicWeights="
        + heuristicWeights
        + ", nodeValidationProcessors="
        + validationProcessors
        + ", nodeCostProcessors="
        + costProcessors
        + ", neighborStrategy="
        + neighborStrategy
        + ", gridCellSize="
        + gridCellSize
        + ", bloomFilterSize="
        + bloomFilterSize
        + ", bloomFilterFpp="
        + bloomFilterFpp
        + ", heuristicMode="
        + heuristicStrategy
        + ", reopenClosedNodes="
        + reopenClosedNodes
        + ", pathfindingHooks="
        + pathfindingHooks
        + ", executorService="
        + executorService
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PathfinderConfiguration that = (PathfinderConfiguration) o;
    return maxIterations == that.maxIterations
        && maxLength == that.maxLength
        && async == that.async
        && fallback == that.fallback
        && gridCellSize == that.gridCellSize
        && bloomFilterSize == that.bloomFilterSize
        && Double.compare(that.bloomFilterFpp, bloomFilterFpp) == 0
        && reopenClosedNodes == that.reopenClosedNodes
        && Objects.equals(provider, that.provider)
        && Objects.equals(heuristicWeights, that.heuristicWeights)
        && Objects.equals(validationProcessors, that.validationProcessors)
        && Objects.equals(costProcessors, that.costProcessors)
        && Objects.equals(neighborStrategy, that.neighborStrategy)
        && heuristicStrategy == that.heuristicStrategy
        && Objects.equals(pathfindingHooks, that.pathfindingHooks)
        && Objects.equals(executorService, that.executorService);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maxIterations,
        maxLength,
        async,
        fallback,
        provider,
        heuristicWeights,
        validationProcessors,
        costProcessors,
        neighborStrategy,
        gridCellSize,
        bloomFilterSize,
        bloomFilterFpp,
        heuristicStrategy,
        reopenClosedNodes,
        pathfindingHooks,
        executorService
      );
  }

  public static class PathfinderConfigurationBuilder {
    private int maxIterations = 5000;
    private int maxLength;
    private boolean async;
    private boolean fallback = true;
    private NavigationPointProvider provider = (position, environmentContext) -> () -> true;
    private HeuristicWeights heuristicWeights = HeuristicWeights.DEFAULT_WEIGHTS;
    private List<ValidationProcessor> validationProcessors = Collections.emptyList();
    private List<CostProcessor> costProcessors = Collections.emptyList();
    private INeighborStrategy neighborStrategy = NeighborStrategies.VERTICAL_AND_HORIZONTAL;
    private int gridCellSize = 12;
    private int bloomFilterSize = 1000;
    private double bloomFilterFpp = 0.01;
    private IHeuristicStrategy heuristicStrategy = HeuristicStrategies.LINEAR;
    private boolean reopenClosedNodes = false;
    private List<PathfinderHook> pathfindingHooks = Collections.emptyList();
    /*
     * Stays null until build() resolves it. Sync-only configurations (async=false without a
     * user-supplied executor) keep this null so the shared pool is never instantiated for them.
     */
    private ExecutorService executorService = null;

    PathfinderConfigurationBuilder() {}

    public PathfinderConfiguration.PathfinderConfigurationBuilder maxIterations(int maxIterations) {
      if (maxIterations <= 0) {
        throw new IllegalArgumentException(
            "maxIterations must be > 0, was " + maxIterations);
      }
      this.maxIterations = maxIterations;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder maxLength(int maxLength) {
      if (maxLength < 0) {
        throw new IllegalArgumentException(
            "maxLength must be >= 0 (0 = unlimited), was " + maxLength);
      }
      this.maxLength = maxLength;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder async(boolean async) {
      this.async = async;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder fallback(
        boolean allowingFallback) {
      this.fallback = allowingFallback;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder provider(
        NavigationPointProvider provider) {
      this.provider = Objects.requireNonNull(provider, "provider must not be null");
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder heuristicWeights(
        HeuristicWeights heuristicWeights) {
      this.heuristicWeights =
          Objects.requireNonNull(heuristicWeights, "heuristicWeights must not be null");
      return this;
    }

    /** {@link #validationProcessors(List)} */
    @Deprecated
    public PathfinderConfiguration.PathfinderConfigurationBuilder nodeValidationProcessors(
        List<ValidationProcessor> validationProcessors) {
      this.validationProcessors =
          Objects.requireNonNull(validationProcessors, "validationProcessors must not be null");
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder validationProcessors(
        List<ValidationProcessor> validationProcessors) {
      this.validationProcessors =
          Objects.requireNonNull(validationProcessors, "validationProcessors must not be null");
      return this;
    }

    /** {@link #costProcessor(List)} */
    @Deprecated
    public PathfinderConfiguration.PathfinderConfigurationBuilder nodeCostProcessors(
        List<CostProcessor> costProcessors) {
      this.costProcessors =
          Objects.requireNonNull(costProcessors, "costProcessors must not be null");
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder costProcessor(
        List<CostProcessor> costProcessors) {
      this.costProcessors =
          Objects.requireNonNull(costProcessors, "costProcessors must not be null");
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder neighborStrategy(
        INeighborStrategy neighborStrategy) {
      this.neighborStrategy =
          Objects.requireNonNull(neighborStrategy, "neighborStrategy must not be null");
      return this;
    }

    /**
     * @deprecated Marked for removal. The bundled A* engine no longer buckets its closed set into
     *     bloom-filtered grid regions; the value is accepted but unused and will be removed in a
     *     future release.
     */
    @Deprecated
    public PathfinderConfiguration.PathfinderConfigurationBuilder gridCellSize(int gridCellSize) {
      if (gridCellSize <= 0) {
        throw new IllegalArgumentException(
            "gridCellSize must be > 0, was " + gridCellSize);
      }
      this.gridCellSize = gridCellSize;
      return this;
    }

    /**
     * @deprecated Marked for removal. The bundled A* engine no longer uses bloom filters for its
     *     closed set; the value is accepted but unused and will be removed in a future release.
     */
    @Deprecated
    public PathfinderConfiguration.PathfinderConfigurationBuilder bloomFilterSize(
        int bloomFilterSize) {
      if (bloomFilterSize <= 0) {
        throw new IllegalArgumentException(
            "bloomFilterSize must be > 0, was " + bloomFilterSize);
      }
      this.bloomFilterSize = bloomFilterSize;
      return this;
    }

    /**
     * @deprecated Marked for removal. The bundled A* engine no longer uses bloom filters for its
     *     closed set; the value is accepted but unused and will be removed in a future release.
     */
    @Deprecated
    public PathfinderConfiguration.PathfinderConfigurationBuilder bloomFilterFpp(
        double bloomFilterFpp) {
      if (!(bloomFilterFpp > 0.0 && bloomFilterFpp < 1.0)) {
        throw new IllegalArgumentException(
            "bloomFilterFpp must be in (0.0, 1.0), was " + bloomFilterFpp);
      }
      this.bloomFilterFpp = bloomFilterFpp;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder heuristicStrategy(
        IHeuristicStrategy heuristicStrategy) {
      this.heuristicStrategy =
          Objects.requireNonNull(heuristicStrategy, "heuristicStrategy must not be null");
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder reopenClosedNodes(
        boolean reopenClosedNodes) {
      this.reopenClosedNodes = reopenClosedNodes;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder pathfindingHooks(
        List<PathfinderHook> pathfindingHooks) {
      this.pathfindingHooks =
          Objects.requireNonNull(pathfindingHooks, "pathfindingHooks must not be null");
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder executorService(
        ExecutorService executorService) {
      this.executorService =
          Objects.requireNonNull(executorService, "executorService must not be null");
      return this;
    }

    public PathfinderConfiguration build() {
      ExecutorService resolvedExecutor = this.executorService;
      if (resolvedExecutor == null && this.async) {
        // Lazy-resolve the shared default only when actually needed (async dispatch).
        resolvedExecutor = SharedAsyncPathfinderExecutorService.get();
      }
      return new PathfinderConfiguration(
          this.maxIterations,
          this.maxLength,
          this.async,
          this.fallback,
          this.provider,
          this.heuristicWeights,
          this.validationProcessors,
          this.costProcessors,
          this.neighborStrategy,
          this.gridCellSize,
          this.bloomFilterSize,
          this.bloomFilterFpp,
          this.heuristicStrategy,
          this.reopenClosedNodes,
          this.pathfindingHooks,
          resolvedExecutor);
    }
  }
}
