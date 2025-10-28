package de.bsommerfeld.pathetic.engine.result;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.util.ParameterizedSupplier;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.util.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

public class PathImpl implements Path {

  private final Iterable<PathPosition> positions;
  private final PathPosition start;
  private final PathPosition end;

  private final int length;

  public PathImpl(PathPosition start, PathPosition end, Iterable<PathPosition> positions) {
    this.start = start;
    this.end = end;
    this.positions = positions;
    this.length = Iterables.size(positions);
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

  /**
   * @deprecated Delegates to {@link PathUtils} - you should use that too!
   */
  @Override
  public Path interpolate(double resolution) {
    return PathUtils.interpolate(this, resolution);
  }

  /**
   * @deprecated Delegates to {@link PathUtils} - you should use that too!
   */
  @Override
  public Path simplify(double epsilon) {
    return PathUtils.simplify(this, epsilon);
  }

  /**
   * @deprecated Delegates to {@link PathUtils} - you should use that too!
   */
  @Override
  public Path join(Path path) {
    return PathUtils.join(this, path);
  }

  /**
   * @deprecated Delegates to {@link PathUtils} - you should use that too!
   */
  @Override
  public Path trim(int length) {
    return PathUtils.trim(this, length);
  }

  /**
   * @deprecated Delegates to {@link PathUtils} - you should use that too!
   */
  @Override
  public Path mutatePositions(ParameterizedSupplier<PathPosition> mutator) {
    return PathUtils.mutatePositions(this, mutator);
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public Collection<PathPosition> collect() {
    Collection<PathPosition> collection = new ArrayList<>(length);
    positions.forEach(collection::add);
    return collection;
  }
}
