package de.bsommerfeld.pathetic.api.annotation;

import java.lang.annotation.*;

/** Marks an API as experimental. Experimental APIs can be changed or removed without notice. */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Experimental {}
