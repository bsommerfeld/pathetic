package de.bsommerfeld.pathetic.engine.pathfinder;

import de.bsommerfeld.pathetic.api.pathing.PathfindingSearch;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PathfindingSearchImpl implements PathfindingSearch {

  private final CompletableFuture<PathfinderResult> completableFuture;

  PathfindingSearchImpl(CompletableFuture<PathfinderResult> completableFuture) {
    this.completableFuture = completableFuture;
  }

  @Override
  public PathfindingSearchImpl ifPresent(Consumer<PathfinderResult> callback) {
    Objects.requireNonNull(callback, "Callback cannot be null");
    completableFuture.thenAccept(
        result -> {
          switch (result.getPathState()) {
            case FOUND:
            case FALLBACK:
              callback.accept(result);
              break;
          }
        });
    return this;
  }

  @Override
  public PathfindingSearchImpl orElse(Consumer<PathfinderResult> callback) {
    Objects.requireNonNull(callback, "Callback cannot be null");
    completableFuture.thenAccept(
        result -> {
          switch (result.getPathState()) {
            case FAILED:
            case ABORTED:
            case LENGTH_LIMITED:
            case MAX_ITERATIONS_REACHED:
              callback.accept(result);
              break;
          }
        });
    return this;
  }

  @Override
  public PathfindingSearchImpl exceptionally(Consumer<Throwable> callback) {
    Objects.requireNonNull(callback, "Callback cannot be null");
    completableFuture.exceptionally(
        ex -> {
          callback.accept(ex);
          return null;
        });
    return this;
  }

  @Override
  public boolean abort() {
    return completableFuture.cancel(true);
  }
}
