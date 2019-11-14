package org.xbdframework.context.trace.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.util.Assert;

/**
 * Composite {@link TraceAttributeSource} implementation that iterates
 * over a given array of {@link TraceAttributeSource} instances.
 *
 * @author luas
 * @since 4.3
 */
@SuppressWarnings("serial")
public class CompositeTraceAttributeSource implements TraceAttributeSource, Serializable {

    private final TraceAttributeSource[] traceAttributeSources;


    /**
     * Create a new CompositeTraceAttributeSource for the given sources.
     * @param transactionAttributeSources the TraceAttributeSource instances to combine
     */
    public CompositeTraceAttributeSource(TraceAttributeSource[] transactionAttributeSources) {
        Assert.notNull(transactionAttributeSources, "TraceAttributeSource array must not be null");
        this.traceAttributeSources = transactionAttributeSources;
    }

    /**
     * Return the TraceAttributeSource instances that this
     * CompositeTraceAttributeSource combines.
     */
    public final TraceAttributeSource[] getTraceAttributeSources() {
        return this.traceAttributeSources;
    }


    @Override
    public TraceAttribute getTraceAttribute(Method method, Class<?> targetClass) {
        for (TraceAttributeSource tas : this.traceAttributeSources) {
            TraceAttribute ta = tas.getTraceAttribute(method, targetClass);
            if (ta != null) {
                return ta;
            }
        }
        return null;
    }

}
