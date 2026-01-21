package de.bsommerfeld.pathetic.engine.benchmark.heap.baritone;

/**
 * Replicated from Baritone Source (https://github.com/cabaletta/baritone) Represents the internal
 * state needed for their BinaryHeapOpenSet.
 */
public class PathNode {
  public final int x, y, z;
  public double combinedCost; // F-Cost
  public int heapPosition = -1; // Tracking internal index

  public PathNode(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
