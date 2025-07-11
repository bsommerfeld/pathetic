package de.bsommerfeld.pathetic.engine.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

class ExpiringHashMapTest {

    @Test
    void testBasicOperations() {
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
        map.put("key1", new ExpiringHashMap.Entry<>("value1"));

        ExpiringHashMap.Entry<String> entry = map.get("key1");
        assertNotNull(entry);
        assertEquals("value1", entry.getValue());

        assertTrue(map.containsKey("key1"));
        assertFalse(map.containsKey("key2"));
    }

    @Test
    void testExpiration() throws InterruptedException {
        // Create a map with a very short expiration time for testing
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>(100, TimeUnit.MILLISECONDS);
        map.put("key1", new ExpiringHashMap.Entry<>("value1", 100));

        // Verify the entry exists
        assertTrue(map.containsKey("key1"));

        // Wait for the entry to expire
        Thread.sleep(150);

        // Force cleanup by accessing the map
        map.get("key2"); // This should trigger cleanup

        // Verify the entry has been removed
        assertFalse(map.containsKey("key1"));
    }

    @Test
    void testCustomExpirationTime() throws InterruptedException {
        // Create a map with default expiration time
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();

        // Add an entry with custom expiration time
        map.putWithExpiration("key1", "value1", 100, TimeUnit.MILLISECONDS);

        // Verify the entry exists
        assertTrue(map.containsKey("key1"));

        // Wait for the entry to expire
        Thread.sleep(150);

        // Force cleanup by accessing the map
        map.get("key2"); // This should trigger cleanup

        // Verify the entry has been removed
        assertFalse(map.containsKey("key1"));
    }

    @Test
    void testDirectValueAccess() {
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
        map.putValue("key1", "value1");

        // Test getValue
        assertEquals("value1", map.getValue("key1"));
        assertNull(map.getValue("key2"));

        // Test containsRawValue
        assertTrue(map.containsRawValue("value1"));
        assertFalse(map.containsRawValue("value2"));

        // Test containsValue
        assertTrue(map.containsValue("value1"));
        assertFalse(map.containsValue("value2"));
    }

    @Test
    void testPutIfAbsent() {
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();

        // First put should succeed
        assertNull(map.putValueIfAbsent("key1", "value1"));

        // Second put should return existing value
        assertEquals("value1", map.putValueIfAbsent("key1", "value2"));

        // Value should not be changed
        assertEquals("value1", map.getValue("key1"));
    }

    @Test
    void testRemove() {
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
        map.putValue("key1", "value1");

        // Remove should return the value
        assertEquals("value1", map.removeValue("key1"));

        // Key should be removed
        assertFalse(map.containsKey("key1"));

        // Removing non-existent key should return null
        assertNull(map.removeValue("key2"));
    }

    @Test
    void testClear() {
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
        map.putValue("key1", "value1");
        map.putValue("key2", "value2");

        // Clear should remove all entries
        map.clear();

        // Map should be empty
        assertEquals(0, map.size());
        assertFalse(map.containsKey("key1"));
        assertFalse(map.containsKey("key2"));
    }

    @Test
    void testCollectionMethods() {
        ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
        map.putValue("key1", "value1");
        map.putValue("key2", "value2");

        // Test keySetWithCleanup
        assertEquals(2, map.keySetWithCleanup().size());
        assertTrue(map.keySetWithCleanup().contains("key1"));
        assertTrue(map.keySetWithCleanup().contains("key2"));

        // Test valuesWithCleanup
        assertEquals(2, map.valuesWithCleanup().size());

        // Test entrySetWithCleanup
        assertEquals(2, map.entrySetWithCleanup().size());
    }

    @Test
    void testEntryMethods() {
        // Test Entry constructor with expiration time
        ExpiringHashMap.Entry<String> entry = new ExpiringHashMap.Entry<>("test", 1000);
        assertEquals("test", entry.getValue());
        assertFalse(entry.isExpired());
        assertTrue(entry.getTimeToLive() > 0);
        assertEquals(entry.getExpirationTime(), System.currentTimeMillis() + 1000, 100);

        // Test Entry toString
        assertTrue(entry.toString().contains("test"));
        assertTrue(entry.toString().contains("expires in"));

        // Test Entry equals and hashCode
        ExpiringHashMap.Entry<String> entry2 = new ExpiringHashMap.Entry<>("test", 2000);
        ExpiringHashMap.Entry<String> entry3 = new ExpiringHashMap.Entry<>("different", 1000);

        assertEquals(entry, entry2);
        assertEquals(entry.hashCode(), entry2.hashCode());
        assertNotEquals(entry, entry3);
        assertNotEquals(entry.hashCode(), entry3.hashCode());
    }
}
