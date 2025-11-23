package de.bsommerfeld.pathetic.engine.util;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.hash.BloomFilter;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.junit.jupiter.api.Test;

class GridRegionDataTest {

    @Test
    void testConstructorWithExplicitParameters() {
        // Test with explicit Bloom filter parameters
        int bloomFilterSize = 500;
        double bloomFilterFpp = 0.05;
        
        GridRegionData gridRegionData = new GridRegionData(bloomFilterSize, bloomFilterFpp);
        
        // Verify the Bloom filter and set are initialized
        assertNotNull(gridRegionData.getBloomFilter());
        assertNotNull(gridRegionData.getRegionalExaminedPositions());
        
        // Verify the set is empty initially
        assertTrue(gridRegionData.getRegionalExaminedPositions().isEmpty());
    }
    
    @Test
    void testConstructorWithConfiguration() {
        // Create a configuration with custom Bloom filter settings
        int bloomFilterSize = 800;
        double bloomFilterFpp = 0.02;
        
        PathfinderConfiguration configuration = PathfinderConfiguration.builder()
                .bloomFilterSize(bloomFilterSize)
                .bloomFilterFpp(bloomFilterFpp)
                .build();
        
        GridRegionData gridRegionData = new GridRegionData(configuration);
        
        // Verify the Bloom filter and set are initialized
        assertNotNull(gridRegionData.getBloomFilter());
        assertNotNull(gridRegionData.getRegionalExaminedPositions());
        
        // Verify the set is empty initially
        assertTrue(gridRegionData.getRegionalExaminedPositions().isEmpty());
    }
    
    @Test
    void testBloomFilterFunctionality() {
        GridRegionData gridRegionData = new GridRegionData(1000, 0.01);
        BloomFilter<PathPosition> bloomFilter = gridRegionData.getBloomFilter();
        
        // Create some test positions
        PathPosition pos1 = new PathPosition(1.5, 2.5, 3.5);
        PathPosition pos2 = new PathPosition(4.5, 5.5, 6.5);
        PathPosition pos3 = new PathPosition(7.5, 8.5, 9.5);
        
        // Initially, the Bloom filter should not contain any positions
        assertFalse(bloomFilter.mightContain(pos1));
        assertFalse(bloomFilter.mightContain(pos2));
        assertFalse(bloomFilter.mightContain(pos3));
        
        // Add positions to the Bloom filter
        bloomFilter.put(pos1);
        bloomFilter.put(pos2);
        
        // Verify the Bloom filter now contains the added positions
        assertTrue(bloomFilter.mightContain(pos1));
        assertTrue(bloomFilter.mightContain(pos2));
        assertFalse(bloomFilter.mightContain(pos3));
    }

    @Test
    void testRegionalExaminedPositionsSet() {
        GridRegionData gridRegionData = new GridRegionData(1000, 0.01);

        // CHANGE 1: Der Rückgabetyp ist jetzt LongSet (primitiv), nicht mehr Set<PathPosition>
        LongSet positions = gridRegionData.getRegionalExaminedPositions();

        PathPosition pos1 = new PathPosition(1.5, 2.5, 3.5);
        PathPosition pos2 = new PathPosition(4.5, 5.5, 6.5);

        // CHANGE 2: Wir müssen die Positionen in longs umwandeln (packen),
        // genau wie es die Engine jetzt tut.
        long key1 = RegionKey.pack(pos1);
        long key2 = RegionKey.pack(pos2);

        // Initially, the set should be empty
        assertTrue(positions.isEmpty());
        assertFalse(positions.contains(key1)); // Prüfe auf key, nicht objekt
        assertFalse(positions.contains(key2));

        // Add positions to the set
        positions.add(key1); // Add key

        // Verify the set now contains the added position
        assertFalse(positions.isEmpty());
        assertTrue(positions.contains(key1));
        assertFalse(positions.contains(key2));
        assertEquals(1, positions.size());

        // Add another position
        positions.add(key2);

        // Verify the set now contains both positions
        assertTrue(positions.contains(key1));
        assertTrue(positions.contains(key2));
        assertEquals(2, positions.size());

        // Remove a position
        positions.remove(key1);

        // Verify the position was removed
        assertFalse(positions.contains(key1));
        assertTrue(positions.contains(key2));
        assertEquals(1, positions.size());
    }
}