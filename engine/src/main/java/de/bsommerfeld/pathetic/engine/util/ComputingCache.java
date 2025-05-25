package de.bsommerfeld.pathetic.engine.util;

import java.util.function.Supplier;

public class ComputingCache<T> {

  private final Supplier<T> supplier;
  private T value;

  public ComputingCache(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  public T get() {
    if (this.value == null) {
      this.value = this.supplier.get();
    }
    return this.value;
  }
}
