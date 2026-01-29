package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PathfindingSearchImplTest {

  @Test
  void testIfPresentCalledOnSuccess() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.FOUND);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);
    AtomicReference<PathfinderResult> capturedResult = new AtomicReference<>();

    // When
    search.ifPresent(
        result -> {
          callbackCalled.set(true);
          capturedResult.set(result);
        });

    // Then
    assertTrue(callbackCalled.get(), "ifPresent callback should be called for FOUND state");
    assertSame(mockResult, capturedResult.get());
  }

  @Test
  void testIfPresentCalledOnFallback() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.FALLBACK);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // When
    search.ifPresent(result -> callbackCalled.set(true));

    // Then
    assertTrue(callbackCalled.get(), "ifPresent callback should be called for FALLBACK state");
  }

  @Test
  void testIfPresentNotCalledOnFailed() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.FAILED);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // When
    search.ifPresent(result -> callbackCalled.set(true));

    // Then
    assertFalse(
        callbackCalled.get(), "ifPresent callback should NOT be called for FAILED state");
  }

  @Test
  void testOrElseCalledOnFailed() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.FAILED);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);
    AtomicReference<PathfinderResult> capturedResult = new AtomicReference<>();

    // When
    search.orElse(
        result -> {
          callbackCalled.set(true);
          capturedResult.set(result);
        });

    // Then
    assertTrue(callbackCalled.get(), "orElse callback should be called for FAILED state");
    assertSame(mockResult, capturedResult.get());
  }

  @Test
  void testOrElseCalledOnAborted() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.ABORTED);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // When
    search.orElse(result -> callbackCalled.set(true));

    // Then
    assertTrue(callbackCalled.get(), "orElse callback should be called for ABORTED state");
  }

  @Test
  void testOrElseCalledOnLengthLimited() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.LENGTH_LIMITED);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // When
    search.orElse(result -> callbackCalled.set(true));

    // Then
    assertTrue(
        callbackCalled.get(), "orElse callback should be called for LENGTH_LIMITED state");
  }

  @Test
  void testOrElseCalledOnMaxIterationsReached() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.MAX_ITERATIONS_REACHED);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // When
    search.orElse(result -> callbackCalled.set(true));

    // Then
    assertTrue(
        callbackCalled.get(),
        "orElse callback should be called for MAX_ITERATIONS_REACHED state");
  }

  @Test
  void testOrElseNotCalledOnSuccess() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.FOUND);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // When
    search.orElse(result -> callbackCalled.set(true));

    // Then
    assertFalse(callbackCalled.get(), "orElse callback should NOT be called for FOUND state");
  }

  @Test
  void testMethodChaining() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    when(mockResult.getPathState()).thenReturn(PathState.FOUND);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean ifPresentCalled = new AtomicBoolean(false);
    AtomicBoolean orElseCalled = new AtomicBoolean(false);

    // When
    PathfindingSearchImpl result =
        search.ifPresent(r -> ifPresentCalled.set(true)).orElse(r -> orElseCalled.set(true));

    // Then
    assertSame(search, result, "Methods should return the same instance for chaining");
    assertTrue(ifPresentCalled.get());
    assertFalse(orElseCalled.get());
  }

  @Test
  void testExceptionallyHandlesException() {
    // Given
    Exception testException = new RuntimeException("Test exception");
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    future.completeExceptionally(testException);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    AtomicBoolean exceptionHandled = new AtomicBoolean(false);
    AtomicReference<Throwable> capturedException = new AtomicReference<>();
    PathfinderResult fallbackResult = mock(PathfinderResult.class);

    // When
    search.exceptionally(
        throwable -> {
          exceptionHandled.set(true);
          capturedException.set(throwable);
          return fallbackResult;
        });

    // Then
    assertTrue(exceptionHandled.get(), "exceptionally callback should be called on exception");
    assertSame(testException, capturedException.get());
  }

  @Test
  void testIfPresentThrowsOnNullCallback() {
    // Given
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(null);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When & Then
    assertThrows(NullPointerException.class, () -> search.ifPresent(null));
  }

  @Test
  void testOrElseThrowsOnNullCallback() {
    // Given
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(null);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When & Then
    assertThrows(NullPointerException.class, () -> search.orElse(null));
  }

  @Test
  void testExceptionallyThrowsOnNullCallback() {
    // Given
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(null);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When & Then
    assertThrows(NullPointerException.class, () -> search.exceptionally(null));
  }

  @Test
  void testAbortCancelsFuture() {
    // Given
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    boolean aborted = search.abort();

    // Then
    assertTrue(aborted, "abort() should return true when future is successfully cancelled");
    assertTrue(future.isCancelled(), "CompletableFuture should be cancelled");
  }

  @Test
  void testAbortReturnsFalseWhenAlreadyCompleted() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    boolean aborted = search.abort();

    // Then
    assertFalse(
        aborted, "abort() should return false when future is already completed");
  }
}
