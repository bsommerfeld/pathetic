/**
 * Immutable value types used throughout the API.
 *
 * <p>{@link de.bsommerfeld.pathetic.api.wrapper.PathPosition} is a 3D position with double
 * precision and floored integer accessors for grid-cell semantics; {@link
 * de.bsommerfeld.pathetic.api.wrapper.PathVector} is the corresponding 3D offset; {@link
 * de.bsommerfeld.pathetic.api.wrapper.Depth} wraps a node's depth in the search tree. All types
 * are immutable; arithmetic methods return new instances.
 */
package de.bsommerfeld.pathetic.api.wrapper;
