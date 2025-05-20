package de.bsommerfeld.pathetic.bukkit.listener;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ChunkInvalidateEvent extends Event {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final Block block;

  public ChunkInvalidateEvent(Block block) {
    this.block = block;
  }

  public Block getBlock() {
    return block;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }
}
