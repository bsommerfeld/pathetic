package de.bsommerfeld.pathetic.api.pathing.configuration;

import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
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
   * <p>Default: {@link HeuristicWeights#NATURAL_PATH_WEIGHTS}
   */
  private final HeuristicWeights heuristicWeights;

  private PathfinderConfiguration(
      int maxIterations,
      int maxLength,
      boolean async,
      boolean fallback,
      NavigationPointProvider provider,
      HeuristicWeights heuristicWeights) {
    this.maxIterations = maxIterations;
    this.maxLength = maxLength;
    this.async = async;
    this.fallback = fallback;
    this.provider = provider;
    this.heuristicWeights = heuristicWeights;
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

  public String toString() {
    return "PathfinderConfiguration(maxIterations="
        + this.getMaxIterations()
        + ", maxLength="
        + this.getMaxLength()
        + ", async="
        + this.isAsync()
        + ", allowingFallback="
        + this.isFallback()
        + ", provider="
        + this.getProvider()
        + ", heuristicWeights="
        + this.getHeuristicWeights()
        + ")";
  }

  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof PathfinderConfiguration)) return false;
    final PathfinderConfiguration other = (PathfinderConfiguration) o;
    if (!other.canEqual(this)) return false;
    if (this.getMaxIterations() != other.getMaxIterations()) return false;
    if (this.getMaxLength() != other.getMaxLength()) return false;
    if (this.isAsync() != other.isAsync()) return false;
    if (this.isFallback() != other.isFallback()) return false;
    if (this.getProvider() == null
        ? other.getProvider() != null
        : !this.getProvider().equals(other.getProvider())) return false;
    final Object this$heuristicWeights = this.getHeuristicWeights();
    final Object other$heuristicWeights = other.getHeuristicWeights();
    return Objects.equals(this$heuristicWeights, other$heuristicWeights);
  }

  protected boolean canEqual(final Object other) {
    return other instanceof PathfinderConfiguration;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + this.getMaxIterations();
    result = result * PRIME + this.getMaxLength();
    result = result * PRIME + (this.isAsync() ? 79 : 97);
    result = result * PRIME + (this.isFallback() ? 79 : 97);
    result = result * PRIME + (this.getProvider() == null ? 43 : this.getProvider().hashCode());
    final Object $heuristicWeights = this.getHeuristicWeights();
    result = result * PRIME + ($heuristicWeights == null ? 43 : $heuristicWeights.hashCode());
    return result;
  }

  public static class PathfinderConfigurationBuilder {
    private int maxIterations = 5000;
    private int maxLength;
    private boolean async;
    private boolean fallback = true;
    private NavigationPointProvider provider;
    private HeuristicWeights heuristicWeights = HeuristicWeights.NATURAL_PATH_WEIGHTS;

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
      this.provider = provider;
      return this;
    }

    public PathfinderConfiguration.PathfinderConfigurationBuilder heuristicWeights(
        HeuristicWeights heuristicWeights) {
      this.heuristicWeights = heuristicWeights;
      return this;
    }

    public PathfinderConfiguration build() {
      if (provider == null) {
        throw new IllegalStateException("NavigationPointProvider cannot be null.");
      }
      return new PathfinderConfiguration(
          this.maxIterations,
          this.maxLength,
          this.async,
          this.fallback,
          this.provider,
          this.heuristicWeights);
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
          + ", provider="
          + this.provider
          + ", heuristicWeights="
          + this.heuristicWeights
          + ")";
    }
  }
}
