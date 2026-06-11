package de.bsommerfeld.pathetic.engine.pathfinder.spatial;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * The SpatialData class represents the data associated with a grid region. This data includes a
 * Bloom filter used to quickly check if a position is within the region and a set of positions that
 * have been examined by the pathfinder.
 *
 * @deprecated The bundled A* engine no longer buckets its closed set into bloom-filtered grid
 *     regions: closed-set membership is a dense-id-indexed array lookup, which is both cheaper
 *     than the bloom-filter probe and exact. The class is retained for consumers that use it
 *     standalone.
 */
@Deprecated
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
   * <p>The packed key is supplied by the caller (it already computes it for the open and closed
   * sets) so this class stays agnostic of the key space - keys are search-relative and only the
   * owning session can derive them. Position and key must refer to the same location.
   *
   * @param pathPosition The position in the path to be registered. It represents a specific
   *     location within the grid region.
   * @param packedKey The position's packed key in the owning session's key space.
   */
  public void register(PathPosition pathPosition, long packedKey) {
    bloomFilter.put(pathPosition);
    regionalExaminedPositions.add(packedKey);
  }

  /**
   * First Line of Defence. This method first checks the bloom filter if it might contain the
   * provided position. If true, it performs an expensive containment check on the examined
   * positions.
   *
   * @param pathPosition The position to check.
   * @param packedKey The position's packed key in the owning session's key space.
   */
  public boolean flod(PathPosition pathPosition, long packedKey) {
    return bloomFilter.mightContain(pathPosition)
        && regionalExaminedPositions.contains(packedKey);
  }
}
