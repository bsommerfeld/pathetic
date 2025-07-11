package de.bsommerfeld.pathetic.engine.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class ErrorLoggerTest {

    @Test
    void testConstructorThrowsException() throws NoSuchMethodException {
        Constructor<ErrorLogger> constructor = ErrorLogger.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        Throwable cause = exception.getCause();
        
        assertTrue(cause instanceof AssertionError);
        assertEquals("ErrorLogger is a utility class and should not be instantiated", cause.getMessage());
    }
    
    @Test
    void testLogFatalErrorWithMessage() {
        String errorMessage = "Test error message";
        IllegalStateException exception = ErrorLogger.logFatalError(errorMessage);
        
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    void testLogFatalErrorWithMessageAndCause() {
        String errorMessage = "Test error message";
        Throwable cause = new RuntimeException("Test cause");
        IllegalStateException exception = ErrorLogger.logFatalError(errorMessage, cause);
        
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testLogFatalErrorWithStacktrace() {
        String errorMessage = "Test error message";
        Throwable cause = new RuntimeException("Test cause");
        IllegalStateException exception = ErrorLogger.logFatalErrorWithStacktrace(errorMessage, cause);
        
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}