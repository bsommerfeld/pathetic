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
 * @apiNote This class is not thread-safe and is used within a ThreadLocal. If used elsewhere,
 *     developers must synchronize access to shared resources.
 */
class PathfindingSession {
  final Long2ObjectMap<SpatialData> visitedRegions = new Long2ObjectOpenHashMap<>();
  final Long2ObjectMap<Node> openSetNodes = new Long2ObjectOpenHashMap<>();
  final Long2DoubleMap closedSetGCosts = new Long2DoubleOpenHashMap();

  private final PathfinderConfiguration pathfinderConfiguration;

  PathfindingSession(PathfinderConfiguration pathfinderConfiguration) {
    this.closedSetGCosts.defaultReturnValue(Double.NaN);
    this.pathfinderConfiguration = pathfinderConfiguration;
  }

  SpatialData getOrCreateSpatialData(PathPosition position) {
    int cellSize = pathfinderConfiguration.getGridCellSize();

    int rX = Math.floorDiv(position.getFlooredX(), cellSize);
    int rY = Math.floorDiv(position.getFlooredY(), cellSize);
    int rZ = Math.floorDiv(position.getFlooredZ(), cellSize);

    long regionKey = RegionKey.pack(rX, rY, rZ);

    return visitedRegions.computeIfAbsent(
        regionKey, (long k) -> new SpatialData(pathfinderConfiguration));
  }

  // Defensive cleanup
  void cleanup() {
    visitedRegions.clear();
    openSetNodes.clear();
    closedSetGCosts.clear();
  }
}
