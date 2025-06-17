package de.bsommerfeld.pathetic.api.pathing.heuristic;

/**
 * Represents a set of weights used to calculate a heuristic for the A* pathfinding algorithm. These
 * weights influence the prioritization of different path characteristics during the search.
 *
 * <p>This class defines weights for the following distance metrics:
 *
 * <ul>
 *   <li><b>Manhattan Distance:</b> Prioritizes direct movement along axes.
 *   <li><b>Octile Distance:</b> Allows for diagonal movement for finer-grained pathing.
 *   <li><b>Perpendicular Distance:</b> Penalizes deviation from the straight line to the target,
 *       aiding in smoother paths.
 *   <li><b>Height Difference:</b> Factors in elevation changes when calculating path costs.
 * </ul>
 */
public final class HeuristicWeights {

  /**
   * Provides a set of default heuristic weights that may be suitable for natural pathfinding. These
   * values can be adjusted for specific scenarios.
   */
  @Deprecated
  public static final HeuristicWeights NATURAL_PATH_WEIGHTS = create(0.3, 0.15, 0.6, 0.3);

  /**
   * Provides a set of weights strongly prioritizing the shortest direct path, even if diagonally.
   */
  @Deprecated public static final HeuristicWeights DIRECT_PATH_WEIGHTS = create(0.6, 0.3, 0.0, 0.1);

  /**
   * Represents the default set of heuristic weights used for pathfinding calculations. This
   * instance is initialized with equal weights for all heuristic components: {@code
   * manhattanWeight}, {@code octileWeight}, {@code perpendicularWeight}, and {@code heightWeight},
   * each set to 1.0.
   *
   * <p>The {@code DEFAULT_WEIGHTS} instance prioritizes a balanced consideration of direct,
   * axis-aligned movement, diagonal movement, path smoothness, and elevation changes.
   */
  public static final HeuristicWeights DEFAULT_WEIGHTS = create(1.0, 1.0, 1.0, 1.0);

  private final double manhattanWeight;

  private final double octileWeight;

  private final double perpendicularWeight;

  private final double heightWeight;

  private HeuristicWeights(
      double manhattanWeight,
      double octileWeight,
      double perpendicularWeight,
      double heightWeight) {
    this.manhattanWeight = manhattanWeight;
    this.octileWeight = octileWeight;
    this.perpendicularWeight = perpendicularWeight;
    this.heightWeight = heightWeight;
  }

  /**
   * Creates a new {@code HeuristicWeights} instance with the specified weights.
   *
   * @param manhattanWeight The weight applied to the Manhattan distance component. A higher weight
   *     favours paths with a greater emphasis on direct, axis-aligned movement.
   * @param octileWeight The weight applied to the Octile distance component. A higher weight allows
   *     diagonal movement, enabling more flexible paths in 3D environments.
   * @param perpendicularWeight The weight applied to the perpendicular distance component.
   *     Increased weight discourages deviations from the straight line between the start and
   *     target, resulting in smoother paths.
   * @param heightWeight The weight applied to the height difference (elevation change) component. A
   *     higher weight gives more consideration to vertical distance, important for terrains with
   *     varying verticality.
   * @return A new {@code HeuristicWeights} instance with the given weights.
   */
  public static HeuristicWeights create(
      double manhattanWeight,
      double octileWeight,
      double perpendicularWeight,
      double heightWeight) {
    return new HeuristicWeights(manhattanWeight, octileWeight, perpendicularWeight, heightWeight);
  }

  /**
   * Returns the weight applied to the Manhattan distance component of the heuristic.
   *
   * @return The Manhattan distance weight.
   */
  public double getManhattanWeight() {
    return this.manhattanWeight;
  }

  /**
   * Returns the weight applied to the Octile distance component of the heuristic.
   *
   * @return The Octile distance weight.
   */
  public double getOctileWeight() {
    return this.octileWeight;
  }

  /**
   * Returns the weight applied to the perpendicular distance component of the heuristic.
   *
   * @return The perpendicular distance weight.
   */
  public double getPerpendicularWeight() {
    return this.perpendicularWeight;
  }

  /**
   * Returns the weight applied to the height difference (elevation change) component of the
   * heuristic.
   *
   * @return The height difference weight.
   */
  public double getHeightWeight() {
    return this.heightWeight;
  }

  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof HeuristicWeights)) return false;
    final HeuristicWeights other = (HeuristicWeights) o;
    if (Double.compare(this.getManhattanWeight(), other.getManhattanWeight()) != 0) return false;
    if (Double.compare(this.getOctileWeight(), other.getOctileWeight()) != 0) return false;
    if (Double.compare(this.getPerpendicularWeight(), other.getPerpendicularWeight()) != 0)
      return false;
    return Double.compare(this.getHeightWeight(), other.getHeightWeight()) == 0;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + Double.hashCode(this.getManhattanWeight());
    result = result * PRIME + Double.hashCode(this.getOctileWeight());
    result = result * PRIME + Double.hashCode(this.getPerpendicularWeight());
    result = result * PRIME + Double.hashCode(this.getHeightWeight());
    return result;
  }

  public String toString() {
    return "HeuristicWeights(manhattanWeight="
        + this.getManhattanWeight()
        + ", octileWeight="
        + this.getOctileWeight()
        + ", perpendicularWeight="
        + this.getPerpendicularWeight()
        + ", heightWeight="
        + this.getHeightWeight()
        + ")";
  }
}
