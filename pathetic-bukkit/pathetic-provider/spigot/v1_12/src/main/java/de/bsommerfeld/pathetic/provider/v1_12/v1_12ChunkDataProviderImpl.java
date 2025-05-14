package de.bsommerfeld.pathetic.provider.v1_12;

import de.bsommerfeld.pathetic.provider.ChunkDataProvider;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.WorldServer;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;

public class v1_12ChunkDataProviderImpl implements ChunkDataProvider {

  @Override
  public ChunkSnapshot getSnapshot(World world, int chunkX, int chunkZ) {
    try {
      WorldServer server = ((CraftWorld) world).getHandle();
      Chunk chunk = server.getChunkProvider().getChunkAt(chunkX, chunkZ);

      return chunk.bukkitChunk.getChunkSnapshot();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public BlockState getBlockState(ChunkSnapshot snapshot, int x, int y, int z) {
    return null;
  }
}
