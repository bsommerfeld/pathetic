package de.bsommerfeld.pathetic.engine;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicMode;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.pathfinder.heuristic.IHeuristic;
import de.bsommerfeld.pathetic.engine.pathfinder.heuristic.LinearHeuristic;
import de.bsommerfeld.pathetic.engine.pathfinder.heuristic.SquaredHeuristic;
import de.bsommerfeld.pathetic.engine.util.ComputingCache;
import java.util.Objects;

/**
 * Represents a node in the pathfinding graph. Each node contains position information, references
 * to start and target positions, and methods to calculate heuristic values for the A* algorithm.
 *
 * <p>The Node class supports two heuristic calculation modes:
 *
 * <ul>
 *   <li>{@code PERFORMANCE}: Uses squared distances to avoid expensive square root operations,
 *       resulting in faster computations at the cost of some precision.
 *   <li>{@code PRECISION}: Uses true linear distances for more accurate path calculations, which
 *       may be slightly slower due to square root operations.
 * </ul>
 */
public class Node implements Comparable<Node> {

  private final PathPosition position;
  private final PathPosition start;
  private final PathPosition target;
  private final HeuristicWeights heuristicWeights;
  private final HeuristicMode heuristicMode;
  private final int depth;

  private final ComputingCache<Double> heuristicCache;

  private double gCost;
  private Node parent;

  /**
   * Creates a new Node with the specified parameters.
   *
   * @param position The position of this node
   * @param start The start position of the path
   * @param target The target position of the path
   * @param heuristicWeights The weights to apply to different heuristic components
   * @param heuristicMode The mode of heuristic calculation (PERFORMANCE or PRECISION)
   * @param depth The depth of this node in the search tree
   */
  public Node(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights,
      HeuristicMode heuristicMode,
      int depth) {
    this.position = position;
    this.start = start;
    this.target = target;
    this.heuristicWeights = heuristicWeights;
    this.heuristicMode = heuristicMode;
    this.depth = depth;

    IHeuristic heuristicCalculator = createHeuristic();
    this.heuristicCache = new ComputingCache<>(heuristicCalculator::calculate);
  }

  private IHeuristic createHeuristic() {
    switch (heuristicMode) {
      case PERFORMANCE:
        return new SquaredHeuristic(position, start, target, heuristicWeights);
      case PRECISION:
        return new LinearHeuristic(position, start, target, heuristicWeights);
      default:
        throw new IllegalArgumentException("Unknown heuristic mode: " + heuristicMode);
    }
  }

  public PathPosition getPosition() {
    return position;
  }

  public PathPosition getStart() {
    return start;
  }

  public PathPosition getTarget() {
    return target;
  }

  public ComputingCache<Double> getHeuristic() {
    return heuristicCache;
  }

  public Node getParent() {
    return parent;
  }

  public int getDepth() {
    return depth;
  }

  /**
   * Sets the calculated G-cost for this node. This is typically called by the pathfinding algorithm
   * after evaluating costs, including contributions from cost processors.
   *
   * @param gCost The G-cost (accumulated cost from the start node).
   */
  public void setGCost(double gCost) {
    this.gCost = gCost;
  }

  public void setParent(Node parent) {
    this.parent = parent;
  }

  public boolean isTarget() {
    return this.position.getFlooredX() == target.getFlooredX()
        && this.position.getFlooredY() == target.getFlooredY()
        && this.position.getFlooredZ() == target.getFlooredZ();
  }

  /**
   * Calculates the estimated total cost (F-cost) of the path from the start node to the target
   * node, passing through this node. F-cost = G-cost + H-cost.
   *
   * @return The estimated total cost.
   */
  public double getFCost() {
    return getGCost() + getHeuristic().get();
  }

  /**
   * Gets the calculated G-cost (accumulated known cost from the start node) for this node. This
   * value is set by the pathfinding algorithm.
   *
   * @return The G-cost.
   */
  public double getGCost() {
    if (this.parent == null) return 0.0;

    return this.gCost > 0
        ? this.gCost
        : (parent.getGCost() + calculateMovementCost(parent.getPosition(), this.position));
  }

  private double calculateMovementCost(PathPosition from, PathPosition to) {
    if (from.getFlooredX() != to.getFlooredX() && from.getFlooredZ() != to.getFlooredZ()) {
      return Math.sqrt(2); // Diagonal
    } else {
      return 1.0; // Horizontal/Vertical
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return Objects.equals(position, node.position);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(position);
  }

  @Override
  public int compareTo(Node o) {
    // First compare by F-cost (G-cost + H-cost)
    int fCostComparison = Double.compare(this.getFCost(), o.getFCost());
    if (fCostComparison != 0) {
      return fCostComparison;
    }

    // If F-costs are equal, compare by heuristic value
    int heuristicComparison = Double.compare(this.getHeuristic().get(), o.getHeuristic().get());
    if (heuristicComparison != 0) {
      return heuristicComparison;
    }

    // If heuristics are equal, compare by depth
    return Integer.compare(this.depth, o.depth);
  }
}
