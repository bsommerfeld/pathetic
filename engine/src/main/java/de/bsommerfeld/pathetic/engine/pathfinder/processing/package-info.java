/**
 * Engine-side implementations of the processor context interfaces.
 *
 * <p>{@link de.bsommerfeld.pathetic.engine.pathfinder.processing.SearchContextImpl} is created
 * once per search; {@link
 * de.bsommerfeld.pathetic.engine.pathfinder.processing.EvaluationContextImpl} is created per
 * neighbor evaluation and handed to validation and cost processors.
 */
package de.bsommerfeld.pathetic.engine.pathfinder.processing;
