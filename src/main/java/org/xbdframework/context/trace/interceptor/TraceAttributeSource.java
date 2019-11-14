package org.xbdframework.context.trace.interceptor;

import java.lang.reflect.Method;

/**
 * @author luas
 * @since 4.3
 */
public interface TraceAttributeSource {

    /**
     * Return the trace attribute for the given method,
     * or {@code null} if the method is non-trace.
     * @param method the method to introspect
     * @param targetClass the target class. May be {@code null},
     * in which case the declaring class of the method must be used.
     * @return TraceAttribute the matching trace attribute,
     * or {@code null} if none found
     */
    TraceAttribute getTraceAttribute(Method method, Class<?> targetClass);

}
