/**
 * Root package of the bundled pathfinding engine.
 *
 * <p>Holds {@link de.bsommerfeld.pathetic.engine.Node}, the mutable search node: position, parent
 * link, depth, G-cost, and the heuristic computed at construction. Everything else lives in the
 * subpackages: the A* implementation under {@code pathfinder}, its heap under {@code
 * pathfinder.heap}, result types under {@code result}, and utilities under {@code util}. The
 * engine artifact shades fastutil under {@code de.bsommerfeld.pathetic.shaded}; consumer code
 * must not reference fastutil through the engine.
 */
package de.bsommerfeld.pathetic.engine;
