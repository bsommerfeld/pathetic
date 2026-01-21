package de.bsommerfeld.pathetic.engine.pathfinder.spatial;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.util.RegionKey;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * The SpatialData class represents the data associated with a grid region. This data includes a
 * Bloom filter used to quickly check if a position is within the region and a set of positions that
 * have been examined by the pathfinder.
 */
public class SpatialData {

  /**
   * The Bloom filter used to store the positions of the region. This filter is used to quickly
   * check if a position is within the region without having to iterate over all the positions in
   * the region.
   */
  private final BloomFilter<PathPosition> bloomFilter;

  /**
   * The set of positions that have been examined by the pathfinder. This set is used to track the
   * positions that have been examined by the pathfinder to avoid examining the same position
   * multiple times.
   */
  private final LongSet regionalExaminedPositions;

  /**
   * Creates a new SpatialData with the specified Bloom filter settings.
   *
   * @param bloomFilterSize The size of the Bloom filter
   * @param bloomFilterFpp The false positive probability of the Bloom filter
   */
  public SpatialData(int bloomFilterSize, double bloomFilterFpp) {
    Funnel<PathPosition> pathPositionFunnel =
        (pathPosition, into) ->
            into.putInt(pathPosition.getFlooredX())
                .putInt(pathPosition.getFlooredY())
                .putInt(pathPosition.getFlooredZ());

    this.bloomFilter = BloomFilter.create(pathPositionFunnel, bloomFilterSize, bloomFilterFpp);
    this.regionalExaminedPositions = new LongOpenHashSet();
  }

  /**
   * Creates a new GridRegionData with Bloom filter settings from the provided configuration.
   *
   * @param configuration The pathfinder configuration containing Bloom filter settings
   */
  public SpatialData(PathfinderConfiguration configuration) {
    this(configuration.getBloomFilterSize(), configuration.getBloomFilterFpp());
  }

  /**
   * Registers a given path position by adding it to the Bloom filter and marking it as examined
   * within the regional positions set.
   *
   * @param pathPosition The position in the path to be registered. It represents a specific
   *     location within the grid region.
   */
  public void register(PathPosition pathPosition) {
    bloomFilter.put(pathPosition);
    regionalExaminedPositions.add(RegionKey.pack(pathPosition));
  }

  /**
   * First Line of Defence. This method first checks the bloom filter if it might contain the
   * provided {@param pathPosition}. If true, it performs an expensive containment check on the
   * examined positions.
   */
  public boolean flod(PathPosition pathPosition) {
    return bloomFilter.mightContain(pathPosition)
        && regionalExaminedPositions.contains(RegionKey.pack(pathPosition));
  }
}
