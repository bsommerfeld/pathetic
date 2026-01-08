package de.bsommerfeld.pathetic.api.pathing.configuration;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.NeighborStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

  /**
   * The size of grid cells used in the closed set optimization for pathfinding algorithms. This
   * parameter affects how positions are grouped into regions for efficient lookup.
   *
   * <p>Default: 12
   */
  private final int gridCellSize;

  /**
   * The size of the Bloom filter used in the GridRegionData. A larger size will reduce the false
   * positive probability of the Bloom filter, but will also increase the memory usage.
   *
   * <p>Default: 1000
   */
  private final int bloomFilterSize;

  /**
   * The false positive probability of the Bloom filter used in the GridRegionData. A lower FPP
   * means a smaller chance of incorrectly identifying a position as being in the region, but it
   * also requires a larger Bloom filter.
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
      boolean reopenClosedNodes) {
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
  }

  /**
   * Creates a deep copy of the given {@link PathfinderConfiguration}.
   *
   * <p>This method constructs a new instance of {@link PathfinderConfiguration} with the same
   * values as the input. It ensures a deep copy by copying the values of primitive and boolean
   * fields directly.
   *
   * @param pathfinderConfiguration The {@link PathfinderConfiguration} to copy.
   * @return A new {@link PathfinderConfiguration} instance with the same values as the input.
   */
  public static PathfinderConfiguration deepCopy(PathfinderConfiguration pathfinderConfiguration) {
    return builder()
        .maxIterations(pathfinderConfiguration.maxIterations)
        .maxLength(pathfinderConfiguration.maxLength)
        .async(pathfinderConfiguration.async)
        .fallback(pathfinderConfiguration.fallback)
        .provider(pathfinderConfiguration.provider)
        .heuristicWeights(pathfinderConfiguration.heuristicWeights)
        .nodeValidationProcessors(pathfinderConfiguration.validationProcessors)
        .nodeCostProcessors(pathfinderConfiguration.costProcessors)
        .neighborStrategy(pathfinderConfiguration.neighborStrategy)
        .gridCellSize(pathfinderConfiguration.gridCellSize)
        .bloomFilterSize(pathfinderConfiguration.bloomFilterSize)
        .bloomFilterFpp(pathfinderConfiguration.bloomFilterFpp)
        .heuristicStrategy(pathfinderConfiguration.heuristicStrategy)
        .reopenClosedNodes(pathfinderConfiguration.reopenClosedNodes)
        .build();
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

  public int getGridCellSize() {
    return gridCellSize;
  }

  public int getBloomFilterSize() {
    return bloomFilterSize;
  }

  public double getBloomFilterFpp() {
    return bloomFilterFpp;
  }

  public IHeuristicStrategy getHeuristicStrategy() {
    return heuristicStrategy;
  }

  public boolean shouldReopenClosedNodes() {
    return reopenClosedNodes;
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
        && heuristicStrategy == that.heuristicStrategy;
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
        reopenClosedNodes);
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

    PathfinderConfigurationBuilder() {}

    public PathfinderConfiguration.PathfinderConfigurationBuilder maxIterations(int maxIterations) {
      this.maxIterations = maxIterations;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder maxLength(int maxLength) {
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
      this.provider = Objects.requireNonNull(provider);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder heuristicWeights(
        HeuristicWeights heuristicWeights) {
      this.heuristicWeights = Objects.requireNonNull(heuristicWeights);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder nodeValidationProcessors(
        List<ValidationProcessor> validationProcessors) {
      this.validationProcessors = Objects.requireNonNull(validationProcessors);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder nodeCostProcessors(
        List<CostProcessor> costProcessors) {
      this.costProcessors = Objects.requireNonNull(costProcessors);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder neighborStrategy(
        INeighborStrategy neighborStrategy) {
      this.neighborStrategy = Objects.requireNonNull(neighborStrategy);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder gridCellSize(int gridCellSize) {
      this.gridCellSize = gridCellSize;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder bloomFilterSize(
        int bloomFilterSize) {
      this.bloomFilterSize = bloomFilterSize;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder bloomFilterFpp(
        double bloomFilterFpp) {
      this.bloomFilterFpp = bloomFilterFpp;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder heuristicStrategy(
        IHeuristicStrategy heuristicStrategy) {
      this.heuristicStrategy = Objects.requireNonNull(heuristicStrategy);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder reopenClosedNodes(
        boolean reopenClosedNodes) {
      this.reopenClosedNodes = reopenClosedNodes;
      return this;
    }

    public PathfinderConfiguration build() {
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
          this.reopenClosedNodes);
    }
  }
}
