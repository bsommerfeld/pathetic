package de.bsommerfeld.pathetic.engine.util;

import java.util.Objects;

public class Tuple3<T> {
  public final T x;
  public final T y;
  public final T z;

  public Tuple3(T x, T y, T z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tuple3<?> tuple3 = (Tuple3<?>) o;
    return Objects.equals(x, tuple3.x)
        && Objects.equals(y, tuple3.y)
        && Objects.equals(z, tuple3.z);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z);
  }
}
