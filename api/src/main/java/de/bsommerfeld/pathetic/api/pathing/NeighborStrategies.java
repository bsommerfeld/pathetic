package de.bsommerfeld.pathetic.api.pathing;

import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import java.util.ArrayList;
import java.util.Arrays;
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

  /** Just checks adjacent nodes (up, down, left, right, forward, back). */
  public static final INeighborStrategy VERTICAL_AND_HORIZONTAL =
      () ->
          of(
              new PathVector(1, 0, 0), new PathVector(-1, 0, 0),
              new PathVector(0, 0, 1), new PathVector(0, 0, -1),
              new PathVector(0, 1, 0), new PathVector(0, -1, 0));

  /** Checks all 26 surrounding nodes in a 3x3x3 cube. */
  public static final INeighborStrategy DIAGONAL_3D =
      () -> {
        List<PathVector> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
          for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
              if (x == 0 && y == 0 && z == 0) continue; // Skip self
              offsets.add(new PathVector(x, y, z));
            }
          }
        }
        return offsets;
      };

  // Prevent instantiation
  private NeighborStrategies() {}

  @SafeVarargs
  private static <T> List<T> of(T... ts) {
    return new ArrayList<>(Arrays.asList(ts));
  }
}
