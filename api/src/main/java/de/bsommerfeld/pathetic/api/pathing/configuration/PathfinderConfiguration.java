package de.bsommerfeld.pathetic.api.pathing.configuration;

import de.bsommerfeld.pathetic.api.pathing.Offset;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeCostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeValidationProcessor;
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
   * Determines whether negative costs are permitted during pathfinding calculations.
   *
   * <p>When set to {@code true}, cost contributions from processors or base transition costs that
   * result in a negative effective cost for a path segment are allowed. This can be useful for
   * scenarios modeling rewards or preferences that reduce the overall path cost. However, allowing
   * negative costs can affect the behavior and guarantees of certain pathfinding algorithms (e.g.,
   * A* might not find the optimal path if negative cycles are introduced or if the heuristic is no
   * longer consistent).
   *
   * <p>When set to {@code false} (or if a mechanism enforces it), the pathfinding engine or
   * pipeline might clamp negative effective transition costs to zero or throw an error, ensuring
   * that path segments do not have a negative cost. This is generally safer for standard A*
   * implementations to guarantee optimality with admissible heuristics.
   *
   * <p>The default behavior if this configuration is not explicitly handled might vary (e.g., a
   * warning for negative costs, as seen in the {@code Cost.of()} method, or clamping within the
   * pipeline). This flag provides a more explicit control point for such behavior.
   */
  private final boolean negativeCostsAllowed;

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
   * <p>Default: {@link HeuristicWeights#NATURAL_PATH_WEIGHTS}
   */
  private final HeuristicWeights heuristicWeights;

  /**
   * A list of {@link NodeValidationProcessor}s to be used in the pathfinding pipeline. May be null
   * or empty if no validators are configured.
   */
  private final List<NodeValidationProcessor> nodeValidationProcessors;

  /**
   * A list of {@link NodeCostProcessor}s to be used in the pathfinding pipeline. May be null or
   * empty if no calculators are configured.
   */
  private final List<NodeCostProcessor> nodeCostProcessors;

  /**
   * Defines the offset configuration to be used in pathfinding logic.
   *
   * <p>This field determines the set of directional offsets applicable for navigation calculations,
   * based on the {@link Offset} enumeration. The offset can define movement in vertical,
   * horizontal, diagonal directions, or a combination of multiple offset types.
   *
   * <p>It plays a critical role in determining the valid movement vectors that can be used when
   * finding possible paths.
   */
  private final Offset offset;

  /**
   * The size of grid cells used in the closed set optimization for pathfinding algorithms.
   * This parameter affects how positions are grouped into regions for efficient lookup.
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
   * The false positive probability of the Bloom filter used in the GridRegionData. A lower FPP means 
   * a smaller chance of incorrectly identifying a position as being in the region, but it also 
   * requires a larger Bloom filter.
   * 
   * <p>Default: 0.01 (1%)
   */
  private final double bloomFilterFpp;

  private PathfinderConfiguration(
      int maxIterations,
      int maxLength,
      boolean async,
      boolean fallback,
      boolean negativeCostsAllowed,
      NavigationPointProvider provider,
      HeuristicWeights heuristicWeights,
      List<NodeValidationProcessor> nodeValidationProcessors,
      List<NodeCostProcessor> nodeCostProcessors,
      Offset offset,
      int gridCellSize,
      int bloomFilterSize,
      double bloomFilterFpp) {
    this.maxIterations = maxIterations;
    this.maxLength = maxLength;
    this.async = async;
    this.fallback = fallback;
    this.negativeCostsAllowed = negativeCostsAllowed;
    this.provider = provider;
    this.heuristicWeights = heuristicWeights;
    this.nodeValidationProcessors = Collections.unmodifiableList(nodeValidationProcessors);
    this.nodeCostProcessors = Collections.unmodifiableList(nodeCostProcessors);
    this.offset = offset;
    this.gridCellSize = gridCellSize;
    this.bloomFilterSize = bloomFilterSize;
    this.bloomFilterFpp = bloomFilterFpp;
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
        .nodeValidationProcessors(pathfinderConfiguration.nodeValidationProcessors)
        .nodeCostProcessors(pathfinderConfiguration.nodeCostProcessors)
        .offset(pathfinderConfiguration.offset)
        .gridCellSize(pathfinderConfiguration.gridCellSize)
        .bloomFilterSize(pathfinderConfiguration.bloomFilterSize)
        .bloomFilterFpp(pathfinderConfiguration.bloomFilterFpp)
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

  public boolean areNegativeCostsAllowed() {
    return this.negativeCostsAllowed;
  }

  public NavigationPointProvider getProvider() {
    return provider;
  }

  public HeuristicWeights getHeuristicWeights() {
    return this.heuristicWeights;
  }

  public List<NodeCostProcessor> getNodeCostProcessors() {
    return nodeCostProcessors;
  }

  public List<NodeValidationProcessor> getNodeValidationProcessors() {
    return nodeValidationProcessors;
  }

  public Offset getOffset() {
    return offset;
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
        + ", negativeCostsAllowed="
        + negativeCostsAllowed
        + ", provider="
        + provider
        + ", heuristicWeights="
        + heuristicWeights
        + ", nodeValidationProcessors="
        + nodeValidationProcessors
        + ", nodeCostProcessors="
        + nodeCostProcessors
        + ", offset="
        + offset
        + ", gridCellSize="
        + gridCellSize
        + ", bloomFilterSize="
        + bloomFilterSize
        + ", bloomFilterFpp="
        + bloomFilterFpp
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    PathfinderConfiguration that = (PathfinderConfiguration) o;
    return maxIterations == that.maxIterations
        && maxLength == that.maxLength
        && async == that.async
        && fallback == that.fallback
        && negativeCostsAllowed == that.negativeCostsAllowed
        && gridCellSize == that.gridCellSize
        && bloomFilterSize == that.bloomFilterSize
        && Double.compare(that.bloomFilterFpp, bloomFilterFpp) == 0
        && Objects.equals(provider, that.provider)
        && Objects.equals(heuristicWeights, that.heuristicWeights)
        && Objects.equals(nodeValidationProcessors, that.nodeValidationProcessors)
        && Objects.equals(nodeCostProcessors, that.nodeCostProcessors)
        && offset == that.offset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maxIterations,
        maxLength,
        async,
        fallback,
        negativeCostsAllowed,
        gridCellSize,
        bloomFilterSize,
        bloomFilterFpp,
        provider,
        heuristicWeights,
        nodeValidationProcessors,
        nodeCostProcessors,
        offset);
  }

  public static class PathfinderConfigurationBuilder {
    private int maxIterations = 5000;
    private int maxLength;
    private boolean async;
    private boolean fallback = true;
    private boolean negativeCostsAllowed = false;
    private NavigationPointProvider provider;
    private HeuristicWeights heuristicWeights = HeuristicWeights.NATURAL_PATH_WEIGHTS;
    private List<NodeValidationProcessor> nodeValidationProcessors = Collections.emptyList();
    private List<NodeCostProcessor> nodeCostProcessors = Collections.emptyList();
    private Offset offset = Offset.VERTICAL_AND_HORIZONTAL;
    private int gridCellSize = 12;
    private int bloomFilterSize = 1000;
    private double bloomFilterFpp = 0.01;

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

    public PathfinderConfiguration.PathfinderConfigurationBuilder negativeCostsAllowed(
        boolean negativeCosts) {
      this.negativeCostsAllowed = negativeCosts;
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
        List<NodeValidationProcessor> nodeValidationProcessors) {
      this.nodeValidationProcessors = Objects.requireNonNull(nodeValidationProcessors);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder nodeCostProcessors(
        List<NodeCostProcessor> nodeCostProcessors) {
      this.nodeCostProcessors = Objects.requireNonNull(nodeCostProcessors);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder offset(Offset offset) {
      this.offset = Objects.requireNonNull(offset);
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder gridCellSize(int gridCellSize) {
      this.gridCellSize = gridCellSize;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder bloomFilterSize(int bloomFilterSize) {
      this.bloomFilterSize = bloomFilterSize;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder bloomFilterFpp(double bloomFilterFpp) {
      this.bloomFilterFpp = bloomFilterFpp;
      return this;
    }

    public PathfinderConfiguration build() {
      return new PathfinderConfiguration(
          this.maxIterations,
          this.maxLength,
          this.async,
          this.fallback,
          this.negativeCostsAllowed,
          this.provider,
          this.heuristicWeights,
          this.nodeValidationProcessors,
          this.nodeCostProcessors,
          this.offset,
          this.gridCellSize,
          this.bloomFilterSize,
          this.bloomFilterFpp);
    }

    public String toString() {
      return "PathfinderConfiguration.PathfinderConfigurationBuilder(maxIterations="
          + this.maxIterations
          + ", maxLength="
          + this.maxLength
          + ", async="
          + this.async
          + ", fallback="
          + this.fallback
          + ", negativeCosts="
          + this.negativeCostsAllowed
          + ", provider="
          + this.provider
          + ", heuristicWeights="
          + this.heuristicWeights
          + ")"
          + ", nodeValidationProcessors="
          + this.nodeValidationProcessors
          + ", nodeCostProcessors="
          + this.nodeCostProcessors
          + ", offset="
          + this.offset
          + ", gridCellSize="
          + this.gridCellSize;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      PathfinderConfigurationBuilder that = (PathfinderConfigurationBuilder) o;
      return maxIterations == that.maxIterations
          && maxLength == that.maxLength
          && async == that.async
          && fallback == that.fallback
          && negativeCostsAllowed == that.negativeCostsAllowed
          && gridCellSize == that.gridCellSize
          && bloomFilterSize == that.bloomFilterSize
          && Double.compare(that.bloomFilterFpp, bloomFilterFpp) == 0
          && Objects.equals(provider, that.provider)
          && Objects.equals(heuristicWeights, that.heuristicWeights)
          && Objects.equals(nodeValidationProcessors, that.nodeValidationProcessors)
          && Objects.equals(nodeCostProcessors, that.nodeCostProcessors)
          && offset == that.offset;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          maxIterations,
          maxLength,
          async,
          fallback,
          negativeCostsAllowed,
          gridCellSize,
          bloomFilterSize,
          bloomFilterFpp,
          provider,
          heuristicWeights,
          nodeValidationProcessors,
          nodeCostProcessors,
          offset);
    }
  }
}
