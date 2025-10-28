package de.bsommerfeld.pathetic.engine.result;

import static org.junit.jupiter.api.Assertions.*;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathImplTest {

  @Test
  void testIterator() {
    List<PathPosition> positions = createPathPositions(5);
    Path path = new PathImpl(positions.get(0), positions.get(4), positions);

    int count = 0;
    for (PathPosition position : path) {
      assertEquals(positions.get(count), position);
      count++;
    }
    assertEquals(positions.size(), count);
  }

  @Test
  void testForEach() {
    List<PathPosition> positions = createPathPositions(5);
    Path path = new PathImpl(positions.get(0), positions.get(4), positions);

    List<PathPosition> collectedPositions = new ArrayList<>();
    path.forEach(collectedPositions::add);

    assertEquals(positions, collectedPositions);
  }

  @Test
  void testInterpolate() {
    List<PathPosition> positions = createPathPositions(2);
    Path path = new PathImpl(positions.get(0), positions.get(1), positions);

    Path interpolatedPath = path.interpolate(0.5);

    assertEquals(5, interpolatedPath.length());
  }

  @Test
  void testSimplify() {
    List<PathPosition> positions = createPathPositions(10);
    Path path = new PathImpl(positions.get(0), positions.get(9), positions);

    Path simplifiedPath = path.simplify(0.5);

    assertTrue(simplifiedPath.length() < path.length());
  }

  @Test
  void testSimplifyInvalidEpsilon() {
    List<PathPosition> positions = createPathPositions(10);
    Path path = new PathImpl(positions.get(0), positions.get(9), positions);

    assertThrows(IllegalStateException.class, () -> path.simplify(-0.5));
    assertThrows(IllegalStateException.class, () -> path.simplify(1.5));
  }

  @Test
  void testJoin() {
    List<PathPosition> positions1 = createPathPositions(5);
    List<PathPosition> positions2 = createPathPositions(5, 5);
    Path path1 = new PathImpl(positions1.get(0), positions1.get(4), positions1);
    Path path2 = new PathImpl(positions2.get(0), positions2.get(4), positions2);

    Path joinedPath = path1.join(path2);

    assertEquals(positions1.get(0), joinedPath.getStart());
    assertEquals(positions2.get(4), joinedPath.getEnd());
    assertEquals(10, joinedPath.length());
  }

  @Test
  void testTrim() {
    List<PathPosition> positions = createPathPositions(5);
    Path path = new PathImpl(positions.get(0), positions.get(4), positions);

    Path trimmedPath = path.trim(3);

    assertEquals(3, trimmedPath.length());
    assertEquals(positions.get(0), trimmedPath.getStart());
    assertEquals(positions.get(2), trimmedPath.getEnd());
  }

  @Test
  void testMutatePositions() {
    List<PathPosition> positions = createPathPositions(5);
    Path path = new PathImpl(positions.get(0), positions.get(4), positions);

    Path mutatedPath =
        path.mutatePositions(
            position ->
                new PathPosition(position.getX() + 1, position.getY() + 1, position.getZ() + 1));

    assertEquals(5, mutatedPath.length());
  }

  @Test
  void testLength() {
    List<PathPosition> positions = createPathPositions(5);
    Path path = new PathImpl(positions.get(0), positions.get(4), positions);

    assertEquals(5, path.length());
  }

  private List<PathPosition> createPathPositions(int numPositions) {
    return createPathPositions(numPositions, 0);
  }

  private List<PathPosition> createPathPositions(int numPositions, int startIndex) {
    List<PathPosition> positions = new ArrayList<>();
    for (int i = startIndex; i < startIndex + numPositions; i++) {
      positions.add(new PathPosition(i, i, i));
    }
    return positions;
  }
}
