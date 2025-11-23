package de.bsommerfeld.pathetic.engine.util;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * Utility class to pack 3D grid coordinates (Region Indices or Block Positions) into a single
 * primitive long.
 *
 * <p>Layout: [X: 26 bit] [Z: 26 bit] [Y: 12 bit] <br>
 * Range X/Z: +/- 33,554,431 <br>
 * Range Y: 0 to 4095
 */
public final class RegionKey {

  private static final long MASK_Y = 0xFFFL; // 12 Bit
  private static final long MASK_XZ = 0x3FFFFFFL; // 26 Bit

  private static final int SHIFT_Z = 12;
  private static final int SHIFT_X = 38; // 12 + 26

  private RegionKey() {}

  /** Packs a PathPosition into a primitive long key. */
  public static long pack(PathPosition pos) {
    return pack(pos.getFlooredX(), pos.getFlooredY(), pos.getFlooredZ());
  }

  /** Packs raw integer coordinates into a primitive long key. */
  public static long pack(int x, int y, int z) {
    return ((long) x & MASK_XZ) << SHIFT_X | ((long) z & MASK_XZ) << SHIFT_Z | ((long) y & MASK_Y);
  }
}
