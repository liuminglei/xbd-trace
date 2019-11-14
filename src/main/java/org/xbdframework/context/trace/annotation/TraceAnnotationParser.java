package org.xbdframework.context.trace.annotation;

import java.lang.reflect.AnnotatedElement;

import org.xbdframework.context.trace.interceptor.TraceAttribute;

/**
 * Strategy interface for parsing known trace annotation types.
 * {@link AnnotationTraceAttributeSource} delegates to such
 * parsers for supporting specific annotation types such as the
 * {@link org.xbdframework.context.trace.annotation.Traceable}.
 *
 * @author luas
 * @since 4.3
 * @see AnnotationTraceAttributeSource
 * @see DefaultTraceAnnotationParser
 */
public interface TraceAnnotationParser {

    /**
     * Parse the trace attribute for the given method or class,
     * based on a known annotation type.
     * <p>This essentially parses a known trace annotation into Spring's
     * metadata attribute class. Returns {@code null} if the method/class
     * is not traced.
     * @param ae the annotated method or class
     * @return TraceAttribute the configured trace attribute,
     * or {@code null} if none was found
     * @see AnnotationTraceAttributeSource#determineTraceAttribute
     */
    TraceAttribute parseTraceAnnotation(AnnotatedElement ae);

}
