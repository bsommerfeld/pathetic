package de.bsommerfeld.pathetic.engine.util;

/**
 * Utility class to pack 3D grid coordinates into a single primitive long.
 *
 * <p>Layout: [X: 22 bit] [Z: 22 bit] [Y: 20 bit] <br>
 * Range X/Z: [-2,097,152, 2,097,151] (signed 22-bit) <br>
 * Range Y: [-524,288, 524,287] (signed 20-bit)
 *
 * <p><strong>Keys are search-relative, not absolute.</strong> The engine packs coordinates as
 * offsets from the search origin (the floored start position, see {@code PathfindingSession}), so
 * the ranges above bound the <em>exploration radius of a single search</em>, not world
 * coordinates. Absolute positions may use the full {@code int} range. A search cannot expand
 * positions farther from its start than the per-axis range; the pathfinder treats such positions
 * as non-navigable. The radius is far beyond what a search can reach in practice: spanning it
 * requires at least 2 million expansions in a straight line with unit offsets (bounded by {@code
 * maxIterations} and, before that, by session memory), or a custom {@code INeighborStrategy} with
 * very large offset vectors.
 *
 * <p>Each field is two's-complement and only injective within its range. Coordinates outside the
 * supported range are rejected with {@link IllegalArgumentException} rather than silently aliasing
 * through the bit mask (which would map distinct positions onto the same key and corrupt the closed
 * set and heap lookups). Callers iterating positions that may leave the range must check {@link
 * #isInRange(int, int, int)} first.
 */
public final class RegionKey {

  private static final long MASK_Y = 0xFFFFFL; // 20 Bit
  private static final long MASK_XZ = 0x3FFFFFL; // 22 Bit

  private static final int SHIFT_Z = 20;
  private static final int SHIFT_X = 42; // 20 + 22

  private static final int MIN_XZ = -(1 << 21); // -2,097,152
  private static final int MAX_XZ = (1 << 21) - 1; //  2,097,151
  private static final int MIN_Y = -(1 << 19); // -524,288
  private static final int MAX_Y = (1 << 19) - 1; //  524,287

  private RegionKey() {}

  /**
   * Checks whether the given (search-relative) coordinates fit into the packed layout, i.e.
   * whether {@link #pack(int, int, int)} would accept them. Callers iterating positions that may
   * leave the supported range (e.g. neighbor expansion at the edge of the exploration radius)
   * should use this to skip such positions instead of letting {@code pack} throw.
   */
  public static boolean isInRange(int x, int y, int z) {
    return x >= MIN_XZ && x <= MAX_XZ && z >= MIN_XZ && z <= MAX_XZ && y >= MIN_Y && y <= MAX_Y;
  }

  /**
   * Packs raw integer coordinates into a primitive long key.
   *
   * @throws IllegalArgumentException if any coordinate is outside the supported range (X/Z in
   *     [-2097152, 2097151], Y in [-524288, 524287])
   */
  public static long pack(int x, int y, int z) {
    if (x < MIN_XZ || x > MAX_XZ) throw outOfRange("x", x, MIN_XZ, MAX_XZ);
    if (z < MIN_XZ || z > MAX_XZ) throw outOfRange("z", z, MIN_XZ, MAX_XZ);
    if (y < MIN_Y || y > MAX_Y) throw outOfRange("y", y, MIN_Y, MAX_Y);
    return ((long) x & MASK_XZ) << SHIFT_X | ((long) z & MASK_XZ) << SHIFT_Z | ((long) y & MASK_Y);
  }

  private static IllegalArgumentException outOfRange(String axis, int value, int min, int max) {
    return new IllegalArgumentException(
        "RegionKey " + axis + "=" + value + " out of range [" + min + ", " + max + "]");
  }
}
