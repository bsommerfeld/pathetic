package de.bsommerfeld.pathetic.engine.pathfinder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    assertFalse(callbackCalled.get(), "ifPresent callback should NOT be called for FAILED state");
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
    assertTrue(callbackCalled.get(), "orElse callback should be called for LENGTH_LIMITED state");
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
        callbackCalled.get(), "orElse callback should be called for MAX_ITERATIONS_REACHED state");
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
    AtomicBoolean aborted = new AtomicBoolean(false);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future, () -> aborted.set(true));

    // When
    search.abort();

    // Then
    assertTrue(aborted.get(), "abort() should return true when future is successfully cancelled");
  }

  @Test
  void testResultBlockingReturnsCompletedResult() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    PathfinderResult result = search.resultBlocking();

    // Then
    assertSame(mockResult, result, "resultBlocking should return the completed result");
  }

  @Test
  void testResultBlockingThrowsOnException() {
    // Given
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Test exception"));
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When & Then
    assertThrows(
        RuntimeException.class,
        search::resultBlocking,
        "resultBlocking should throw RuntimeException when future completes exceptionally");
  }

  @Test
  void testResultReturnsEmptyWhenNotDone() {
    // Given
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    Optional<PathfinderResult> result = search.result();

    // Then
    assertFalse(result.isPresent(), "result() should return empty Optional when future is not done");
  }

  @Test
  void testResultReturnsPresentWhenDone() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    Optional<PathfinderResult> result = search.result();

    // Then
    assertTrue(result.isPresent(), "result() should return present Optional when future is done");
    assertSame(mockResult, result.get(), "Optional should contain the completed result");
  }

  @Test
  void testResultReturnsEmptyWhenCompletedExceptionally() {
    // Given
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Test exception"));
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    Optional<PathfinderResult> result = search.result();

    // Then
    assertFalse(
        result.isPresent(),
        "result() should return empty Optional when future completes exceptionally");
  }

  @Test
  void testDoneReturnsFalseWhenNotComplete() {
    // Given
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    boolean done = search.done();

    // Then
    assertFalse(done, "done() should return false when future is not complete");
  }

  @Test
  void testDoneReturnsTrueWhenComplete() {
    // Given
    PathfinderResult mockResult = mock(PathfinderResult.class);
    CompletableFuture<PathfinderResult> future = CompletableFuture.completedFuture(mockResult);
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    boolean done = search.done();

    // Then
    assertTrue(done, "done() should return true when future is complete");
  }

  @Test
  void testDoneReturnsTrueWhenCompletedExceptionally() {
    // Given
    CompletableFuture<PathfinderResult> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Test exception"));
    PathfindingSearchImpl search = new PathfindingSearchImpl(future);

    // When
    boolean done = search.done();

    // Then
    assertTrue(done, "done() should return true even when future completes exceptionally");
  }
}
