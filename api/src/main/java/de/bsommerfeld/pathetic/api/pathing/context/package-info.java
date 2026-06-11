/**
 * Caller-supplied environment information for a single request.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext} is a marker interface:
 * callers pass an implementation into {@code Pathfinder.findPath} to describe the world the
 * request runs against (a platform integration would carry its world handle here), and providers
 * and processors receive it back unchanged. The value may be {@code null} when no environment
 * information is needed.
 */
package de.bsommerfeld.pathetic.api.pathing.context;
