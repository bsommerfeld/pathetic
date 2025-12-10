package de.bsommerfeld.pathetic.engine.util;

import static org.junit.jupiter.api.Assertions.*;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.util.ParameterizedSupplier;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.result.PathImpl;
import de.bsommerfeld.pathetic.engine.result.PathUtils;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PathUtilsTest {

  @Test
  @DisplayName("interpolate: correctly inserts points (robust against fast-sqrt errors)")
  void testInterpolate() {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition end = new PathPosition(10, 0, 0);

    List<PathPosition> inputList = Arrays.asList(start, end);
    Path path = new PathImpl(start, end, inputList);

    assertEquals(2, path.length(), "Input path must have 2 points");

    // Test 1: Resolution 3.0
    // Mathematik: 10.0 / 3.0 = 3.333...
    // Fast-Sqrt Fehler macht daraus vllt 3.333...4 -> Ceil ist immer noch 4.
    // Steps = 4.
    // Zwischenpunkte (Loop 1 bis <4): 3 Stück.
    // Total: 1 (Start) + 3 (Inter) + 1 (End) = 5.
    Path interpolated3 = PathUtils.interpolate(path, 3.0);
    assertEquals(5, interpolated3.length(), "Resolution 3.0 should safely yield 5 points");

    // Test 2: Resolution 1.4
    // Mathematik: 10.0 / 1.4 ≈ 7.14
    // Fast-Sqrt Fehler macht daraus vllt 7.14...1 -> Ceil ist immer noch 8.
    // Steps = 8.
    // Zwischenpunkte (Loop 1 bis <8): 7 Stück.
    // Total: 1 (Start) + 7 (Inter) + 1 (End) = 9.
    Path interpolated1_4 = PathUtils.interpolate(path, 1.4);
    assertEquals(9, interpolated1_4.length(), "Resolution 1.4 should safely yield 9 points");
  }

  @Test
  void testSimplify() {
    List<PathPosition> positions =
        Arrays.asList(
            new PathPosition(1, 0, 0),
            new PathPosition(2, 0, 0),
            new PathPosition(3, 0, 0),
            new PathPosition(4, 0, 0),
            new PathPosition(5, 0, 0));
    Path path = new PathImpl(positions.get(0), positions.get(4), positions);

    // Epsilon 0.5 -> Stride = 1/0.5 = 2. Keep every 2nd point.
    // Indices: 0 (keep), 1 (skip), 2 (keep), 3 (skip), 4 (keep)
    Path simplified = PathUtils.simplify(path, 0.5);

    assertEquals(3, simplified.length());
    int val = 1;
    for (PathPosition p : simplified) {
      assertEquals(val, p.getX(), 0.001);
      val += 2;
    }
  }

  @Test
  void testJoin() {
    PathPosition p1 = new PathPosition(0, 0, 0);
    PathPosition p2 = new PathPosition(1, 0, 0);
    PathPosition p3 = new PathPosition(2, 0, 0);

    Path path1 = new PathImpl(p1, p1, of(p1));
    Path path2 = new PathImpl(p2, p3, of(p2, p3));

    Path joined = PathUtils.join(path1, path2);

    assertEquals(3, joined.length());
    assertEquals(p1, joined.getStart());
    assertEquals(p3, joined.getEnd());
  }

  @Test
  void testTrim() {
    List<PathPosition> positions =
        Arrays.asList(
            new PathPosition(0, 0, 0), new PathPosition(1, 0, 0), new PathPosition(2, 0, 0));
    Path path = new PathImpl(positions.get(0), positions.get(2), positions);

    Path trimmed = PathUtils.trim(path, 2);
    assertEquals(2, trimmed.length());
    assertEquals(new PathPosition(1, 0, 0), trimmed.getEnd());

    Path notTrimmed = PathUtils.trim(path, 10);
    assertEquals(3, notTrimmed.length());
  }

  @Test
  void testMutatePositions() {
    PathPosition p1 = new PathPosition(1, 1, 1);
    Path path = new PathImpl(p1, p1, of(p1));

    ParameterizedSupplier<PathPosition> mutator =
        pos -> new PathPosition(pos.getX() * 2, pos.getY() * 2, pos.getZ() * 2);

    Path mutated = PathUtils.mutatePositions(path, mutator);

    PathPosition result = mutated.iterator().next();
    assertEquals(2.0, result.getX());
    assertEquals(2.0, result.getY());
    assertEquals(2.0, result.getZ());
  }

  private <T> List<T> of(T... elements) {
    return Arrays.asList(elements);
  }
}
