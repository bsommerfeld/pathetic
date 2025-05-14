package de.bsommerfeld.pathetic.provider;

import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.BlockState;

/** This is for internal purpose only and is used to receive a ChunkSnapshot version-independent. */
public interface ChunkDataProvider {

  /**
   * Returns a {@link ChunkSnapshot} of the chunk at the given coordinates.
   *
   * @param world The {@link World} to get the {@link ChunkSnapshot} from
   * @param chunkX The x-coordinate of the chunk
   * @param chunkZ The z-coordinate of the chunk
   * @return The {@link ChunkSnapshot} of the chunk at the given coordinates
   */
  ChunkSnapshot getSnapshot(World world, int chunkX, int chunkZ);

  /** Get the block state from a chunk snapshot at the given coordinates */
  BlockState getBlockState(ChunkSnapshot snapshot, int x, int y, int z);
}
