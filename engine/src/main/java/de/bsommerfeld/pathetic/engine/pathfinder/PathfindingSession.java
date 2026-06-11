package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;

/**
 * Manages state for a single pathfinding operation, ensuring thread-safety via isolation.
 *
 * <p>Per-node state is held in two layers:
 *
 * <ul>
 *   <li><strong>Packed keys</strong> identify grid cells. Keys are relative to the search origin
 *       (the floored start position), so absolute world coordinates are unconstrained (full
 *       {@code int} range) and the {@link RegionKey} ranges bound only the exploration radius of
 *       this one search. The engine never unpacks keys; they exist purely as injective
 *       identities. Keys from different sessions are never compared, so the per-session origin
 *       needs no global consistency.
 *   <li><strong>Dense ids</strong> (0, 1, 2, ...) are assigned to a cell when it first enters the
 *       open set. The single key-to-id hash map below is the only hash lookup per node; every
 *       other per-node access (open-set node, closed flag, recorded G-cost, and the heap's
 *       position index) is a plain array access indexed by id.
 * </ul>
 *
 * @apiNote This class is not thread-safe and is used within a ThreadLocal. If used elsewhere,
 *     developers must synchronize access to shared resources.
 */
class PathfindingSession {

  /** Marker returned by {@link #idOf(long)} for cells that never entered the open set. */
  static final int NO_ID = -1;

  /*
   * Bounds for sizing the per-search structures from the caller's node estimate. The floor keeps
   * degenerate estimates usable; the ceiling caps the upfront allocation for huge requests, whose
   * growth then amortizes. Setup cost is dominated by zeroing these structures, so short searches
   * (the common case in workloads issuing many small requests) must not pay for large arrays.
   */
  private static final int MIN_ID_CAPACITY = 16;
  private static final int MAX_INITIAL_ID_CAPACITY = 16384;

  private final Long2IntOpenHashMap keyToId;
  private int nextId = 0;

  /** Open-set node per id; {@code null} when the id is not currently in the open set. */
  private Node[] openNodes;

  /** Closed-set membership per id. */
  private boolean[] closed;

  /*
   * Recorded G-cost at close time per id, used by the reopen comparison. Only allocated when
   * reopening is enabled; the engine never reads it otherwise. NaN marks "never recorded".
   */
  private double[] closedGCosts;

  private final int originX;
  private final int originY;
  private final int originZ;

  PathfindingSession(
      PathfinderConfiguration pathfinderConfiguration, PathPosition start, int expectedNodes) {
    this.originX = start.getFlooredX();
    this.originY = start.getFlooredY();
    this.originZ = start.getFlooredZ();

    int capacity = Math.max(MIN_ID_CAPACITY, Math.min(expectedNodes, MAX_INITIAL_ID_CAPACITY));
    this.keyToId = new Long2IntOpenHashMap(capacity);
    this.keyToId.defaultReturnValue(NO_ID);
    this.openNodes = new Node[capacity];
    this.closed = new boolean[capacity];
    if (pathfinderConfiguration.shouldReopenClosedNodes()) {
      this.closedGCosts = newGCostArray(capacity);
    }
  }

  /**
   * Packs the given position into this session's key space, relative to the search origin. The
   * start position itself packs to key 0. Callers must check {@link #isInRange(PathPosition)}
   * first for positions that may lie outside the exploration radius.
   */
  long pack(PathPosition position) {
    return RegionKey.pack(
        position.getFlooredX() - originX,
        position.getFlooredY() - originY,
        position.getFlooredZ() - originZ);
  }

  /** Checks whether the given position lies within this search's exploration radius. */
  boolean isInRange(PathPosition position) {
    return RegionKey.isInRange(
        position.getFlooredX() - originX,
        position.getFlooredY() - originY,
        position.getFlooredZ() - originZ);
  }

  /** Returns the dense id assigned to the given key, or {@link #NO_ID}. */
  int idOf(long packedKey) {
    return keyToId.get(packedKey);
  }

  /**
   * Assigns the next dense id to the given key. Must only be called when {@link #idOf(long)}
   * returned {@link #NO_ID} for it; ids are stable for the session lifetime.
   */
  int assignId(long packedKey) {
    int id = nextId++;
    keyToId.put(packedKey, id);
    ensureIdCapacity(id);
    return id;
  }

  Node openNode(int id) {
    return openNodes[id];
  }

  void setOpenNode(int id, Node node) {
    openNodes[id] = node;
  }

  void clearOpenNode(int id) {
    openNodes[id] = null;
  }

  boolean isClosed(int id) {
    return closed[id];
  }

  void markClosed(int id) {
    closed[id] = true;
  }

  /** Returns the G-cost recorded at close time, or NaN. Requires reopening to be enabled. */
  double closedGCost(int id) {
    return closedGCosts[id];
  }

  /** Records the G-cost for the reopen comparison. Requires reopening to be enabled. */
  void recordClosedGCost(int id, double gCost) {
    closedGCosts[id] = gCost;
  }

  private void ensureIdCapacity(int id) {
    if (id < openNodes.length) return;

    int newCapacity = Math.max(id + 1, openNodes.length * 2);
    openNodes = Arrays.copyOf(openNodes, newCapacity);
    closed = Arrays.copyOf(closed, newCapacity);
    if (closedGCosts != null) {
      double[] grown = newGCostArray(newCapacity);
      System.arraycopy(closedGCosts, 0, grown, 0, closedGCosts.length);
      closedGCosts = grown;
    }
  }

  private static double[] newGCostArray(int capacity) {
    double[] array = new double[capacity];
    Arrays.fill(array, Double.NaN);
    return array;
  }
}
