/**
 * The processor pipeline that extends per-neighbor evaluation.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor} accepts or rejects
 * a neighbor; {@link de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor} contributes an
 * additional {@link de.bsommerfeld.pathetic.api.pathing.processing.Cost} to its G-cost. Both
 * extend {@link de.bsommerfeld.pathetic.api.pathing.processing.Processor}, whose {@code
 * initializeSearch}/{@code finalizeSearch} lifecycle runs once per search. {@link
 * de.bsommerfeld.pathetic.api.pathing.processing.Validators} provides boolean composites (allOf,
 * anyOf, noneOf, not) that propagate the lifecycle to their children.
 *
 * <p>Processors are configured as ordered lists on the {@link
 * de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration} and run inside the
 * search's hot loop, once per surviving neighbor; allocations and blocking calls in
 * implementations directly affect search throughput. Exceptions thrown by a processor end the
 * search with a failed result.
 *
 * @since 5.0.0
 */
package de.bsommerfeld.pathetic.api.pathing.processing;
