package org.xbdframework.context.trace.interceptor;

import java.io.Serializable;

import java.lang.reflect.Method;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

/**
 * Inner class that implements a Pointcut that matches if the underlying
 * {@link TraceAttributeSource} has an attribute for a given method.
 *
 * @author luas
 * @since 4.3
 */
@SuppressWarnings("serial")
public abstract class TraceAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (targetClass != null && TraceProxy.class.isAssignableFrom(targetClass)) {
            return false;
        }

        TraceAttributeSource tas = getTraceAttributeSource();
        return (tas == null || tas.getTraceAttribute(method, targetClass) != null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TraceAttributeSourcePointcut)) {
            return false;
        }
        TraceAttributeSourcePointcut otherPc = (TraceAttributeSourcePointcut) other;
        return ObjectUtils.nullSafeEquals(getTraceAttributeSource(), otherPc.getTraceAttributeSource());
    }

    @Override
    public int hashCode() {
        return TraceAttributeSourcePointcut.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getTraceAttributeSource();
    }


    /**
     * Obtain the underlying TransactionAttributeSource (may be {@code null}).
     * To be implemented by subclasses.
     */
    protected abstract TraceAttributeSource getTraceAttributeSource();

}
