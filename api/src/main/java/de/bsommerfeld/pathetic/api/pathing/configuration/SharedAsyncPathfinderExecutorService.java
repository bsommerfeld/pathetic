package de.bsommerfeld.pathetic.api.pathing.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Internal type to the {@link PathfinderConfiguration} that holds onto the default, shared, executor service used
 * to dispatch async pathfinding requests.
 */
final class SharedAsyncPathfinderExecutorService {

  static final ExecutorService SHARED_PATHING_EXECUTOR_SERVICE =
    Executors.newWorkStealingPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(SharedAsyncPathfinderExecutorService::shutdownExecutor));
  }

  private static void shutdownExecutor() {
    SHARED_PATHING_EXECUTOR_SERVICE.shutdown();
    try {
      if (!SHARED_PATHING_EXECUTOR_SERVICE.awaitTermination(5, TimeUnit.SECONDS)) {
        SHARED_PATHING_EXECUTOR_SERVICE.shutdownNow();
      }
    } catch (InterruptedException e) {
      SHARED_PATHING_EXECUTOR_SERVICE.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
