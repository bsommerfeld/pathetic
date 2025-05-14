package de.bsommerfeld.pathetic.api.wrapper;

/**
 * Represents the height range of an environment with defined minimum and maximum bounds.
 * This class provides methods to access the minimum and maximum height values.
 */
public class EnvironmentHeight {

  private final int minHeight;
  private final int maxHeight;

  public EnvironmentHeight(int minHeight, int maxHeight) {
    this.minHeight = minHeight;
    this.maxHeight = maxHeight;
  }

  public int getMinHeight() {
    return minHeight;
  }

  public int getMaxHeight() {
    return maxHeight;
  }
}
