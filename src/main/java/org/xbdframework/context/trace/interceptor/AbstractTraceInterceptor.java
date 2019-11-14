package org.xbdframework.context.trace.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.apache.commons.logging.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;

/**
 * Base {@code MethodInterceptor} implementation for tracing.
 *
 * <p>By default, log messages are written to the log for the interceptor class,
 * not the class which is being intercepted. Setting the {@code useDynamicLogger}
 * bean property to {@code true} causes all log messages to be written to
 * the {@code Log} for the target class being intercepted.
 *
 * <p>Subclasses must implement the {@code invokeUnderTrace} method, which
 * is invoked by this class ONLY when a particular invocation SHOULD be traced.
 * Subclasses should write to the {@code Log} instance provided.
 *
 * @author luas
 * @since 4.3
 * @see #setUseDynamicLogger
 * @see #invokeUnderTrace(org.aopalliance.intercept.MethodInvocation, org.slf4j.Logger)
 */
@SuppressWarnings("serial")
public abstract class AbstractTraceInterceptor implements MethodInterceptor, Serializable {

    @Nullable
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * whether the interceptor should kick in, that is,
     * whether the {@code invokeUnderTrace} method should be called.
     * <p>Subclasses or configuration can set this to apply the
     * interceptor in other cases as well.
     */
    private boolean enabled = true;

    /**
     * Determine whether the given {@link Logger} instance is enabled.
     * <p>Default is {@code true} when the "trace" level is enabled.
     * Subclasses or configuration can set this to change the level under which 'tracing' occurs.
     */
    private boolean loggerEnabled = true;

    /**
     * Indicates whether or not proxy class names should be hidden when using dynamic loggers.
     * @see #setUseDynamicLogger
     */
    private boolean hideProxyClassNames = false;

    /**
     * Indicates whether to pass an exception to the logger.
     * @see #writeToLog(Logger, String, Throwable)
     */
    private boolean printStackTrace = true;

    /**
     * Determines whether or not logging is enabled for the particular {@code MethodInvocation}.
     * If not, the method invocation proceeds as normal, otherwise the method invocation is passed
     * to the {@code invokeUnderTrace} method for handling.
     * @see #invokeUnderTrace(org.aopalliance.intercept.MethodInvocation, org.slf4j.Logger)
     */
    @Override
    @Nullable
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Logger logger = getLoggerForInvocation(invocation);
        if (isEnabled()) {
            return invokeUnderTrace(invocation, logger);
        }
        else {
            return invocation.proceed();
        }
    }

    /**
     * Subclasses must override this method to perform any tracing around the
     * supplied {@code MethodInvocation}. Subclasses are responsible for
     * ensuring that the {@code MethodInvocation} actually executes by
     * calling {@code MethodInvocation.proceed()}.
     * <p>By default, the passed-in {@code Log} instance will have log level
     * "trace" enabled. Subclasses do not have to check for this again, unless
     * they overwrite the {@code isInterceptorEnabled} method to modify
     * the default behavior, and may delegate to {@code writeToLog} for actual
     * messages to be written.
     * @param logger the {@code Log} to write trace messages to
     * @return the result of the call to {@code MethodInvocation.proceed()}
     * @throws Throwable if the call to {@code MethodInvocation.proceed()}
     * encountered any errors
     * @see #isLoggerEnabled
     * @see #writeToLog(Logger, String)
     * @see #writeToLog(Logger, String, Throwable)
     */
    @Nullable
    protected abstract Object invokeUnderTrace(MethodInvocation invocation, Logger logger) throws Throwable;

    /**
     * Set whether to use a dynamic logger or a static logger.
     * Default is a static logger for this trace interceptor.
     * <p>Used to determine which {@code Log} instance should be used to write
     * log messages for a particular method invocation: a dynamic one for the
     * {@code Class} getting called, or a static one for the {@code Class}
     * of the trace interceptor.
     * <p><b>NOTE:</b> Specify either this property or "loggerName", not both.
     * @see #getLoggerForInvocation(org.aopalliance.intercept.MethodInvocation)
     */
    public void setUseDynamicLogger(boolean useDynamicLogger) {
        // Release default logger if it is not being used.
        this.logger = (useDynamicLogger ? null : LoggerFactory.getLogger(getClass()));
    }

    /**
     * Set the name of the logger to use. The name will be passed to the
     * underlying logger implementation through Commons Logging, getting
     * interpreted as log category according to the logger's configuration.
     * <p>This can be specified to not log into the category of a class
     * (whether this interceptor's class or the class getting called)
     * but rather into a specific named category.
     * <p><b>NOTE:</b> Specify either this property or "useDynamicLogger", not both.
     * @see org.slf4j.LoggerFactory#getLogger(String)
     * @see java.util.logging.Logger#getLogger(String)
     */
    public void setLoggerName(String loggerName) {
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * Return the appropriate {@code Logger} instance to use for the given
     * {@code MethodInvocation}. If the {@code useDynamicLogger} flag
     * is set, the {@code Logger} instance will be for the target class of the
     * {@code MethodInvocation}, otherwise the {@code Logger} will be the
     * default static logger.
     * @param invocation the {@code MethodInvocation} being traced
     * @return the {@code Logger} instance to use
     * @see #setUseDynamicLogger
     */
    protected Logger getLoggerForInvocation(MethodInvocation invocation) {
        if (this.logger != null) {
            return this.logger;
        }
        else {
            Object target = invocation.getThis();
            return LoggerFactory.getLogger(getTargetClass(target));
        }
    }

    /**
     * Determine the class to use for logging purposes.
     * @param target the target object to introspect
     * @return the target class for the given object
     */
    protected Class<?> getTargetClass(Object target) {
        return getTargetClass(target, this.hideProxyClassNames);
    }

    /**
     * Determine the class to use for logging purposes.
     * @param target the target object to introspect
     * @return the target class for the given object
     * @see #getTargetClass
     * @see #setHideProxyClassNames
     */
    protected Class<?> getTargetClass(Object target, boolean hideProxyClassNames) {
        if (target == null) {
            return null;
        }

        return hideProxyClassNames ? AopUtils.getTargetClass(target) : target.getClass();
    }

    /**
     * Write the supplied trace message to the supplied {@code Log} instance.
     * <p>To be called by {@link #invokeUnderTrace} for enter/exit messages.
     * <p>Delegates to {@link #writeToLog(Logger, String, Throwable)} as the
     * ultimate delegate that controls the underlying logger invocation.
     * @see #writeToLog(Logger, String, Throwable)
     */
    protected void writeToLog(Logger logger, String message) {
        writeToLog(logger, message, null);
    }

    /**
     * Write the supplied trace message and {@link Throwable} to the
     * supplied {@code Log} instance.
     * <p>To be called by {@link #invokeUnderTrace} for enter/exit outcomes,
     * potentially including an exception. Note that an exception's stack trace
     * won't get logged when {@link #setPrintStackTrace} is "false".
     * <p>By default messages are written at {@code TRACE} level. Subclasses
     * can override this method to control which level the message is written
     * at, typically also overriding {@link #isLoggerEnabled} accordingly.
     * @see #setPrintStackTrace
     * @see #isLoggerEnabled
     */
    protected void writeToLog(Logger logger, String message, @Nullable Throwable ex) {
        if (isLoggerEnabled()) {
            if (ex != null && this.printStackTrace) {
                logger.info(message, ex);
            }
            else {
                logger.info(message);
            }
        }
    }

    /**
     * Determine whether the interceptor should kick in, that is,
     * whether the {@code invokeUnderTrace} method should be called.
     * <p>Default behavior is to check whether the given {@code Log}
     * instance is enabled. Subclasses or configuration can override this to apply the
     * interceptor in other cases as well.
     * @see #invokeUnderTrace
     * @see #isLoggerEnabled
     */
    protected boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Determine whether the given {@link Log} instance is enabled.
     * <p>Default is {@code true} when the "trace" level is enabled.
     */
    protected boolean isLoggerEnabled() {
        return this.loggerEnabled;
    }

    public void setLoggerEnabled(boolean loggerEnabled) {
        this.loggerEnabled = loggerEnabled;
    }

    /**
     * Set to "true" to have {@link #setUseDynamicLogger dynamic loggers} hide
     * proxy class names wherever possible. Default is "false".
     */
    public void setHideProxyClassNames(boolean hideProxyClassNames) {
        this.hideProxyClassNames = hideProxyClassNames;
    }

    public boolean isHideProxyClassNames() {
        return hideProxyClassNames;
    }

    public boolean isPrintStackTrace() {
        return this.printStackTrace;
    }

    /**
     * Set whether to pass an exception to the logger, suggesting inclusion
     * of its stack trace into the log. Default is "true"; set this to "false"
     * in order to reduce the log output to just the trace message (which may
     * include the exception class name and exception message, if applicable).
     * @since 4.3.10
     */
    public void setPrintStackTrace(boolean printStackTrace) {
        this.printStackTrace = printStackTrace;
    }

}
