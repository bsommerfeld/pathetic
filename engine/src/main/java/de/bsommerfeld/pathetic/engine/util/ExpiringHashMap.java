package de.bsommerfeld.pathetic.engine.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link ConcurrentHashMap} that removes entries on access if they are expired.
 *
 * <p>The default expiration time is 5 minutes. The cleanup itself is only being triggered every 5 minutes on
 * access by default, but both values can be configured.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ExpiringHashMap<K, V> extends ConcurrentHashMap<K, ExpiringHashMap.Entry<V>> {

  /** Default expiration time in milliseconds (5 minutes) */
  private static final long DEFAULT_EXPIRATION_TIME_MS = 5 * 60 * 1000;

  /** Default cleanup interval in milliseconds (5 minutes) */
  private static final long DEFAULT_CLEANUP_INTERVAL_MS = 5 * 60 * 1000;

  /** The expiration time for entries in milliseconds */
  private final long expirationTimeMs;

  /** The interval between cleanup operations in milliseconds */
  private final long cleanupIntervalMs;

  /** The timestamp of the last cleanup operation */
  private long lastCleanupTime;

  /**
   * Creates a new ExpiringHashMap with default expiration time (5 minutes) and cleanup interval (5 minutes).
   */
  public ExpiringHashMap() {
    this(DEFAULT_EXPIRATION_TIME_MS, DEFAULT_CLEANUP_INTERVAL_MS);
  }

  /**
   * Creates a new ExpiringHashMap with the specified expiration time and default cleanup interval (5 minutes).
   *
   * @param expirationTime the expiration time for entries
   * @param timeUnit the time unit of the expiration time
   */
  public ExpiringHashMap(long expirationTime, TimeUnit timeUnit) {
    this(timeUnit.toMillis(expirationTime), DEFAULT_CLEANUP_INTERVAL_MS);
  }

  /**
   * Creates a new ExpiringHashMap with the specified expiration time and cleanup interval.
   *
   * @param expirationTime the expiration time for entries
   * @param cleanupInterval the interval between cleanup operations
   * @param timeUnit the time unit of both the expiration time and cleanup interval
   */
  public ExpiringHashMap(long expirationTime, long cleanupInterval, TimeUnit timeUnit) {
    this(timeUnit.toMillis(expirationTime), timeUnit.toMillis(cleanupInterval));
  }

  /**
   * Creates a new ExpiringHashMap with the specified expiration time and cleanup interval in milliseconds.
   *
   * @param expirationTimeMs the expiration time for entries in milliseconds
   * @param cleanupIntervalMs the interval between cleanup operations in milliseconds
   */
  private ExpiringHashMap(long expirationTimeMs, long cleanupIntervalMs) {
    this.expirationTimeMs = expirationTimeMs;
    this.cleanupIntervalMs = cleanupIntervalMs;
    this.lastCleanupTime = System.currentTimeMillis();
  }

  /**
   * Returns the expiration time for entries in milliseconds.
   *
   * @return the expiration time in milliseconds
   */
  public long getExpirationTimeMs() {
    return expirationTimeMs;
  }

  /**
   * Returns the cleanup interval in milliseconds.
   *
   * @return the cleanup interval in milliseconds
   */
  public long getCleanupIntervalMs() {
    return cleanupIntervalMs;
  }

  /**
   * Puts a value in the map with the default expiration time.
   *
   * @param key the key
   * @param value the value
   * @return the previous value associated with the key, or null if there was no mapping
   */
  public V putValue(K key, V value) {
    Entry<V> previous = put(key, new Entry<>(value, expirationTimeMs));
    return previous != null ? previous.getValue() : null;
  }

  /**
   * Puts a value in the map with a custom expiration time.
   *
   * @param key the key
   * @param value the value
   * @param expirationTime the expiration time
   * @param timeUnit the time unit of the expiration time
   * @return the previous value associated with the key, or null if there was no mapping
   */
  public V putWithExpiration(K key, V value, long expirationTime, TimeUnit timeUnit) {
    Entry<V> previous = put(key, new Entry<>(value, timeUnit.toMillis(expirationTime)));
    return previous != null ? previous.getValue() : null;
  }

  /**
   * Returns the value associated with the key, or null if the key is not in the map or the value has expired.
   *
   * @param key the key
   * @return the value associated with the key, or null if the key is not in the map or the value has expired
   */
  public V getValue(Object key) {
    Entry<V> entry = get(key);
    return entry != null ? entry.getValue() : null;
  }

  /**
   * Puts a value in the map if the key is not already associated with a value or if the current value has expired.
   *
   * @param key the key
   * @param value the value
   * @return the previous value associated with the key, or null if there was no mapping or the value had expired
   */
  public V putValueIfAbsent(K key, V value) {
    Entry<V> entry = get(key);
    if (entry == null || entry.isExpired()) {
      Entry<V> newEntry = new Entry<>(value, expirationTimeMs);
      entry = putIfAbsent(key, newEntry);
      if (entry == null || entry.isExpired()) {
        // If there was no previous entry or it was expired, replace it and return null
        if (entry != null && entry.isExpired()) {
          // Make sure we replace the expired entry
          replace(key, entry, newEntry);
        }
        return null;
      }
    }
    return entry.getValue();
  }

  /**
   * Removes the mapping for the specified key and returns the value that was associated with the key.
   *
   * @param key the key
   * @return the value associated with the key, or null if the key was not in the map
   */
  public V removeValue(Object key) {
    Entry<V> entry = remove(key);
    return entry != null ? entry.getValue() : null;
  }

  /**
   * Returns true if the map contains a mapping for the specified key and the value has not expired.
   *
   * @param key the key
   * @return true if the map contains a mapping for the specified key and the value has not expired
   */
  @Override
  public boolean containsKey(Object key) {
    Entry<V> entry = super.get(key);
    if (entry != null && entry.isExpired()) {
      // Remove expired entry
      super.remove(key, entry);
      return false;
    }
    removeExpiredEntries();
    return entry != null && !entry.isExpired();
  }

  /**
   * Returns true if the map contains a mapping with the specified raw value and the entry has not expired.
   *
   * @param value the raw value (not wrapped in an Entry)
   * @return true if the map contains a mapping with the specified value and the entry has not expired
   */
  public boolean containsRawValue(V value) {
    removeExpiredEntries();
    for (Entry<V> entry : values()) {
      if (!entry.isExpired() && Objects.equals(entry.getValue(), value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    if (value instanceof Entry) {
      return super.containsValue(value);
    }
    // If it's not an Entry, check if any Entry contains this value
    for (Entry<V> entry : values()) {
      if (!entry.isExpired() && Objects.equals(entry.getValue(), value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Entry<V> put(K key, Entry<V> value) {
    removeExpiredEntries();
    return super.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends Entry<V>> m) {
    removeExpiredEntries();
    super.putAll(m);
  }

  @Override
  public Entry<V> putIfAbsent(K key, Entry<V> value) {
    removeExpiredEntries();
    Entry<V> existing = super.get(key);
    if (existing != null && existing.isExpired()) {
      // Remove expired entry
      super.remove(key, existing);
      existing = null;
    }
    return existing != null ? existing : super.putIfAbsent(key, value);
  }

  @Override
  public Entry<V> get(Object key) {
    Entry<V> entry = super.get(key);
    if (entry != null && entry.isExpired()) {
      // Remove expired entry
      super.remove(key, entry);
      return null;
    }
    removeExpiredEntries();
    return entry;
  }

  @Override
  public Entry<V> remove(Object key) {
    removeExpiredEntries();
    return super.remove(key);
  }

  @Override
  public boolean remove(Object key, Object value) {
    removeExpiredEntries();
    return super.remove(key, value);
  }

  @Override
  public Entry<V> replace(K key, Entry<V> value) {
    removeExpiredEntries();
    return super.replace(key, value);
  }

  @Override
  public boolean replace(K key, Entry<V> oldValue, Entry<V> newValue) {
    removeExpiredEntries();
    return super.replace(key, oldValue, newValue);
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super Entry<V>, ? extends Entry<V>> function) {
    removeExpiredEntries();
    super.replaceAll(function);
  }

  @Override
  public Entry<V> computeIfAbsent(K key, Function<? super K, ? extends Entry<V>> mappingFunction) {
    removeExpiredEntries();
    return super.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public Entry<V> computeIfPresent(K key, BiFunction<? super K, ? super Entry<V>, ? extends Entry<V>> remappingFunction) {
    removeExpiredEntries();
    return super.computeIfPresent(key, remappingFunction);
  }

  @Override
  public Entry<V> compute(K key, BiFunction<? super K, ? super Entry<V>, ? extends Entry<V>> remappingFunction) {
    removeExpiredEntries();
    return super.compute(key, remappingFunction);
  }

  @Override
  public Entry<V> merge(K key, Entry<V> value, BiFunction<? super Entry<V>, ? super Entry<V>, ? extends Entry<V>> remappingFunction) {
    removeExpiredEntries();
    return super.merge(key, value, remappingFunction);
  }

  /**
   * Removes expired entries and then returns the key set.
   *
   * @return the key set
   */
  public Set<K> keySetWithCleanup() {
    removeExpiredEntries();
    return super.keySet();
  }

  /**
   * Removes expired entries and then returns the values.
   *
   * @return the values
   */
  public Collection<Entry<V>> valuesWithCleanup() {
    removeExpiredEntries();
    return super.values();
  }

  /**
   * Removes expired entries and then returns the entry set.
   *
   * @return the entry set
   */
  public Set<Map.Entry<K, Entry<V>>> entrySetWithCleanup() {
    removeExpiredEntries();
    return super.entrySet();
  }

  /**
   * Removes all expired entries from the map if the cleanup interval has elapsed.
   */
  private void removeExpiredEntries() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastCleanupTime < cleanupIntervalMs) {
      return;
    }

    // Create a copy of the keys to avoid ConcurrentModificationException
    for (K key : keySet()) {
      Entry<V> value = super.get(key);
      if (value != null && value.isExpired()) {
        super.remove(key, value);
      }
    }

    lastCleanupTime = currentTime;
  }

  /**
   * A wrapper for values stored in the ExpiringHashMap that includes an expiration time.
   *
   * @param <V> the type of the value
   */
  public static class Entry<V> {
    /** The wrapped value */
    private final V value;

    /** The timestamp when this entry expires */
    private final long expirationTime;

    /**
     * Creates a new Entry with the specified value and default expiration time.
     *
     * @param value the value
     */
    public Entry(V value) {
      this(value, DEFAULT_EXPIRATION_TIME_MS);
    }

    /**
     * Creates a new Entry with the specified value and expiration time.
     *
     * @param value the value
     * @param expirationTimeMs the expiration time in milliseconds from now
     */
    public Entry(V value, long expirationTimeMs) {
      this.value = value;
      this.expirationTime = System.currentTimeMillis() + expirationTimeMs;
    }

    /**
     * Returns the wrapped value.
     *
     * @return the value
     */
    public V getValue() {
      return value;
    }

    /**
     * Returns the expiration time of this entry in milliseconds since the epoch.
     *
     * @return the expiration time
     */
    public long getExpirationTime() {
      return expirationTime;
    }

    /**
     * Returns true if this entry has expired.
     *
     * @return true if this entry has expired
     */
    public boolean isExpired() {
      return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Returns the time remaining until this entry expires in milliseconds.
     *
     * @return the time remaining in milliseconds, or 0 if the entry has expired
     */
    public long getTimeToLive() {
      long ttl = expirationTime - System.currentTimeMillis();
      return Math.max(0, ttl);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Entry<?> entry = (Entry<?>) o;
      return Objects.equals(value, entry.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Entry{value=" + value + ", expires in " + getTimeToLive() + "ms}";
    }
  }
}
