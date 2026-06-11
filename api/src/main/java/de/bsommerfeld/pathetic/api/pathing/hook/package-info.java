/**
 * Observation hooks into a running search.
 *
 * <p>A {@link de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook} is invoked once per main
 * loop iteration with a {@link de.bsommerfeld.pathetic.api.pathing.hook.PathfindingContext}
 * (current position and depth). Hooks are registered via the configuration; the engine snapshots
 * them at search start, so hooks registered while a search runs apply only to subsequent
 * searches. Hooks run on the search thread and therefore add directly to per-iteration cost.
 */
package de.bsommerfeld.pathetic.api.pathing.hook;
