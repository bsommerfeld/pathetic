package de.bsommerfeld.pathetic.provider.v1_15;

import de.bsommerfeld.pathetic.provider.ChunkDataProvider;
import net.minecraft.server.v1_15_R1.ChunkStatus;
import net.minecraft.server.v1_15_R1.WorldServer;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_15_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;

public class v1_15ChunkDataProviderImpl implements ChunkDataProvider {

  @Override
  public ChunkSnapshot getSnapshot(World world, int chunkX, int chunkZ) {
    try {
      WorldServer server = ((CraftWorld) world).getHandle();
      CraftChunk newCraftChunk = ((CraftChunk) world.getChunkAt(chunkX, chunkZ));

      server.getChunkProvider().getChunkAt(chunkX, chunkZ, ChunkStatus.FULL, true);

      return newCraftChunk.getChunkSnapshot();
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
