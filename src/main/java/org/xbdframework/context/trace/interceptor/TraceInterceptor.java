package org.xbdframework.context.trace.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;

/**
 * AOP Alliance MethodInterceptor for declarative trace
 * management using the common traceable infrastructure.
 *
 * <p>Derives from the {@link TraceAspectSupport} class which
 * contains the integration with Spring's underlying trace API.
 * TraceInterceptor simply calls the relevant superclass methods
 * such as {@link #invokeWithinTrace} in the correct order.
 *
 * <p>TraceInterceptors are thread-safe.
 *
 * @author luas
 * @since 4.3
 * @see TraceProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactory
 */
@SuppressWarnings("serial")
public class TraceInterceptor extends TraceAspectSupport implements MethodInterceptor, Serializable {

    private TraceAttributeSource traceAttributeSource;

    /**
     * Create a new TransactionInterceptor.
     * @see #setTraceAttributeSource(TraceAttributeSource)
     */
    public TraceInterceptor() {
    }


    /**
     * Create a new TraceInterceptor.
     * @param tas the attribute source to be used to find trace attributes
     * @see #setTraceAttributeSource(TraceAttributeSource)
     */
    public TraceInterceptor(TraceAttributeSource tas) {
        setTraceAttributeSource(tas);
    }


    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        // Work out the target class: may be {@code null}.
        // The TraceAttributeSource should be passed the target class
        // as well as the method, which may be from an interface.
        Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

        // Adapt to TraceAspectSupport's invokeWithinTrace...
        return invokeWithinTrace(invocation.getMethod(), targetClass, invocation.getArguments(), new InvocationCallback() {
            @Override
            public Object proceedWithInvocation() throws Throwable {
                return invocation.proceed();
            }
        });
    }

    public TraceAttributeSource getTraceAttributeSource() {
        return traceAttributeSource;
    }

    public void setTraceAttributeSource(TraceAttributeSource traceAttributeSource) {
        this.traceAttributeSource = traceAttributeSource;
    }
}
