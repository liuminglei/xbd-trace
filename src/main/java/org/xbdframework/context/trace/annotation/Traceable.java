package org.xbdframework.context.trace.annotation;

import java.lang.annotation.*;

import org.xbdframework.context.trace.TraceHandler;

/**
 * Describes trace attributes on a method or class.
 *
 * <p>This annotation type is generally directly comparable to xbd's
 * {@link org.xbdframework.context.trace.interceptor.DefaultTraceAttribute}
 * class, and in fact {@link AnnotationTraceAttributeSource} will directly
 * convert the data to the latter class, so that xbd's trace support code
 * does not have to know about annotations. If no rules are relevant to the exception,
 * it will be treated like
 * {@link org.xbdframework.context.trace.interceptor.DefaultTraceAttribute}.
 *
 * <p>For specific information about the semantics of this annotation's attributes,
 * consult the {@link org.xbdframework.context.trace.TraceDefinition} and
 * {@link org.xbdframework.context.trace.interceptor.TraceAttribute} javadocs.
 *
 * @author luas
 * @since 4.3
 * @see org.xbdframework.context.trace.interceptor.TraceAttribute
 * @see org.xbdframework.context.trace.interceptor.DefaultTraceAttribute
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Traceable {

    /**
     * determine whether the current tracing is enabled.
     * @return the current tracing is enabled.
     */
    boolean enabled() default true;

    /**
     * determine whether the current tracing context's logger is enabled.
     * @return the current tracing context's logger is enabled.
     */
    boolean loggerEnabled() default true;

    /**
     * the current tracing context's logger name.
     * @return the current tracing context's logger name.
     */
    String loggerName() default "";

    /**
     * claiming that during tracing, the traced information's handlers.
     * @return {@link org.xbdframework.context.trace.TraceHandler} bean names to be invoked.
     */
    String[] handlerRefs() default {};

    /**
     * claiming that during tracing, the traced information's handlers.
     * @return {@link org.xbdframework.context.trace.TraceHandler} classes to be invoked.
     */
    Class<? extends TraceHandler>[] handlers() default {};

    /**
     * determine whether to record exception stack trace information when exception occurs during tracing.
     * @return whether to record exception stack trace information.
     */
    boolean printStackTrace() default true;

}
