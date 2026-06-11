/**
 * The SPI that adapts the pathfinder to a concrete world.
 *
 * <p>A {@link de.bsommerfeld.pathetic.api.provider.NavigationPointProvider} resolves a {@link
 * de.bsommerfeld.pathetic.api.wrapper.PathPosition} (plus the request's {@link
 * de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext}) to a {@link
 * de.bsommerfeld.pathetic.api.provider.NavigationPoint}, which answers whether that position is
 * traversable. The bundled engine never queries the provider itself; validation and cost
 * processors do, so providers are exercised exactly as often as the configured processors demand.
 */
package de.bsommerfeld.pathetic.api.provider;
