package de.bsommerfeld.pathetic.example.filter;

import de.bsommerfeld.pathetic.api.pathing.filter.PathFilter;
import de.bsommerfeld.pathetic.api.pathing.filter.PathValidationContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * A PathFilter that excludes nodes below a specified minimum height from the pathfinding process.
 */
public class MinimumHeightFilter implements PathFilter {

  private final int minHeight;

  /**
   * Constructor to initialize the filter with a minimum height.
   *
   * @param minHeight The minimum height that a node must have to be considered valid.
   */
  public MinimumHeightFilter(int minHeight) {
    this.minHeight = minHeight;
  }

  /**
   * Filters out nodes that are below the specified minimum height.
   *
   * @param context The context of the current pathfinding validation.
   * @return true if the node is above or equal to the minimum height, false otherwise.
   */
  @Override
  public boolean filter(PathValidationContext context) {
    PathPosition position = context.getPosition();
    return position.getFlooredY() >= minHeight;
  }
}
