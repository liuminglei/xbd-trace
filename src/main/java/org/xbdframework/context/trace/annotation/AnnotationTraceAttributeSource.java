package org.xbdframework.context.trace.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.xbdframework.context.trace.interceptor.AbstractFallbackTraceAttributeSource;
import org.xbdframework.context.trace.interceptor.TraceAttribute;

import org.springframework.util.Assert;

/**
 * Implementation of the {@link org.xbdframework.context.trace.interceptor.TraceAttributeSource}
 * interface for working with trace metadata in JDK 1.5+ annotation format.
 *
 * <p>This class reads {@link Traceable} annotation and exposes corresponding
 * trace attributes to xbd's trace infrastructure.
 * This class may also serve as base class for a custom TraceAttributeSource,
 * or get customized through {@link TraceAnnotationParser} strategies.
 *
 * @author luas
 * @since 4.3
 * @see Traceable
 * @see TraceAnnotationParser
 * @see DefaultTraceAnnotationParser
 * @see org.xbdframework.context.trace.interceptor.TraceInterceptor#setTraceAttributeSource
 * @see org.xbdframework.context.trace.interceptor.TraceProxyFactoryBean#setTraceAttributeSource
 */
@SuppressWarnings("serial")
public class AnnotationTraceAttributeSource extends AbstractFallbackTraceAttributeSource implements Serializable {

    private final boolean publicMethodsOnly;

    private final Set<TraceAnnotationParser> annotationParsers;

    /**
     * Create a default AnnotationTransactionAttributeSource, supporting
     * public methods that carry the {@code Traceable} annotation.
     */
    public AnnotationTraceAttributeSource() {
        this(true);
    }

    /**
     * Create a custom AnnotationTraceAttributeSource, supporting
     * public methods that carry the {@code Traceable} annotation.
     * @param publicMethodsOnly whether to support public methods that carry
     * the {@code Traceable} annotation only (typically for use
     * with proxy-based AOP), or protected/private methods as well
     * (typically used with AspectJ class weaving)
     */
    public AnnotationTraceAttributeSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        this.annotationParsers = new LinkedHashSet<TraceAnnotationParser>(2);
        this.annotationParsers.add(new DefaultTraceAnnotationParser());
    }

    /**
     * Create a custom AnnotationTraceAttributeSource.
     * @param annotationParser the TraceAnnotationParser to use
     */
    public AnnotationTraceAttributeSource(TraceAnnotationParser annotationParser) {
        this.publicMethodsOnly = true;
        Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
        this.annotationParsers = Collections.singleton(annotationParser);
    }

    /**
     * Create a custom AnnotationTraceAttributeSource.
     * @param annotationParsers the TraceAnnotationParsers to use
     */
    public AnnotationTraceAttributeSource(TraceAnnotationParser... annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one TraceAnnotationParser needs to be specified");
        Set<TraceAnnotationParser> parsers = new LinkedHashSet<TraceAnnotationParser>(annotationParsers.length);
        Collections.addAll(parsers, annotationParsers);
        this.annotationParsers = parsers;
    }

    /**
     * Create a custom AnnotationTraceAttributeSource.
     * @param annotationParsers the TraceAnnotationParsers to use
     */
    public AnnotationTraceAttributeSource(Set<TraceAnnotationParser> annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
        this.annotationParsers = annotationParsers;
    }


    @Override
    protected TraceAttribute findTraceAttribute(Method method) {
        return determineTraceAttribute(method);
    }

    @Override
    protected TraceAttribute findTraceAttribute(Class<?> clazz) {
        return determineTraceAttribute(clazz);
    }

    /**
     * Determine the traceable attribute for the given method or class.
     * <p>This implementation delegates to configured
     * {@link TraceAnnotationParser TraceAnnotationParsers}
     * for parsing known annotations into Spring's metadata attribute class.
     * Returns {@code null} if it's not traced.
     * <p>Can be overridden to support custom annotations that carry trace metadata.
     * @param ae the annotated method or class
     * @return TraceAttribute the configured trace attribute,
     * or {@code null} if none was found
     */
    protected TraceAttribute determineTraceAttribute(AnnotatedElement ae) {
        if (ae.getAnnotations().length > 0) {
            for (TraceAnnotationParser annotationParser : this.annotationParsers) {
                TraceAttribute attr = annotationParser.parseTraceAnnotation(ae);
                if (attr != null) {
                    return attr;
                }
            }
        }
        return null;
    }

    /**
     * By default, only public methods can be made traceable.
     */
    @Override
    protected boolean allowPublicMethodsOnly() {
        return this.publicMethodsOnly;
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationTraceAttributeSource)) {
            return false;
        }
        AnnotationTraceAttributeSource otherTas = (AnnotationTraceAttributeSource) other;
        return (this.annotationParsers.equals(otherTas.annotationParsers) &&
                this.publicMethodsOnly == otherTas.publicMethodsOnly);
    }

    @Override
    public int hashCode() {
        return this.annotationParsers.hashCode();
    }

}
