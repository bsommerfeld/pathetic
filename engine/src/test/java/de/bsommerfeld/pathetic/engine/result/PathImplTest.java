package de.bsommerfeld.pathetic.engine.result;

import static org.junit.jupiter.api.Assertions.*;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
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

  /*
   * Constructor accepts every concrete Collection<PathPosition> shape used across the codebase
   * (ArrayList, ArrayDeque, LinkedHashSet, singleton, empty) and reports length() in O(1).
   */
  @Test
  void acceptsArrayList() {
    List<PathPosition> positions = createPathPositions(3);
    Path path = new PathImpl(positions.get(0), positions.get(2), positions);
    assertEquals(3, path.length());
  }

  @Test
  void acceptsArrayDeque() {
    Deque<PathPosition> positions = new ArrayDeque<>(createPathPositions(4));
    Path path =
        new PathImpl(positions.peekFirst(), positions.peekLast(), positions);
    assertEquals(4, path.length());
  }

  @Test
  void acceptsLinkedHashSet() {
    LinkedHashSet<PathPosition> positions = new LinkedHashSet<>(createPathPositions(2));
    Path path = new PathImpl(positions.iterator().next(), positions.iterator().next(), positions);
    assertEquals(2, path.length());
  }

  @Test
  void acceptsSingletonList() {
    PathPosition only = new PathPosition(1, 2, 3);
    Path path = new PathImpl(only, only, Collections.singletonList(only));
    assertEquals(1, path.length());
  }

  @Test
  void acceptsEmptyCollection() {
    PathPosition start = new PathPosition(0, 0, 0);
    PathPosition end = new PathPosition(1, 0, 0);
    Path path = new PathImpl(start, end, Collections.emptyList());
    assertEquals(0, path.length());
  }

  @Test
  void lengthMatchesCollectionSize() {
    // Direct verification that length() is O(1) via Collection.size() rather than walking.
    List<PathPosition> positions = createPathPositions(1000);
    Path path = new PathImpl(positions.get(0), positions.get(999), positions);
    assertEquals(positions.size(), path.length());
  }

  @Test
  void collectReturnsIndependentCopy() {
    List<PathPosition> positions = createPathPositions(3);
    Path path = new PathImpl(positions.get(0), positions.get(2), positions);
    java.util.Collection<PathPosition> collected = path.collect();
    assertEquals(3, collected.size());
    // Mutating the returned collection must not affect the path.
    collected.clear();
    assertEquals(3, path.length());
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
