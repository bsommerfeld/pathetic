package de.bsommerfeld.pathetic.engine.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Utility class that provides methods for working with Iterables. This is a replacement for
 * Google's Iterables API.
 */
public final class Iterables {

  // Private constructor to prevent instantiation
  private Iterables() {
    throw new AssertionError("No instances");
  }

  /**
   * Returns the number of elements in the specified iterable.
   *
   * @param iterable the iterable to count elements in
   * @return the number of elements in the iterable
   */
  public static int size(Iterable<?> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).size();
    }
    int count = 0;
    for (Object element : iterable) {
      count++;
    }
    return count;
  }

  /**
   * Returns the last element of the specified iterable.
   *
   * @param iterable the iterable to get the last element from
   * @param <T> the type of elements in the iterable
   * @return the last element of the iterable
   * @throws NoSuchElementException if the iterable is empty
   */
  public static <T> T getLast(Iterable<T> iterable) {
    if (iterable instanceof List) {
      List<T> list = (List<T>) iterable;
      if (list.isEmpty()) {
        throw new NoSuchElementException();
      }
      return list.get(list.size() - 1);
    }

    Iterator<T> iterator = iterable.iterator();
    if (!iterator.hasNext()) {
      throw new NoSuchElementException();
    }

    T last = iterator.next();
    while (iterator.hasNext()) {
      last = iterator.next();
    }
    return last;
  }

  /**
   * Returns an iterable that contains the first {@code limitSize} elements of the specified
   * iterable.
   *
   * @param iterable the iterable to limit
   * @param limitSize the maximum number of elements to include
   * @param <T> the type of elements in the iterable
   * @return an iterable containing at most {@code limitSize} elements from the input iterable
   */
  public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
    if (limitSize < 0) {
      throw new IllegalArgumentException("limit is negative: " + limitSize);
    }

    return () ->
        new Iterator<T>() {
          private final Iterator<T> iterator = iterable.iterator();
          private int remaining = limitSize;

          @Override
          public boolean hasNext() {
            return remaining > 0 && iterator.hasNext();
          }

          @Override
          public T next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            remaining--;
            return iterator.next();
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
  }

  /**
   * Returns an iterable that concatenates two iterables.
   *
   * @param a the first iterable
   * @param b the second iterable
   * @param <T> the type of elements in the iterables
   * @return an iterable containing all elements from both input iterables
   */
  public static <T> Iterable<T> concat(
      final Iterable<? extends T> a, final Iterable<? extends T> b) {
    return () ->
        new Iterator<T>() {
          private final Iterator<? extends T> iteratorA = a.iterator();
          private final Iterator<? extends T> iteratorB = b.iterator();

          @Override
          public boolean hasNext() {
            return iteratorA.hasNext() || iteratorB.hasNext();
          }

          @Override
          public T next() {
            if (iteratorA.hasNext()) {
              return iteratorA.next();
            }
            if (iteratorB.hasNext()) {
              return iteratorB.next();
            }
            throw new NoSuchElementException();
          }
        };
  }
}
