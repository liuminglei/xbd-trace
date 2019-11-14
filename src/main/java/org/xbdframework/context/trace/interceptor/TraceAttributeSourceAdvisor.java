package org.xbdframework.context.trace.interceptor;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;

/**
 * Advisor driven by a {@link TraceAttributeSource}, used to include
 * a {@link TraceInterceptor} only for methods that are traceable.
 *
 * <p>Because the AOP framework caches advice calculations, this is normally
 * faster than just letting the TraceInterceptor run and find out
 * itself that it has no work to do.
 *
 * @author luas
 * @see #setTraceInterceptor
 * @see TraceProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TraceAttributeSourceAdvisor extends AbstractPointcutAdvisor {

    private TraceInterceptor traceInterceptor;

    private final TraceAttributeSourcePointcut pointcut = new TraceAttributeSourcePointcut() {
        @Override
        protected TraceAttributeSource getTraceAttributeSource() {
            return traceInterceptor != null ? traceInterceptor.getTraceAttributeSource() : null;
        }
    };

    /**
     * Create a new TraceAttributeSourceAdvisor.
     */
    public TraceAttributeSourceAdvisor() {
    }

    /**
     * Create a new TraceAttributeSourceAdvisor.
     * @param interceptor the traceable interceptor to use for this advisor
     */
    public TraceAttributeSourceAdvisor(TraceInterceptor interceptor) {
        setTraceInterceptor(interceptor);
    }


    /**
     * Set the trace interceptor to use for this advisor.
     */
    public void setTraceInterceptor(TraceInterceptor interceptor) {
        this.traceInterceptor = interceptor;
    }

    /**
     * Set the {@link ClassFilter} to use for this pointcut.
     * Default is {@link ClassFilter#TRUE}.
     */
    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }


    @Override
    public Advice getAdvice() {
        return this.traceInterceptor;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

}
