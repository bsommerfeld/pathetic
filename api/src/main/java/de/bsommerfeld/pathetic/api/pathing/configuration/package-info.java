/**
 * Configuration of pathfinder instances.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration} is the
 * immutable aggregate of everything a search needs: the navigation point provider, validation and
 * cost processors, neighbor and heuristic strategies, iteration and length limits, and the
 * sync/async execution mode. Instances are built via {@code PathfinderConfiguration.builder()},
 * which validates numeric arguments at the setter. {@link
 * de.bsommerfeld.pathetic.api.pathing.configuration.SharedAsyncPathfinderExecutorService} holds
 * the process-wide default executor, created lazily and only when an async configuration is
 * built.
 */
package de.bsommerfeld.pathetic.api.pathing.configuration;
