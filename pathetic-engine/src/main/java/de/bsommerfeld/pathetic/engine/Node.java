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

  public void setParent(Node parent) {
    this.parent = parent;
  }

  public boolean isTarget() {
    return this.position.getFlooredX() == target.getFlooredX()
        && this.position.getFlooredY() == target.getFlooredY()
        && this.position.getFlooredZ() == target.getFlooredZ();
  }

  /**
   * Calculates the estimated total cost of the path from the start node to the goal node, passing
   * through this node.
   *
   * @return the estimated total cost (represented by the F-Score)
   */
  public double getFCost() {
    return calculateFCost();
  }

  /**
   * The accumulated cost (also known as G-Score) from the starting node to the current node. This
   * value represents the actual (known) cost of traversing the path to the current node. It is
   * typically calculated by summing the movement costs from the start node to the current node.
   */
  public double getGCost() {
    return calculateGCost();
  }

  private double calculateFCost() {
    return getGCost() + heuristic.get();
  }

  private double calculateGCost() {
    if (parent == null) {
      return 0;
    }
    return parent.getGCost() + position.distance(parent.position);
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
