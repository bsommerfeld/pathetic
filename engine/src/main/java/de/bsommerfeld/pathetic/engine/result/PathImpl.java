package de.bsommerfeld.pathetic.engine.result;

import de.bsommerfeld.pathetic.api.pathing.result.Path;
import de.bsommerfeld.pathetic.api.util.ParameterizedSupplier;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.util.ErrorLogger;
import de.bsommerfeld.pathetic.engine.util.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

  @Override
  public Path interpolate(double resolution) {
    if (resolution <= 0) throw new IllegalArgumentException("Resolution cannot be <= 0");

    List<PathPosition> enlargedPositions = new ArrayList<>();

    PathPosition previousPosition = null;
    for (PathPosition position : positions) {
      if (previousPosition != null)
        interpolateBetweenPositions(previousPosition, position, resolution, enlargedPositions);

      enlargedPositions.add(position);
      previousPosition = position;
    }

    return new PathImpl(start, end, enlargedPositions);
  }

  private void interpolateBetweenPositions(
      PathPosition startPosition,
      PathPosition endPosition,
      double resolution,
      List<PathPosition> result) {
    double distance = startPosition.distance(endPosition);
    int steps = (int) Math.ceil(distance / resolution);

    for (int i = 1; i <= steps; i++) {
      double progress = (double) i / steps;

      PathPosition interpolatedPosition = startPosition.interpolate(endPosition, progress);
      result.add(interpolatedPosition);
    }
  }

  @Override
  public Path simplify(double epsilon) {
    try {
      validateEpsilon(epsilon);

      Set<PathPosition> simplifiedPositions =
          new LinkedHashSet<>(filterPositionsByEpsilon(epsilon));

      return new PathImpl(start, end, simplifiedPositions);
    } catch (IllegalArgumentException e) {
      throw ErrorLogger.logFatalError("Invalid epsilon value for path simplification", e);
    }
  }

  private Set<PathPosition> filterPositionsByEpsilon(double epsilon) {
    Set<PathPosition> filteredPositions = new LinkedHashSet<>();

    int index = 0;
    for (PathPosition pathPosition : positions) {
      int stride = Math.max(1, (int) Math.round(1.0 / epsilon));
      if (index % stride == 0) {
        filteredPositions.add(pathPosition);
      }
      index++;
    }

    return filteredPositions;
  }

  private void validateEpsilon(double epsilon) {
    if (epsilon <= 0.0 || epsilon > 1.0) {
      throw ErrorLogger.logFatalError("Epsilon must be in the range of 0.0 to 1.0, inclusive");
    }
  }

  @Override
  public Path join(Path path) {
    return new PathImpl(start, path.getEnd(), Iterables.concat(positions, path));
  }

  @Override
  public Path trim(int length) {
    Iterable<PathPosition> limitedPositions = Iterables.limit(positions, length);
    return new PathImpl(start, Iterables.getLast(limitedPositions), limitedPositions);
  }

  @Override
  public Path mutatePositions(ParameterizedSupplier<PathPosition> mutator) {
    List<PathPosition> positionList = new LinkedList<>();
    applyMutator(mutator, positionList);
    return new PathImpl(
        positionList.get(0), positionList.get(positionList.size() - 1), positionList);
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

  private void applyMutator(
      ParameterizedSupplier<PathPosition> mutator, List<PathPosition> positionList) {
    for (PathPosition position : this.positions) positionList.add(mutator.accept(position));
  }
}
