package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class serves as a util class in order to access Pathetic's predefined {@link
 * INeighborStrategy}s.
 *
 * <p>To create your own please see the INeighborStrategy interface linked above.
 *
 * <p>This util class consists of two implementations:
 *
 * <p><code>VERTICAL_AND_HORIZONTAL</code> which consists of adjacent nodes (up, down, left, right,
 * forward, backward)
 *
 * <p><code>DIAGONAL_3D</code> which consists of all 26 surrounding nodes in a 3x3x3 cube.
 */
public final class NeighborStrategies {

  private static final List<PathVector> VERTICAL_AND_HORIZONTAL_OFFSETS =
      Collections.unmodifiableList(
          Arrays.asList(
              new PathVector(1, 0, 0), new PathVector(-1, 0, 0),
              new PathVector(0, 0, 1), new PathVector(0, 0, -1),
              new PathVector(0, 1, 0), new PathVector(0, -1, 0)));

  private static final List<PathVector> DIAGONAL_3D_OFFSETS = buildDiagonal3dOffsets();

  /** Just checks adjacent nodes (up, down, left, right, forward, back). */
  public static final INeighborStrategy VERTICAL_AND_HORIZONTAL = () -> VERTICAL_AND_HORIZONTAL_OFFSETS;

  /** Checks all 26 surrounding nodes in a 3x3x3 cube. */
  public static final INeighborStrategy DIAGONAL_3D = () -> DIAGONAL_3D_OFFSETS;

  // Prevent instantiation
  private NeighborStrategies() {}

  private static List<PathVector> buildDiagonal3dOffsets() {
    List<PathVector> offsets = new ArrayList<>(26);
    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x == 0 && y == 0 && z == 0) continue; // Skip self
          offsets.add(new PathVector(x, y, z));
        }
      }
    }
    return Collections.unmodifiableList(offsets);
  }
}
