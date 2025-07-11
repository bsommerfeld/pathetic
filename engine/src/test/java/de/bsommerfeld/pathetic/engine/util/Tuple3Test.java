package de.bsommerfeld.pathetic.engine.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Tuple3Test {

    @Test
    void testConstructorAndFieldAccess() {
        // Test with String
        String x = "test1";
        String y = "test2";
        String z = "test3";
        Tuple3<String> tuple = new Tuple3<>(x, y, z);
        
        assertEquals(x, tuple.x);
        assertEquals(y, tuple.y);
        assertEquals(z, tuple.z);
        
        // Test with Integer
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Tuple3<Integer> intTuple = new Tuple3<>(a, b, c);
        
        assertEquals(a, intTuple.x);
        assertEquals(b, intTuple.y);
        assertEquals(c, intTuple.z);
    }
    
    @Test
    void testEqualsWithEqualTuples() {
        Tuple3<String> tuple1 = new Tuple3<>("a", "b", "c");
        Tuple3<String> tuple2 = new Tuple3<>("a", "b", "c");
        
        assertEquals(tuple1, tuple2);
        assertEquals(tuple2, tuple1);
    }
    
    @Test
    void testEqualsWithUnequalTuples() {
        Tuple3<String> tuple1 = new Tuple3<>("a", "b", "c");
        Tuple3<String> tuple2 = new Tuple3<>("a", "b", "d"); // Different z
        Tuple3<String> tuple3 = new Tuple3<>("a", "d", "c"); // Different y
        Tuple3<String> tuple4 = new Tuple3<>("d", "b", "c"); // Different x
        
        assertNotEquals(tuple1, tuple2);
        assertNotEquals(tuple1, tuple3);
        assertNotEquals(tuple1, tuple4);
    }
    
    @Test
    void testEqualsWithNull() {
        Tuple3<String> tuple = new Tuple3<>("a", "b", "c");
        
        assertNotEquals(null, tuple);
    }
    
    @Test
    void testEqualsWithDifferentClass() {
        Tuple3<String> tuple = new Tuple3<>("a", "b", "c");
        String notATuple = "not a tuple";
        
        assertNotEquals(notATuple, tuple);
    }
    
    @Test
    void testHashCode() {
        Tuple3<String> tuple1 = new Tuple3<>("a", "b", "c");
        Tuple3<String> tuple2 = new Tuple3<>("a", "b", "c");
        
        assertEquals(tuple1.hashCode(), tuple2.hashCode());
    }
    
    @Test
    void testWithNullValues() {
        Tuple3<String> tuple1 = new Tuple3<>(null, "b", "c");
        Tuple3<String> tuple2 = new Tuple3<>(null, "b", "c");
        Tuple3<String> tuple3 = new Tuple3<>("a", null, "c");
        
        assertEquals(tuple1, tuple2);
        assertNotEquals(tuple1, tuple3);
        
        // Test hashCode with null values
        assertEquals(tuple1.hashCode(), tuple2.hashCode());
    }
    
    @Test
    void testWithCustomObject() {
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
            
            @Override
            public int hashCode() {
                return value.hashCode();
            }
        }
        
        TestObject obj1 = new TestObject("test1");
        TestObject obj2 = new TestObject("test2");
        TestObject obj3 = new TestObject("test3");
        TestObject obj1Duplicate = new TestObject("test1");
        
        Tuple3<TestObject> tuple1 = new Tuple3<>(obj1, obj2, obj3);
        Tuple3<TestObject> tuple2 = new Tuple3<>(obj1Duplicate, obj2, obj3);
        
        assertEquals(tuple1, tuple2);
        assertEquals(tuple1.hashCode(), tuple2.hashCode());
    }
}