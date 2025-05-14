package de.bsommerfeld.pathetic.bukkit.listener;

import de.bsommerfeld.pathetic.bukkit.provider.FailingNavigationPointProvider;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;

public class ChunkInvalidateListener implements Listener {

  @EventHandler
  public void onBurn(BlockBurnEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onExplode(BlockExplodeEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onFade(BlockFadeEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onFromTo(BlockFromToEvent event) {
    handleEvent(event.getBlock(), event.getToBlock());
  }

  @EventHandler
  public void onGrow(BlockGrowEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onPistonChange(BlockPistonRetractEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onPistonChange(BlockPistonExtendEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onPlace(BlockPlaceEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onBreak(BlockBreakEvent event) {
    handleEvent(event.getBlock());
  }

  @EventHandler
  public void onDecay(LeavesDecayEvent event) {
    handleEvent(event.getBlock());
  }

  private void handleEvent(Block... blocks) {
    for (Block block : blocks)
      FailingNavigationPointProvider.invalidateChunk(
          block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ());
  }
}
