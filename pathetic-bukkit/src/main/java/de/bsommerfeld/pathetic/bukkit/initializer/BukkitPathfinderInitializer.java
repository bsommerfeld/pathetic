package de.bsommerfeld.pathetic.bukkit.initializer;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.factory.PathfinderInitializer;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.bukkit.hook.SpigotPathfindingHook;

public class BukkitPathfinderInitializer implements PathfinderInitializer {

  @Override
  public void initialize(Pathfinder pathfinder, PathfinderConfiguration configuration) {
    pathfinder.registerPathfindingHook(new SpigotPathfindingHook());
  }
}
