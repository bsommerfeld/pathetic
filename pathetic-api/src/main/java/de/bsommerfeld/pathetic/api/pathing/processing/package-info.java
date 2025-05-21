/**
 * Provides interfaces and classes for extending the core pathfinding logic of "pathetic" through a
 * customizable processor pipeline.
 *
 * <p>The processor system allows users to inject custom logic for:
 *
 * <ul>
 *   <li><b>Node Validation ({@link de.bsommerfeld.pathetic.api.processors.NodeValidator}):</b>
 *       Determining whether a {@link de.bsommerfeld.pathetic.api.wrapper.PathPosition} is
 *       traversable or meets specific criteria. Users are responsible for implementing validators
 *       for fundamental checks like basic traversability (e.g., using a {@link
 *       de.bsommerfeld.pathetic.api.provider.NavigationPointProvider}) and world boundaries, as the
 *       core engine is designed to be highly generic.
 *   <li><b>Cost Calculation ({@link
 *       de.bsommerfeld.pathetic.api.processors.NodeCostCalculator}):</b> Modifying the cost of
 *       traversing to a node, allowing for terrain-specific penalties, incentives, or other dynamic
 *       cost adjustments.
 * </ul>
 *
 * Processors operate within a defined lifecycle (see {@link
 * de.bsommerfeld.pathetic.api.processors.Processor}) and are provided with contextual information
 * for the current search ({@link de.bsommerfeld.pathetic.api.processors.context.SearchContext}) and
 * the specific node being evaluated ({@link
 * de.bsommerfeld.pathetic.api.processors.context.NodeEvaluationContext}).
 *
 * <p>Users can configure a list of validators and cost calculators via the {@link
 * de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration}. The engine will
 * execute these processors in the provided order.
 *
 * @since 4.1.0 (or your new version)
 */
package de.bsommerfeld.pathetic.api.pathing.processing;
