/**
 * Read-only context objects handed to processors.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext} describes the
 * whole search (start, target, configuration, provider, environment context, and a shared,
 * single-threaded scratch map). {@link
 * de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext} describes one
 * neighbor evaluation within it (current and previous position, depth, base transition cost,
 * accumulated path cost) and exposes the owning {@code SearchContext}.
 */
package de.bsommerfeld.pathetic.api.pathing.processing.context;
