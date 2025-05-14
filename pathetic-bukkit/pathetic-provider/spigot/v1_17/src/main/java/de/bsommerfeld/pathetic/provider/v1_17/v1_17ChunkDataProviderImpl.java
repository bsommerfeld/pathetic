package de.bsommerfeld.pathetic.provider.v1_17;

import de.bsommerfeld.pathetic.provider.ChunkDataProvider;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockStates;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;

public class v1_17ChunkDataProviderImpl implements ChunkDataProvider {

  @Override
  public ChunkSnapshot getSnapshot(World world, int chunkX, int chunkZ) {
    try {

      WorldServer server = ((CraftWorld) world).getHandle();
      CraftChunk newCraftChunk = new CraftChunk(server, chunkX, chunkZ);

      server.getChunkProvider().getChunkAt(chunkX, chunkZ, ChunkStatus.m, true);
      return newCraftChunk.getChunkSnapshot();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public BlockState getBlockState(ChunkSnapshot snapshot, int x, int y, int z) {
    BlockData data = snapshot.getBlockData(x, y, z);
    IBlockData state = ((CraftBlockData) data).getState();
    return CraftBlockStates.getBlockState(state, null);
  }
}
