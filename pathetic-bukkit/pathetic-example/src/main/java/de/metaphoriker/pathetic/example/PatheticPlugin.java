package de.metaphoriker.pathetic.example;

import de.metaphoriker.pathetic.api.factory.PathfinderFactory;
import de.metaphoriker.pathetic.api.factory.PathfinderInitializer;
import de.metaphoriker.pathetic.api.pathing.Pathfinder;
import de.metaphoriker.pathetic.api.pathing.configuration.HeuristicWeights;
import de.metaphoriker.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.metaphoriker.pathetic.bukkit.PatheticBukkit;
import de.metaphoriker.pathetic.bukkit.initializer.BukkitPathfinderInitializer;
import de.metaphoriker.pathetic.bukkit.provider.LoadingNavigationPointProvider;
import de.metaphoriker.pathetic.engine.factory.AStarPathfinderFactory;
import de.metaphoriker.pathetic.example.command.PatheticCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PatheticPlugin extends JavaPlugin {

  // Called when the plugin is enabled
  @Override
  public void onEnable() {

    // Initialize Pathetic with this plugin instance
    PatheticBukkit.initialize(this);

    // Create the respective PathfinderFactory
    PathfinderFactory factory = new AStarPathfinderFactory();

    // Some pathfinders need specific initialization
    // For example Bukkit pathfinders need a BukkitPathfinderInitializer
    PathfinderInitializer initializer = new BukkitPathfinderInitializer();

    // Create custom configuration for the pathfinder
    // Keep in mind that a provider must always be given
    PathfinderConfiguration configuration =
        PathfinderConfiguration.builder()
            .provider(new LoadingNavigationPointProvider()) // For loading chunks
            .fallback(true) // Allow fallback strategies if the primary fails
            .heuristicWeights(
                HeuristicWeights.create(1.0, 1.0, 1.0, 1.0, 0.0)) // custom weights for default paths
            .build();

    Pathfinder reusablePathfinder = factory.createPathfinder(configuration, initializer);

    // Register the command executor for the "pathetic" command
    getCommand("pathetic").setExecutor(new PatheticCommand(reusablePathfinder));
  }
}
