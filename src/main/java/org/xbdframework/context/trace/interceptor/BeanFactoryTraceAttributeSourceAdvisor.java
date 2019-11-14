package org.xbdframework.context.trace.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * PointcutAdvisor that can structure by setting the advice beanName or advice bean instance.
 *
 * @author luas
 * @since 4.3
 */
@SuppressWarnings("serial")
public class BeanFactoryTraceAttributeSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    private TraceAttributeSource traceAttributeSource;

    private final TraceAttributeSourcePointcut pointcut = new TraceAttributeSourcePointcut() {
        @Override
        protected TraceAttributeSource getTraceAttributeSource() {
            return traceAttributeSource;
        }
    };

    public void setTraceAttributeSource(TraceAttributeSource traceAttributeSource) {
        this.traceAttributeSource = traceAttributeSource;
    }

    /**
     * Set the {@link ClassFilter} to use for this pointcut.
     * Default is {@link ClassFilter#TRUE}.
     */
    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

}
