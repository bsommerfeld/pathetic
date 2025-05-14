package de.bsommerfeld.pathetic.api.pathing.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A stage for processing a collection of {@link PathFilter} instances. This class facilitates the
 * sequential application of multiple filters to a given pathfinding context.
 */
public final class PathFilterStage {

  private final Set<PathFilter> filters = new HashSet<>();

  /**
   * Constructs a {@code PathFilterStage} with an initial set of filters.
   *
   * @param pathFilter An array of {@link PathFilter} instances to include in the stage.
   */
  public PathFilterStage(PathFilter... pathFilter) {
    filters.addAll(Arrays.asList(pathFilter));
  }

  /**
   * Applies all filters within this stage to the provided {@link PathValidationContext}. Each
   * filter in the stage is executed sequentially.
   *
   * @param context The {@link PathValidationContext} to be filtered.
   * @return {@code true} if the context passes all filters in the stage, {@code false} otherwise.
   */
  public boolean filter(PathValidationContext context) {
    return filters.stream().allMatch(filter -> filter.filter(context));
  }

  /**
   * Performs cleanup operations on all filters within the stage.
   *
   * @see PathFilter#cleanup()
   */
  public void cleanup() {
    filters.forEach(PathFilter::cleanup);
  }

  /**
   * Returns an immutable set of the filters currently in this stage.
   *
   * @return The set of {@link PathFilter} instances in this stage.
   */
  public Set<PathFilter> getFilters() {
    return Collections.unmodifiableSet(filters);
  }

  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof PathFilterStage)) return false;
    final PathFilterStage other = (PathFilterStage) o;
    final Object this$filters = this.getFilters();
    final Object other$filters = other.getFilters();
    return Objects.equals(this$filters, other$filters);
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object $filters = this.getFilters();
    result = result * PRIME + ($filters == null ? 43 : $filters.hashCode());
    return result;
  }

  public String toString() {
    return "PathFilterStage(filters=" + this.getFilters() + ")";
  }
}
