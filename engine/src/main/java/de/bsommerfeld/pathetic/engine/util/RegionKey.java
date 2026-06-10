package de.bsommerfeld.pathetic.engine.util;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * Utility class to pack 3D grid coordinates (Region Indices or Block Positions) into a single
 * primitive long.
 *
 * <p>Layout: [X: 26 bit] [Z: 26 bit] [Y: 12 bit] <br>
 * Range X/Z: [-33,554,432, 33,554,431] (signed 26-bit) <br>
 * Range Y: [-2048, 2047] (signed 12-bit)
 *
 * <p>Each field is two's-complement and only injective within its range. Coordinates outside the
 * supported range are rejected with {@link IllegalArgumentException} rather than silently aliasing
 * through the bit mask (which would map distinct positions onto the same key and corrupt the closed
 * set and heap lookups).
 */
public final class RegionKey {

  private static final long MASK_Y = 0xFFFL; // 12 Bit
  private static final long MASK_XZ = 0x3FFFFFFL; // 26 Bit

  private static final int SHIFT_Z = 12;
  private static final int SHIFT_X = 38; // 12 + 26

  private static final int MIN_XZ = -(1 << 25); // -33,554,432
  private static final int MAX_XZ = (1 << 25) - 1; //  33,554,431
  private static final int MIN_Y = -(1 << 11); // -2,048
  private static final int MAX_Y = (1 << 11) - 1; //   2,047

  private RegionKey() {}

  /**
   * Checks whether the given coordinates fit into the packed layout, i.e. whether {@link
   * #pack(int, int, int)} would accept them. Callers iterating positions that may leave the
   * supported range (e.g. neighbor expansion near the boundary) should use this to skip such
   * positions instead of letting {@code pack} throw.
   */
  public static boolean isInRange(int x, int y, int z) {
    return x >= MIN_XZ && x <= MAX_XZ && z >= MIN_XZ && z <= MAX_XZ && y >= MIN_Y && y <= MAX_Y;
  }

  /** Checks whether the floored coordinates of the given position fit into the packed layout. */
  public static boolean isInRange(PathPosition pos) {
    return isInRange(pos.getFlooredX(), pos.getFlooredY(), pos.getFlooredZ());
  }

  /** Packs a PathPosition into a primitive long key. */
  public static long pack(PathPosition pos) {
    return pack(pos.getFlooredX(), pos.getFlooredY(), pos.getFlooredZ());
  }

  /**
   * Packs raw integer coordinates into a primitive long key.
   *
   * @throws IllegalArgumentException if any coordinate is outside the supported range (X/Z in
   *     [-33,554,432, 33,554,431], Y in [-2048, 2047])
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
