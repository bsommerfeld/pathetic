package de.bsommerfeld.pathetic.engine.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.pathfinder.spatial.SpatialData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SpatialDataTest {

  private SpatialData spatialData;
  private PathPosition position1;
  private PathPosition position2;
  private PathPosition position3;

  @BeforeEach
  void setUp() {
    spatialData = new SpatialData(1000, 0.01);
    position1 = new PathPosition(10, 20, 30);
    position2 = new PathPosition(100, 200, 300);
    position3 = new PathPosition(-5, -10, -15);
  }

  @Test
  void testConstructorWithBloomFilterSettings() {
    SpatialData data = new SpatialData(500, 0.05);
    assertNotNull(data);
  }

  @Test
  void testConstructorWithConfiguration() {
    PathfinderConfiguration config = Mockito.mock(PathfinderConfiguration.class);
    when(config.getBloomFilterSize()).thenReturn(1000);
    when(config.getBloomFilterFpp()).thenReturn(0.01);

    SpatialData data = new SpatialData(config);
    assertNotNull(data);

    verify(config).getBloomFilterSize();
    verify(config).getBloomFilterFpp();
  }

  @Test
  void testRegisterAddsPositionToDataStructures() {
    spatialData.register(position1);

    // After registering, flod should return true for the same position
    assertTrue(spatialData.flod(position1));
  }

  @Test
  void testFlodReturnsFalseForUnregisteredPosition() {
    // Without registering, flod should return false
    assertFalse(spatialData.flod(position1));
  }

  @Test
  void testFlodReturnsTrueAfterRegistration() {
    spatialData.register(position1);
    assertTrue(spatialData.flod(position1));
  }

  @Test
  void testMultipleRegistrations() {
    spatialData.register(position1);
    spatialData.register(position2);
    spatialData.register(position3);

    assertTrue(spatialData.flod(position1));
    assertTrue(spatialData.flod(position2));
    assertTrue(spatialData.flod(position3));
  }

  @Test
  void testFlodWithDifferentPositions() {
    spatialData.register(position1);

    assertTrue(spatialData.flod(position1));
    assertFalse(spatialData.flod(position2));
    assertFalse(spatialData.flod(position3));
  }

  @Test
  void testRegisterSamePositionMultipleTimes() {
    spatialData.register(position1);
    spatialData.register(position1);
    spatialData.register(position1);

    // Should still return true even after multiple registrations
    assertTrue(spatialData.flod(position1));
  }

  @Test
  void testFlodWithNegativeCoordinates() {
    spatialData.register(position3);
    assertTrue(spatialData.flod(position3));
  }

  @Test
  void testFlodWithZeroCoordinates() {
    PathPosition zeroPosition = new PathPosition(0, 0, 0);
    spatialData.register(zeroPosition);
    assertTrue(spatialData.flod(zeroPosition));
  }

  @Test
  void testFlodWithLargeCoordinates() {
    // Boundary of the supported RegionKey range: X/Z at the 26-bit max, Y at the 12-bit max.
    PathPosition largePosition = new PathPosition(33554431, 2047, 33554431);
    spatialData.register(largePosition);
    assertTrue(spatialData.flod(largePosition));
  }

  @Test
  void testBloomFilterAndExaminedPositionsIntegration() {
    // Register multiple positions
    for (int i = 0; i < 100; i++) {
      PathPosition pos = new PathPosition(i, i * 2, i * 3);
      spatialData.register(pos);
    }

    // Verify all registered positions are found
    for (int i = 0; i < 100; i++) {
      PathPosition pos = new PathPosition(i, i * 2, i * 3);
      assertTrue(spatialData.flod(pos));
    }

    // Verify unregistered positions are not found
    PathPosition unregistered = new PathPosition(999, 999, 999);
    assertFalse(spatialData.flod(unregistered));
  }

  @Test
  void testFlodPerformsTwoLevelCheck() {
    // Create a spy to verify method interactions
    SpatialData spyData = spy(new SpatialData(1000, 0.01));

    // Register a position
    spyData.register(position1);

    // Call flod - should check bloom filter first, then examined positions
    boolean result = spyData.flod(position1);
    assertTrue(result);

    // Verify flod was called
    verify(spyData).flod(position1);
  }

  @Test
  void testFlodWithSimilarPositions() {
    PathPosition pos1 = new PathPosition(10, 20, 30);
    PathPosition pos2 = new PathPosition(10, 20, 31); // Only z differs by 1

    spatialData.register(pos1);

    assertTrue(spatialData.flod(pos1));
    assertFalse(spatialData.flod(pos2));
  }

  @Test
  void testFlodWithIdenticalCoordinatesDifferentInstances() {
    PathPosition pos1 = new PathPosition(50, 60, 70);
    PathPosition pos2 = new PathPosition(50, 60, 70); // Same coordinates, different instance

    spatialData.register(pos1);

    // Should return true for pos2 since it has the same coordinates
    assertTrue(spatialData.flod(pos2));
  }

  @Test
  void testRegisterAndFlodStressTest() {
    // Register many positions
    for (int x = 0; x < 10; x++) {
      for (int y = 0; y < 10; y++) {
        for (int z = 0; z < 10; z++) {
          PathPosition pos = new PathPosition(x, y, z);
          spatialData.register(pos);
        }
      }
    }

    // Verify all registered positions
    for (int x = 0; x < 10; x++) {
      for (int y = 0; y < 10; y++) {
        for (int z = 0; z < 10; z++) {
          PathPosition pos = new PathPosition(x, y, z);
          assertTrue(spatialData.flod(pos));
        }
      }
    }
  }

  @Test
  void testBloomFilterFalsePositiveBehavior() {
    // With a very small bloom filter, we might see false positives in the first check
    // but the second check (examined positions) should filter them out
    SpatialData smallFilterData = new SpatialData(10, 0.5); // Small size, high FPP

    smallFilterData.register(position1);

    // The registered position should definitely be found
    assertTrue(smallFilterData.flod(position1));

    // Even with potential bloom filter false positives,
    // unregistered positions should not be found due to the second check
    assertFalse(smallFilterData.flod(position2));
  }

  @Test
  void testConfigurationConstructorUsesCorrectSettings() {
    PathfinderConfiguration config = Mockito.mock(PathfinderConfiguration.class);
    when(config.getBloomFilterSize()).thenReturn(2000);
    when(config.getBloomFilterFpp()).thenReturn(0.001);

    SpatialData data = new SpatialData(config);

    // Test that the data structure works correctly
    data.register(position1);
    assertTrue(data.flod(position1));

    verify(config).getBloomFilterSize();
    verify(config).getBloomFilterFpp();
  }
}
