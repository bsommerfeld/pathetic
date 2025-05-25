package de.bsommerfeld.pathetic.engine.result;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;

public class PathfinderResultImpl implements PathfinderResult {

  private final PathState pathState;
  private final Path path;

  public PathfinderResultImpl(PathState pathState, Path path) {
    this.pathState = pathState;
    this.path = path;
  }

  @Override
  public boolean successful() {
    return pathState == PathState.FOUND;
  }

  @Override
  public boolean hasFailed() {
    return pathState == PathState.FAILED
        || pathState == PathState.LENGTH_LIMITED
        || pathState == PathState.MAX_ITERATIONS_REACHED;
  }

  @Override
  public boolean hasFallenBack() {
    return pathState == PathState.FALLBACK;
  }

  @Override
  public PathState getPathState() {
    return this.pathState;
  }

  @Override
  public Path getPath() {
    return this.path;
  }
}
