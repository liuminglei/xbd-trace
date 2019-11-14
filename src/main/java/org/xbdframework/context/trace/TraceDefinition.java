package org.xbdframework.context.trace;

/**
 * Interface that defines xbd trace properties.
 *
 * <p>Note that handlerRefs and handlers can only set one. If all values are assigned,
 * handlerRefs will be configured first, and handlers will be ignored.
 *
 * <p>The {@link #isEnabled} () read-only flag} applies to any trace context,
 * whether backed by an actual resource trace or operating non-traceable
 * at the resource level. In the latter case, the flag will only apply to managed
 * resources within the application, such as a Hibernate {@code Session}.
 *
 * @author luas
 * @since 4.3
 * @see org.xbdframework.context.trace.support.DefaultTraceDefinition
 * @see org.xbdframework.context.trace.interceptor.TraceAttribute
 */
public interface TraceDefinition {

    /**
     * determine whether the current tracing is enabled.
     * @return the current tracing is enabled.
     */
    boolean isEnabled();

    /**
     * determine whether the current tracing context's logger is enabled.
     * @return the current tracing context's logger is enabled.
     */
    boolean isLoggerEnabled();

    /**
     * the current tracing context's logger name.
     * @return the current tracing context's logger name.
     */
    String getLoggerName();

    /**
     * claiming that during tracing, the traced information's handlers.
     * @return {@link org.xbdframework.context.trace.TraceHandler} bean names to be invoked.
     */
    String[] getHandlerRefs();

    /**
     * claiming that during tracing, the traced information's handlers.
     * @return {@link org.xbdframework.context.trace.TraceHandler} classes to be invoked.
     */
    Class<? extends TraceHandler>[] getHandlers();

    /**
     * determine whether to record exception stack trace information when exception occurs during tracing.
     * @return whether to record exception stack trace information.
     */
    boolean isPrintStackTrace();
}
