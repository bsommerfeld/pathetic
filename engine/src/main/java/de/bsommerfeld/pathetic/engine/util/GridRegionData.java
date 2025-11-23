package de.bsommerfeld.pathetic.engine.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * The GridRegionData class represents the data associated with a grid region. This data includes a
 * Bloom filter used to quickly check if a position is within the region and a set of positions that
 * have been examined by the pathfinder.
 */
public class GridRegionData {

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
   * Creates a new GridRegionData with the specified Bloom filter settings.
   *
   * @param bloomFilterSize The size of the Bloom filter
   * @param bloomFilterFpp The false positive probability of the Bloom filter
   */
  public GridRegionData(int bloomFilterSize, double bloomFilterFpp) {
    Funnel<PathPosition> pathPositionFunnel =
        (pathPosition, into) ->
            into.putInt(pathPosition.getFlooredX())
                .putInt(pathPosition.getFlooredY())
                .putInt(pathPosition.getFlooredZ());

    bloomFilter = BloomFilter.create(pathPositionFunnel, bloomFilterSize, bloomFilterFpp);
    this.regionalExaminedPositions = new LongOpenHashSet();
  }

  /**
   * Creates a new GridRegionData with Bloom filter settings from the provided configuration.
   *
   * @param configuration The pathfinder configuration containing Bloom filter settings
   */
  public GridRegionData(PathfinderConfiguration configuration) {
    this(configuration.getBloomFilterSize(), configuration.getBloomFilterFpp());
  }

  public BloomFilter<PathPosition> getBloomFilter() {
    return bloomFilter;
  }

  public LongSet getRegionalExaminedPositions() {
    return regionalExaminedPositions;
  }
}
