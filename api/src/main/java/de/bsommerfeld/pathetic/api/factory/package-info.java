/**
 * Creation SPI for pathfinder instances.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.factory.PathfinderFactory} creates {@link
 * de.bsommerfeld.pathetic.api.pathing.Pathfinder} instances from a {@link
 * de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration}; implementations are
 * supplied by engine modules. {@link de.bsommerfeld.pathetic.api.factory.PathfinderInitializer}
 * hooks platform-specific setup into creation (used, for example, by platform integrations that
 * must register listeners before the pathfinder is handed out).
 */
package de.bsommerfeld.pathetic.api.factory;
