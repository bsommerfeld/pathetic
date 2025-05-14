package de.bsommerfeld.pathetic.bukkit.hook;

import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.bukkit.util.WatchdogUtil;

public class SpigotPathfindingHook implements PathfinderHook {

  @Override
  public void onPathfindingStep(PathfindingContext pathfindingContext) {
    tickWatchdogIfNeeded(pathfindingContext.getDepth());
  }

  private void tickWatchdogIfNeeded(Depth depth) {
    if (depth.getValue() % 500 == 0) {
      WatchdogUtil.tickWatchdog();
    }
  }
}
