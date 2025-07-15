package de.bsommerfeld.pathetic.api.factory;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;

/**
 * A factory interface for creating {@link Pathfinder} instances.
 */
public interface PathfinderFactory {

    /**
     * Creates a new {@link Pathfinder} instance with the given configuration.
     *
     * @param configuration The configuration for the pathfinder.
     *
     * @return A new {@link Pathfinder} instance.
     */
    Pathfinder createPathfinder(PathfinderConfiguration configuration);

    /**
     * Creates a new {@link Pathfinder} instance with the given configuration and initializer. This method first creates
     * a pathfinder using the {@link #createPathfinder(PathfinderConfiguration)} method and then initializes it using
     * the provided {@link PathfinderInitializer}.
     *
     * @param configuration The configuration for the pathfinder.
     * @param initializer   The initializer to use for initializing the pathfinder.
     *
     * @return A new, initialized {@link Pathfinder} instance.
     */
    default Pathfinder createPathfinder(PathfinderConfiguration configuration, PathfinderInitializer initializer) {
        Pathfinder pathfinder = createPathfinder(configuration);
        initializer.initialize(pathfinder, configuration);
        return pathfinder;
    }
}
