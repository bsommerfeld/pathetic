package de.bsommerfeld.pathetic.example;

import de.bsommerfeld.pathetic.api.factory.PathfinderFactory;
import de.bsommerfeld.pathetic.api.factory.PathfinderInitializer;
import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.bukkit.PatheticBukkit;
import de.bsommerfeld.pathetic.bukkit.initializer.BukkitPathfinderInitializer;
import de.bsommerfeld.pathetic.bukkit.provider.LoadingNavigationPointProvider;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import de.bsommerfeld.pathetic.example.command.PatheticCommand;
import de.bsommerfeld.pathetic.example.processor.SimpleTraversableProcessor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
                HeuristicWeights.create(
                    1.0, 1.0, 1.0, 1.0, 0.0)) // custom weights for default paths
            .nodeValidators(List.of(new SimpleTraversableProcessor()))
            .build();

    Pathfinder reusablePathfinder = factory.createPathfinder(configuration, initializer);

    // Register the command executor for the "pathetic" command
    getCommand("pathetic").setExecutor(new PatheticCommand(reusablePathfinder));
  }
}
