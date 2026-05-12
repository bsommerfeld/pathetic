package de.bsommerfeld.pathetic.api.pathing.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Internal lazy provider for the default {@link ExecutorService} used to dispatch async
 * pathfinding requests.
 *
 * <p>The pool and its JVM shutdown hook are created on first call to {@link #get()} via the
 * "initialization-on-demand holder" idiom. Callers that never request the shared executor (for
 * example, configurations with {@code async = false} or those supplying a custom executor) cause
 * neither the pool nor the shutdown hook to be allocated. The class is final and not
 * instantiable.
 */
final class SharedAsyncPathfinderExecutorService {

  private SharedAsyncPathfinderExecutorService() {}

  /**
   * Returns the shared work-stealing pool, allocating it (and registering the JVM shutdown hook)
   * on first call. Subsequent calls return the same instance.
   */
  static ExecutorService get() {
    return Holder.INSTANCE;
  }

  private static final class Holder {

    static final ExecutorService INSTANCE =
        Executors.newWorkStealingPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    static {
      Runtime.getRuntime().addShutdownHook(new Thread(Holder::shutdownExecutor));
    }

    private static void shutdownExecutor() {
      INSTANCE.shutdown();
      try {
        if (!INSTANCE.awaitTermination(5, TimeUnit.SECONDS)) {
          INSTANCE.shutdownNow();
        }
      } catch (InterruptedException e) {
        INSTANCE.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
