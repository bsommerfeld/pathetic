package de.bsommerfeld.pathetic.api.pathing.hook;

import de.bsommerfeld.pathetic.api.wrapper.Depth;

import java.util.Objects;

/** Context for the current step of the pathfinding process. */
public final class PathfindingContext {

    private final Depth depth;

    public PathfindingContext(Depth depth) {
        this.depth = depth;
    }

    /**
     * The current depth of the pathfinding step.
     */
    public Depth getDepth() {
        return this.depth;
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
