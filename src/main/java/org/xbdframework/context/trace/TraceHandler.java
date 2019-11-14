package org.xbdframework.context.trace;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * handle the trace context that during tracing.
 *
 * @author luas
 * @since 4.3
 */
public interface TraceHandler {

    /**
     * handle before the method invocation proceed.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters
     * @param time the time before the method invocation proceed
     */
    void beforeHandle(Class<?> targetClass, Method method, Object[] arguments, LocalDateTime time);

    /**
     * handle when the method invocation proceeding occurred exception.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters
     * @param exception any {@code Throwable} raised during the invocation.
     * @param time the time when the method invocation proceeding occurred exception
     */
    void errorHandle(Class<?> targetClass, Method method, Object[] arguments, Throwable exception, LocalDateTime time);

    /**
     * handle after the method invocation proceed.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters
     * @param returnValue
     * @param time the time after the method invocation proceed
     */
    void afterHandle(Class<?> targetClass, Method method, Object[] arguments, Object returnValue, LocalDateTime time);
}
