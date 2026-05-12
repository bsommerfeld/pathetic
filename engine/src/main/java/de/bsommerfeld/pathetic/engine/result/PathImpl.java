package de.bsommerfeld.pathetic.engine.result;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

public class PathImpl implements Path {

  /*
   * Collection guarantees O(1) size() and safe re-iteration without a
   * runtime collection check. The previous Iterable signature was a leftover from when
   * Guava's lazy concat/limit results were piped straight into the constructor; that
   * pipeline now lives in PathUtils and materializes its buffer beforehand. PathImpl
   * is internal, so the tighter contract has no downstream cost.
   */
  private final Collection<PathPosition> positions;
  private final PathPosition start;
  private final PathPosition end;

  private final int length;

  public PathImpl(PathPosition start, PathPosition end, Collection<PathPosition> positions) {
    this.start = start;
    this.end = end;
    this.positions = positions;
    this.length = positions.size();
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
  public Iterator<PathPosition> iterator() {
    return positions.iterator();
  }

  @Override
  public void forEach(Consumer<? super PathPosition> action) {
    positions.forEach(action);
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public Collection<PathPosition> collect() {
    return new ArrayList<>(positions);
  }
}
