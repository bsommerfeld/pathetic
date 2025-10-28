package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.Objects;

/**
 * Immutable container for the three core positions in a pathfinding operation.
 *
 * <p>Holds the start position, the current position being evaluated, and the target position. Used
 * to pass these positions together to pathfinding components.
 *
 * @since 5.3.3
 */
public class PathfindingProgress {

  private final PathPosition start;
  private final PathPosition current;
  private final PathPosition target;

  public PathfindingProgress(
      PathPosition startPosition, PathPosition currentPosition, PathPosition targetPosition) {
    this.start = startPosition;
    this.current = currentPosition;
    this.target = targetPosition;
  }

  /**
   * Returns the start position of the overall pathfinding process.
   *
   * @return the start position
   */
  public PathPosition startPosition() {
    return start;
  }

  /**
   * Returns the current step (position) of the pathfinding process.
   *
   * @return the current position
   */
  public PathPosition currentPosition() {
    return current;
  }

  /**
   * Returns the destination (target) position of the overall pathfinding process.
   *
   * @return the target position
   */
  public PathPosition targetPosition() {
    return target;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    PathfindingProgress that = (PathfindingProgress) o;
    return Objects.equals(start, that.start)
        && Objects.equals(current, that.current)
        && Objects.equals(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, current, target);
  }

  @Override
  public String toString() {
    return "PathfindingProgress{"
        + "start="
        + start
        + ", current="
        + current
        + ", target="
        + target
        + '}';
  }
}
