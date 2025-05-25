package de.bsommerfeld.pathetic.api.wrapper;

import java.util.UUID;

/**
 * Represents the environment in which pathfinding operations take place. This class encapsulates
 * properties of the environment that can influence pathfinding, such as the environment's unique
 * identifier, name, and height constraints.
 *
 * @deprecated marked for removal
 */
@Deprecated
public final class PathEnvironment {

  private final UUID uuid;
  private final String name;
  private final EnvironmentHeight environmentHeight;

  /**
   * Constructs a {@code PathEnvironment} with the specified attributes.
   *
   * @param uuid The unique identifier for this environment.
   * @param name The name of this environment.
   * @param minHeight The minimum height within this environment. Can be {@code null} if there is no
   *     minimum height.
   * @param maxHeight The maximum height within this environment. Can be {@code null} if there is no
   *     maximum height.
   */
  public PathEnvironment(UUID uuid, String name, Integer minHeight, Integer maxHeight) {
    this.uuid = uuid;
    this.name = name;
    this.environmentHeight = new EnvironmentHeight(minHeight, maxHeight);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof PathEnvironment)) return false;

    PathEnvironment other = (PathEnvironment) o;
    if (this.name.length() != other.name.length()) return false; // early exit

    return this.name.equals(other.name);
  }

  @Override
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + this.name.hashCode();
    return result;
  }

  /**
   * Get the unique identifier of this environment.
   */
  public UUID getUuid() {
    return this.uuid;
  }

  /**
   * Get the name of this environment.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the min height of this environment.
   */
  public Integer getMinHeight() {
    return environmentHeight.getMinHeight();
  }

  /**
   * Get the max height of this environment.
   */
  public Integer getMaxHeight() {
    return environmentHeight.getMaxHeight();
  }

  public String toString() {
    return "PathEnvironment(uuid="
        + this.getUuid()
        + ", name="
        + this.getName()
        + ", minHeight="
        + this.getMinHeight()
        + ", maxHeight="
        + this.getMaxHeight()
        + ")";
  }
}
