package de.bsommerfeld.pathetic.engine.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class ComputingCacheTest {

    @Test
    void testSupplierCalledOnlyOnce() {
        // Setup a counter to track how many times the supplier is called
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = () -> {
            counter.incrementAndGet();
            return 42;
        };
        
        ComputingCache<Integer> cache = new ComputingCache<>(supplier);
        
        // First call should invoke the supplier
        assertEquals(42, cache.get());
        assertEquals(1, counter.get(), "Supplier should be called exactly once on first get()");
        
        // Subsequent calls should not invoke the supplier again
        assertEquals(42, cache.get());
        assertEquals(42, cache.get());
        assertEquals(1, counter.get(), "Supplier should not be called again on subsequent get() calls");
    }
    
    @Test
    void testNullSupplier() {
        // Test with a supplier that returns null
        ComputingCache<String> cache = new ComputingCache<>(() -> null);
        
        // The get method should return null
        assertNull(cache.get());
        
        // Subsequent calls should still return null without re-invoking the supplier
        assertNull(cache.get());
    }
    
    @Test
    void testWithDifferentTypes() {
        // Test with String
        ComputingCache<String> stringCache = new ComputingCache<>(() -> "test");
        assertEquals("test", stringCache.get());
        
        // Test with Boolean
        ComputingCache<Boolean> booleanCache = new ComputingCache<>(() -> true);
        assertTrue(booleanCache.get());
        
        // Test with custom object
        class TestObject {
            private final String value;
            
            TestObject(String value) {
                this.value = value;
            }
            
            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof TestObject)) return false;
                return value.equals(((TestObject) obj).value);
            }
        }
        
        TestObject expected = new TestObject("test");
        ComputingCache<TestObject> objectCache = new ComputingCache<>(() -> expected);
        assertEquals(expected, objectCache.get());
    }
}