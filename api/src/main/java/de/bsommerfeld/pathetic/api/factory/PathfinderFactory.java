package de.bsommerfeld.pathetic.api.factory;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;

/**
 * A factory interface for creating {@link Pathfinder} instances.
 */
public interface PathfinderFactory {

    /**
     * Creates a new instance of a {@link Pathfinder} with a default {@link PathfinderConfiguration}.
     *
     * <p>The resulting pathfinder uses the builder defaults, including a {@link
     * de.bsommerfeld.pathetic.api.provider.NavigationPointProvider} that reports <strong>every
     * position as traversable</strong>. The returned pathfinder will therefore route through walls,
     * obstacles, and otherwise non-traversable terrain. This is only useful for ad-hoc smoke tests
     * and never for production use.
     *
     * @return A new {@link Pathfinder} instance configured with builder defaults.
     * @deprecated Use {@link #createPathfinder(PathfinderConfiguration)} with an explicit
     *     configuration that supplies a real {@link
     *     de.bsommerfeld.pathetic.api.provider.NavigationPointProvider}. Calling this no-arg
     *     overload almost always indicates a misconfiguration.
     */
    @Deprecated
    Pathfinder createPathfinder();

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
