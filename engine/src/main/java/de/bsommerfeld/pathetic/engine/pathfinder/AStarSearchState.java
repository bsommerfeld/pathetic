package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.MinHeap;
import de.bsommerfeld.pathetic.engine.pathfinder.heap.impl.QuaternaryPrimitiveMinHeap;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;

/**
 * A* per-search state: the open set (a {@link MinHeap}) and the closed set for a single search.
 *
 * <p>Per-node state is held in two layers:
 *
 * <ul>
 *   <li><strong>Packed keys</strong> identify grid cells. Keys are relative to the search origin
 *       (the floored start position), so absolute world coordinates are unconstrained (full
 *       {@code int} range) and the {@link RegionKey} ranges bound only the exploration radius of
 *       this one search. The engine never unpacks keys; they exist purely as injective
 *       identities. Keys from different searches are never compared, so the per-search origin
 *       needs no global consistency.
 *   <li><strong>Dense ids</strong> (0, 1, 2, ...) are assigned to a cell when it first enters the
 *       open set. The single key-to-id hash map below is the only hash lookup per node; every
 *       other per-node access (the heap's position index, open-set node, closed flag, recorded
 *       G-cost) is a plain array access indexed by id.
 * </ul>
 *
 * @apiNote This class is not thread-safe; one instance belongs to exactly one running search. It is
 *     created as a stack local and passed explicitly into the pathfinder's template methods.
 */
class AStarSearchState implements SearchState {

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

  /*
   * The open set. Keyed by dense ids, so its decrease-key position tracking is a plain array access
   * instead of a hash-map update per sift level.
   */
  private final MinHeap openSet;

  private final Long2IntOpenHashMap keyToId;
  private int nextId = 0;

  /*
   * Id of the node most recently returned by extractBest. markExpanded runs on that same node
   * immediately afterwards and would otherwise re-pack the position and re-hash to recover the id;
   * handing it over directly keeps the per-expansion hash lookups at exactly one (the idOf in
   * processSuccessors).
   */
  private int lastExtractedId = NO_ID;

  /** Open-set node per id; {@code null} when the id is not currently in the open set. */
  private Node[] openNodes;

  /** Closed-set membership per id. */
  private boolean[] closed;

  /*
   * Recorded G-cost at close time per id, used by the reopen comparison. Only allocated when
   * reopening is enabled; the engine never reads it otherwise. NaN marks "never recorded".
   */
  private double[] closedGCosts;

  private final boolean reopenEnabled;

  private final int originX;
  private final int originY;
  private final int originZ;

  AStarSearchState(
      PathfinderConfiguration pathfinderConfiguration, PathPosition start, int expectedNodes) {
    this.originX = start.getFlooredX();
    this.originY = start.getFlooredY();
    this.originZ = start.getFlooredZ();
    this.reopenEnabled = pathfinderConfiguration.shouldReopenClosedNodes();

    /*
     * The heap follows the raw node estimate; the id-indexed arrays are clamped so a single huge
     * request does not pay a large upfront zeroing cost (the arrays grow on demand past the cap).
     */
    this.openSet = new QuaternaryPrimitiveMinHeap(expectedNodes);

    int capacity = Math.max(MIN_ID_CAPACITY, Math.min(expectedNodes, MAX_INITIAL_ID_CAPACITY));
    this.keyToId = new Long2IntOpenHashMap(capacity);
    this.keyToId.defaultReturnValue(NO_ID);
    this.openNodes = new Node[capacity];
    this.closed = new boolean[capacity];
    if (reopenEnabled) {
      this.closedGCosts = newGCostArray(capacity);
    }
  }

  /* ----------------------------------------------------------------------------------------------
   * SearchState protocol: the algorithm-independent open/closed-set operations the main loop calls.
   * -------------------------------------------------------------------------------------------- */

  @Override
  public boolean hasOpenNodes() {
    return !openSet.isEmpty();
  }

  @Override
  public void insert(Node node, double heapKey) {
    long packed = pack(node.getPosition());
    int id = idOf(packed);
    if (id == NO_ID) {
      id = assignId(packed);
    }
    openSet.insertOrUpdate(id, heapKey);
    openNodes[id] = node;
  }

  @Override
  public Node extractBest() {
    /* Heap keys are dense ids assigned via assignId, so the narrowing cast is lossless. */
    int id = (int) openSet.extractMin();
    Node node = openNodes[id];
    openNodes[id] = null;
    lastExtractedId = id;
    return node;
  }

  @Override
  public void markExpanded(Node node) {
    /*
     * The node was just returned by extractBest, which recorded its id; reuse it instead of
     * re-packing the position and re-hashing the key.
     */
    int id = lastExtractedId;
    closed[id] = true;
    if (reopenEnabled) {
      closedGCosts[id] = node.getGCost();
    }
  }

  /* ----------------------------------------------------------------------------------------------
   * A*-specific per-neighbor API used inside processSuccessors. The single hash lookup per neighbor
   * is idOf; everything else below is an id-indexed array or heap access.
   * -------------------------------------------------------------------------------------------- */

  /**
   * Packs the given position into this search's key space, relative to the search origin. The start
   * position itself packs to key 0. Callers must check {@link #isInRange(PathPosition)} first for
   * positions that may lie outside the exploration radius.
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
   * returned {@link #NO_ID} for it; ids are stable for the search lifetime.
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

  /** Inserts or decrease-keys the given id in the open set. */
  void openInsert(int id, double heapKey) {
    openSet.insertOrUpdate(id, heapKey);
  }

  /** Returns the current heap key of the given open-set id. */
  double openKey(int id) {
    return openSet.cost(id);
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
