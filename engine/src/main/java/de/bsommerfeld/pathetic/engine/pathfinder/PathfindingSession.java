package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.Node;
import de.bsommerfeld.pathetic.engine.pathfinder.spatial.SpatialData;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Manages state for a single pathfinding operation, ensuring thread-safety via isolation.
 *
 * <p>All packed position keys produced by this session are relative to the search origin (the
 * floored start position). The engine never unpacks keys; they serve purely as injective
 * identities for the open set, the closed set, and the region buckets. Packing relative to the
 * origin means absolute world coordinates are unconstrained (full {@code int} range) and the
 * {@link RegionKey} ranges bound only the exploration radius of this one search. Keys from
 * different sessions are never compared, so the per-session origin needs no global consistency.
 *
 * @apiNote This class is not thread-safe and is used within a ThreadLocal. If used elsewhere,
 *     developers must synchronize access to shared resources.
 */
class PathfindingSession {
  final Long2ObjectMap<SpatialData> visitedRegions = new Long2ObjectOpenHashMap<>();
  final Long2ObjectMap<Node> openSetNodes = new Long2ObjectOpenHashMap<>(1024);
  final Long2DoubleMap closedSetGCosts = new Long2DoubleOpenHashMap(2048);

  private final PathfinderConfiguration pathfinderConfiguration;

  private final int originX;
  private final int originY;
  private final int originZ;

  PathfindingSession(PathfinderConfiguration pathfinderConfiguration, PathPosition start) {
    this.closedSetGCosts.defaultReturnValue(Double.NaN);
    this.pathfinderConfiguration = pathfinderConfiguration;
    this.originX = start.getFlooredX();
    this.originY = start.getFlooredY();
    this.originZ = start.getFlooredZ();
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

  SpatialData getOrCreateSpatialData(PathPosition position) {
    int cellSize = pathfinderConfiguration.getGridCellSize();

    /*
     * Region buckets live in the same origin-relative space as the position keys so the region
     * coordinates stay packable regardless of absolute world coordinates.
     */
    int rX = Math.floorDiv(position.getFlooredX() - originX, cellSize);
    int rY = Math.floorDiv(position.getFlooredY() - originY, cellSize);
    int rZ = Math.floorDiv(position.getFlooredZ() - originZ, cellSize);

    long regionKey = RegionKey.pack(rX, rY, rZ);

    return visitedRegions.computeIfAbsent(
        regionKey, (long k) -> new SpatialData(pathfinderConfiguration));
  }
}
