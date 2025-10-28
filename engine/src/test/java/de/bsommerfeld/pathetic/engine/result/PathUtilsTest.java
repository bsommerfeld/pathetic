package de.bsommerfeld.pathetic.engine.result;

import static org.junit.jupiter.api.Assertions.*;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.util.ParameterizedSupplier;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PathUtilsTest {

  private PathPosition p(double x, double y, double z) {
    return new PathPosition(x, y, z);
  }

  // Helper: Create test path
  private Path path(PathPosition... positions) {
    if (positions.length == 0) {
      return new PathImpl(null, null, Collections.emptyList());
    }
    return new PathImpl(positions[0], positions[positions.length - 1], Arrays.asList(positions));
  }

  @Test
  @DisplayName("interpolate: throws on resolution <= 0")
  void interpolate_throwsOnInvalidResolution() {
    Path path = path(p(0, 0, 0), p(1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> PathUtils.interpolate(path, 0));
    assertThrows(IllegalArgumentException.class, () -> PathUtils.interpolate(path, -1));
  }

  @Test
  @DisplayName("interpolate: single point path unchanged")
  void interpolate_singlePointPath() {
    Path path = path(p(5, 5, 5));
    Path result = PathUtils.interpolate(path, 0.1);
    assertPathEquals(Collections.singletonList(p(5, 5, 5)), result);
  }

  // ========================================================================
  // simplify()
  // ========================================================================
  @Test
  @DisplayName("simplify: epsilon 1.0 keeps all points")
  void simplify_epsilon1KeepsAll() {
    Path path = path(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0), p(3, 0, 0));
    Path result = PathUtils.simplify(path, 1.0);
    assertEquals(4, result.length());
  }

  @Test
  @DisplayName("simplify: epsilon 0.5 keeps every 2nd point")
  void simplify_epsilon05KeepsEverySecond() {
    Path path = path(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0), p(3, 0, 0), p(4, 0, 0), p(5, 0, 0));
    Path result = PathUtils.simplify(path, 0.5);
    assertEquals(3, result.length()); // 0, 2, 5
  }

  @Test
  @DisplayName("simplify: epsilon 0.1 keeps first and last")
  void simplify_epsilon01KeepsFirstAndLast() {
    Path path = path(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0), p(3, 0, 0), p(4, 0, 0), p(5, 0, 0));
    Path result = PathUtils.simplify(path, 0.3);
    assertEquals(2, result.length());
  }

  @Test
  @DisplayName("simplify: throws on invalid epsilon")
  void simplify_throwsOnInvalidEpsilon() {
    Path path = path(p(0, 0, 0), p(1, 0, 0));
    assertThrows(RuntimeException.class, () -> PathUtils.simplify(path, 0));
    assertThrows(RuntimeException.class, () -> PathUtils.simplify(path, 1.1));
    assertThrows(RuntimeException.class, () -> PathUtils.simplify(path, -0.1));
  }

  // ========================================================================
  // join()
  // ========================================================================
  @Test
  @DisplayName("join: combines two 3D paths")
  void join_combinesTwoPaths() {
    Path a = path(p(0, 0, 0), p(1, 1, 1));
    Path b = path(p(1, 1, 1), p(2, 2, 2), p(3, 3, 3));
    Path result = PathUtils.join(a, b);

    assertPathEquals(Arrays.asList(p(0, 0, 0), p(1, 1, 1), p(2, 2, 2), p(3, 3, 3)), result);
  }

  @Test
  @DisplayName("join: handles empty paths")
  void join_handlesEmptyPaths() {
    Path empty = path();
    Path full = path(p(1, 1, 1), p(2, 2, 2));

    assertSame(full, PathUtils.join(empty, full));
    assertSame(full, PathUtils.join(full, empty));
    assertSame(empty, PathUtils.join(empty, empty));
  }

  // ========================================================================
  // trim()
  // ========================================================================
  @Test
  @DisplayName("trim: limits path to max length")
  void trim_limitsPathLength() {
    Path path = path(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0), p(3, 0, 0));
    Path result = PathUtils.trim(path, 2);
    assertPathEquals(Arrays.asList(p(0, 0, 0), p(1, 0, 0)), result);
  }

  @Test
  @DisplayName("trim: returns same path if shorter")
  void trim_returnsSameIfShorter() {
    Path path = path(p(0, 0, 0), p(1, 0, 0));
    Path result = PathUtils.trim(path, 5);
    assertSame(path, result);
  }

  @Test
  @DisplayName("trim: throws on maxLength <= 0")
  void trim_throwsOnInvalidLength() {
    Path path = path(p(0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> PathUtils.trim(path, 0));
  }

  // ========================================================================
  // mutatePositions()
  // ========================================================================
  @Test
  @DisplayName("mutatePositions: applies 3D transformation")
  void mutatePositions_appliesTransformation() {
    Path path = path(p(1, 2, 3), p(4, 5, 6));
    Path result =
        PathUtils.mutatePositions(path, pos -> p(pos.getX() * 2, pos.getY() * 2, pos.getZ() * 2));

    assertPathEquals(Arrays.asList(p(2, 4, 6), p(8, 10, 12)), result);
  }

  @Test
  @DisplayName("mutatePositions: preserves order")
  void mutatePositions_preservesOrder() {
    Path path = path(p(0, 0, 0), p(1, 1, 1), p(2, 2, 2));
    Path result =
        PathUtils.mutatePositions(
            path, pos -> p(pos.getZ(), pos.getY(), pos.getX()) // ZYX
            );
    assertPathEquals(Arrays.asList(p(0, 0, 0), p(1, 1, 1), p(2, 2, 2)), result);
  }

  // ========================================================================
  // Helper: Assert path content
  // ========================================================================
  private void assertPathEquals(List<PathPosition> expected, Path actual) {
    List<PathPosition> actualList = new ArrayList<>(actual.collect());
    assertEquals(expected.size(), actualList.size(), "Path length mismatch");

    for (int i = 0; i < expected.size(); i++) {
      PathPosition exp = expected.get(i);
      PathPosition act = actualList.get(i);
      assertEquals(exp.getX(), act.getX(), 1e-9, "X mismatch at index " + i);
      assertEquals(exp.getY(), act.getY(), 1e-9, "Y mismatch at index " + i);
      assertEquals(exp.getZ(), act.getZ(), 1e-9, "Z mismatch at index " + i);
    }

    assertEquals(expected.get(0), actual.getStart(), "Start position mismatch");
    assertEquals(expected.get(expected.size() - 1), actual.getEnd(), "End position mismatch");
  }

  // ========================================================================
  // Minimal PathImpl
  // ========================================================================
  private static class PathImpl implements Path {
    private final PathPosition start;
    private final PathPosition end;
    private final List<PathPosition> positions;

    PathImpl(PathPosition start, PathPosition end, List<PathPosition> positions) {
      this.start = start;
      this.end = end;
      this.positions = new ArrayList<>(positions);
    }

    @Override
    public PathPosition getStart() {
      return start;
    }

    @Override
    public PathPosition getEnd() {
      return end;
    }

    @Override
    public int length() {
      return positions.size();
    }

    @Override
    public Path interpolate(double resolution) {
      return null;
    }

    @Override
    public Path simplify(double epsilon) {
      return null;
    }

    @Override
    public Path join(Path path) {
      return null;
    }

    @Override
    public Path trim(int length) {
      return null;
    }

    @Override
    public Path mutatePositions(ParameterizedSupplier<PathPosition> mutator) {
      return null;
    }

    @Override
    public Iterator<PathPosition> iterator() {
      return positions.iterator();
    }

    @Override
    public Collection<PathPosition> collect() {
      return new ArrayList<>(positions);
    }
  }
}
