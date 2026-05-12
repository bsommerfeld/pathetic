package de.bsommerfeld.pathetic.engine.result;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.pathing.result.PathState;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PathfinderResultImplTest {

  private static final Path EMPTY_PATH =
      new PathImpl(new PathPosition(0, 0, 0), new PathPosition(0, 0, 0), Collections.emptyList());

  // -------------------------------------------------------------------------
  // hasFailed contract - see CODE_REVIEW 4.3
  // FALLBACK is explicitly *not* a failure; hasFallenBack() is the right query.
  // -------------------------------------------------------------------------

  @Test
  void hasFailedIsTrueForFailedState() {
    PathfinderResultImpl result = new PathfinderResultImpl(PathState.FAILED, EMPTY_PATH);
    assertTrue(result.hasFailed());
    assertFalse(result.hasFallenBack());
    assertFalse(result.successful());
  }

  @Test
  void hasFailedIsTrueForLengthLimited() {
    PathfinderResultImpl result = new PathfinderResultImpl(PathState.LENGTH_LIMITED, EMPTY_PATH);
    assertTrue(result.hasFailed());
  }

  @Test
  void hasFailedIsTrueForMaxIterationsReached() {
    PathfinderResultImpl result =
        new PathfinderResultImpl(PathState.MAX_ITERATIONS_REACHED, EMPTY_PATH);
    assertTrue(result.hasFailed());
  }

  @Test
  void hasFailedIsFalseForFallback() {
    PathfinderResultImpl result = new PathfinderResultImpl(PathState.FALLBACK, EMPTY_PATH);
    assertFalse(result.hasFailed(), "FALLBACK must not be classified as a failure");
    assertTrue(result.hasFallenBack(), "FALLBACK must be queryable via hasFallenBack()");
    assertFalse(result.successful());
  }

  @Test
  void hasFailedIsFalseForFound() {
    PathfinderResultImpl result = new PathfinderResultImpl(PathState.FOUND, EMPTY_PATH);
    assertFalse(result.hasFailed());
    assertTrue(result.successful());
    assertFalse(result.hasFallenBack());
  }

  @Test
  void hasFailedIsFalseForAborted() {
    PathfinderResultImpl result = new PathfinderResultImpl(PathState.ABORTED, EMPTY_PATH);
    // ABORTED is a separate category - not a failure per the impl
    assertFalse(result.hasFailed());
  }
}
