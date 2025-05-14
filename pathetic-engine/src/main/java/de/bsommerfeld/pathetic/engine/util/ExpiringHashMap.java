package de.bsommerfeld.pathetic.engine.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ConcurrentHashMap} that removes entries on access if they are expired.
 *
 * <p>The expiration time is 5 minutes. The cleanup itself is only being triggered every 5 minutes on
 * access.
 *
 * @param <K>
 * @param <V>
 */
public class ExpiringHashMap<K, V> extends ConcurrentHashMap<K, ExpiringHashMap.Entry<V>> {

  private static final long EXPIRATION_TIME = 5 * 60 * 1000;
  private long lastCleanupTime = System.currentTimeMillis();

  @Override
  public Entry<V> put(K key, Entry<V> value) {
    removeExpiredEntries();
    return super.put(key, value);
  }

  @Override
  public Entry<V> get(Object key) {
    Entry<V> entry = super.get(key);
    removeExpiredEntries();
    return entry;
  }

  @Override
  public boolean containsKey(Object key) {
    Entry<V> entry = super.get(key);
    removeExpiredEntries();
    return entry != null;
  }

  private void removeExpiredEntries() {
    if (System.currentTimeMillis() - lastCleanupTime < EXPIRATION_TIME) {
      return;
    }
    this.forEach(
        (key, value) -> {
          if (value.isExpired()) {
            this.remove(key);
          }
        });
    lastCleanupTime = System.currentTimeMillis();
  }

  public static class Entry<V> {

    private final V value;
    private final long expirationTime;

    public Entry(V value) {
      this.value = value;
      this.expirationTime = System.currentTimeMillis() + EXPIRATION_TIME;
    }

    public V getValue() {
      return value;
    }

    public boolean isExpired() {
      return System.currentTimeMillis() > expirationTime;
    }
  }
}
