package de.bsommerfeld.pathetic.engine;

import de.bsommerfeld.pathetic.api.pathing.configuration.HeuristicWeights;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.util.ComputingCache;
import java.util.Objects;

public class Node implements Comparable<Node> {

  private final PathPosition position;
  private final PathPosition start;
  private final PathPosition target;
  private final HeuristicWeights heuristicWeights;
  private final int depth;

  private final ComputingCache<Double> heuristic = new ComputingCache<>(this::heuristic);

  private double gCost;
  private Node parent;

  public Node(
      PathPosition position,
      PathPosition start,
      PathPosition target,
      HeuristicWeights heuristicWeights,
      int depth) {
    this.position = position;
    this.start = start;
    this.target = target;
    this.heuristicWeights = heuristicWeights;
    this.depth = depth;
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
    return heuristic;
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
    if (this.parent == null) return 0.0; // G-Cost for start node is 0.
    return this.gCost;
  }

  private double heuristic() {
    double manhattanDistance = this.position.manhattanDistance(target);
    double octileDistance = this.position.octileDistance(target);
    double perpendicularDistance = calculatePerpendicularDistance();
    double heightDifference = Math.abs(this.position.getFlooredY() - target.getFlooredY());

    double manhattanWeight = heuristicWeights.getManhattanWeight();
    double octileWeight = heuristicWeights.getOctileWeight();
    double perpendicularWeight = heuristicWeights.getPerpendicularWeight();
    double heightWeight = heuristicWeights.getHeightWeight();
    double directionalPenaltyWeight = heuristicWeights.getDirectionalPenaltyWeight();

    double directionalPenalty = Math.abs(this.position.getFlooredY() - start.getFlooredY());

    return (manhattanDistance * manhattanWeight)
        + (octileDistance * octileWeight)
        + (perpendicularDistance * perpendicularWeight)
        + (heightDifference * heightWeight)
        + (directionalPenalty * directionalPenaltyWeight);
  }

  private double calculatePerpendicularDistance() {
    PathVector a = this.position.toVector();
    PathVector b = this.start.toVector();
    PathVector c = this.target.toVector();
    return a.subtract(b).getCrossProduct(c.subtract(b)).length() / c.subtract(b).length();
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
    int fCostComparison = Double.compare(this.getFCost(), o.getFCost());
    if (fCostComparison != 0) {
      return fCostComparison;
    }
    int heuristicComparison = Double.compare(this.heuristic.get(), o.heuristic.get());
    if (heuristicComparison != 0) {
      return heuristicComparison;
    }
    return Integer.compare(this.depth, o.depth);
  }
}
