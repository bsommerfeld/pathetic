package de.bsommerfeld.pathetic.api.pathing.hook;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.wrapper.Depth;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

import java.util.Objects;

/** Context for the current step of the pathfinding process. */
public final class PathfindingContext {

    private final PathPosition currentPosition;
    private final Depth depth;
    private final PathPosition target;
    private final EnvironmentContext environmentContext;

    public PathfindingContext(
        PathPosition position,
        Depth depth,
        PathPosition target,
        EnvironmentContext environmentContext) {
        this.currentPosition = position;
        this.depth = depth;
        this.target = target;
        this.environmentContext = environmentContext;
    }

    /**
     * The current position of the pathfinding step.
     */
    public PathPosition currentPosition() {
        return currentPosition;
    }

    /**
     * The current depth of the pathfinding step.
     */
    public Depth getDepth() {
        return this.depth;
    }

    /**
     * The target position of the search. Invariant for the entire search: the same reference is
     * supplied to every callback, so only {@link #currentPosition()} and {@link #getDepth()} change
     * between steps.
     */
    public PathPosition target() {
        return target;
    }

    /**
     * The environment context supplied to the search, or {@code null} if none was provided. Like
     * {@link #target()}, this is invariant for the entire search.
     */
    public EnvironmentContext environmentContext() {
        return environmentContext;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PathfindingContext)) return false;
        final PathfindingContext other = (PathfindingContext) o;
        final Object this$depth = this.getDepth();
        final Object other$depth = other.getDepth();
        return Objects.equals(this$depth, other$depth);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $depth = this.getDepth();
        result = result * PRIME + ($depth == null ? 43 : $depth.hashCode());
        return result;
    }

    public String toString() {
        return "PathfindingContext(depth=" + this.getDepth() + ")";
    }
}
