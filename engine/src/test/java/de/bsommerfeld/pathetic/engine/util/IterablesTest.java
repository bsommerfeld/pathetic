package de.bsommerfeld.pathetic.engine.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class IterablesTest {

    @Test
    void testConstructorThrowsException() throws NoSuchMethodException {
        Constructor<Iterables> constructor = Iterables.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        Throwable cause = exception.getCause();
        
        assertTrue(cause instanceof AssertionError);
        assertEquals("No instances", cause.getMessage());
    }
    
    @Test
    void testSizeWithCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Iterables.size(list));
        
        List<Integer> emptyList = Collections.emptyList();
        assertEquals(0, Iterables.size(emptyList));
    }
    
    @Test
    void testSizeWithCustomIterable() {
        Iterable<Integer> iterable = () -> {
            return new Iterator<Integer>() {
                private int count = 0;
                
                @Override
                public boolean hasNext() {
                    return count < 5;
                }
                
                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return count++;
                }
            };
        };
        
        assertEquals(5, Iterables.size(iterable));
    }
    
    @Test
    void testGetLastWithList() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", Iterables.getLast(list));
        
        List<String> singletonList = Collections.singletonList("only");
        assertEquals("only", Iterables.getLast(singletonList));
    }
    
    @Test
    void testGetLastWithCustomIterable() {
        Iterable<Integer> iterable = () -> {
            return new Iterator<Integer>() {
                private int count = 0;
                private final int max = 5;
                
                @Override
                public boolean hasNext() {
                    return count < max;
                }
                
                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return count++;
                }
            };
        };
        
        assertEquals(4, Iterables.getLast(iterable));
    }
    
    @Test
    void testGetLastWithEmptyIterableThrowsException() {
        List<String> emptyList = Collections.emptyList();
        assertThrows(NoSuchElementException.class, () -> Iterables.getLast(emptyList));
        
        Iterable<Object> emptyIterable = Collections::emptyIterator;
        assertThrows(NoSuchElementException.class, () -> Iterables.getLast(emptyIterable));
    }
    
    @Test
    void testLimitWithValidSize() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        
        // Limit to 3 elements
        Iterable<String> limited = Iterables.limit(list, 3);
        List<String> result = new ArrayList<>();
        limited.forEach(result::add);
        
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
        
        // Limit to 0 elements
        limited = Iterables.limit(list, 0);
        result = new ArrayList<>();
        limited.forEach(result::add);
        
        assertTrue(result.isEmpty());
        
        // Limit larger than list size
        limited = Iterables.limit(list, 10);
        result = new ArrayList<>();
        limited.forEach(result::add);
        
        assertEquals(5, result.size());
        assertEquals("a", result.get(0));
        assertEquals("e", result.get(4));
    }
    
    @Test
    void testLimitWithNegativeSizeThrowsException() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThrows(IllegalArgumentException.class, () -> Iterables.limit(list, -1));
    }
    
    @Test
    void testConcat() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("d", "e", "f");
        
        Iterable<String> concatenated = Iterables.concat(list1, list2);
        List<String> result = new ArrayList<>();
        concatenated.forEach(result::add);
        
        assertEquals(6, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
        assertEquals("d", result.get(3));
        assertEquals("e", result.get(4));
        assertEquals("f", result.get(5));
    }
    
    @Test
    void testConcatWithEmptyIterables() {
        List<String> emptyList = Collections.emptyList();
        List<String> list = Arrays.asList("a", "b", "c");
        
        // First iterable empty
        Iterable<String> concatenated = Iterables.concat(emptyList, list);
        List<String> result = new ArrayList<>();
        concatenated.forEach(result::add);
        
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("c", result.get(2));
        
        // Second iterable empty
        concatenated = Iterables.concat(list, emptyList);
        result = new ArrayList<>();
        concatenated.forEach(result::add);
        
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("c", result.get(2));
        
        // Both iterables empty
        concatenated = Iterables.concat(emptyList, emptyList);
        result = new ArrayList<>();
        concatenated.forEach(result::add);
        
        assertTrue(result.isEmpty());
    }
}