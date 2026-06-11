/**
 * Engine-internal utilities.
 *
 * <p>{@link de.bsommerfeld.pathetic.engine.util.RegionKey} packs 3D grid coordinates into a
 * single primitive long ([X: 22 bit][Z: 22 bit][Y: 20 bit]); the engine packs coordinates
 * relative to the search origin, so the ranges bound the exploration radius of one search rather
 * than absolute world coordinates. {@link de.bsommerfeld.pathetic.engine.util.Iterables} provides
 * allocation-conscious helpers over {@code Iterable}.
 */
package de.bsommerfeld.pathetic.engine.util;
