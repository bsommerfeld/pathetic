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
