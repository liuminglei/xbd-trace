package org.xbdframework.context.trace.interceptor;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * Proxy factory bean for simplified declarative trace handling.
 * This is a convenient alternative to a standard AOP
 * {@link org.springframework.aop.framework.ProxyFactoryBean}
 * with a separate {@link TraceInterceptor} definition.
 *
 * <p><strong>HISTORICAL NOTE:</strong> This class was originally designed to cover the
 * typical case of declarative trace demarcation: namely, wrapping a singleton
 * target object with a trace proxy, proxying all the interfaces that the target
 * implements.
 *
 * <p>There are two main properties that need to be specified:
 * <ul>
 * <li>"target": the target object that a trace proxy should be created for
 * <li>"traceAttributeSource": the trace attributes (for example, enabled
 * loggerName and loggerEnabled flag) per target method name (or method name pattern)
 * </ul>
 *
 * <p>In contrast to {@link TraceInterceptor}, the trace attributes are
 * specified as properties, with method names as keys and trace attribute
 * descriptors as values. Method names are always applied to the target class.
 *
 * <p>Internally, a {@link TraceInterceptor} instance is used, but the user of this
 * class does not have to care. Optionally, a method pointcut can be specified
 * to cause conditional invocation of the underlying {@link TraceInterceptor}.
 *
 * <p>The "preInterceptors" and "postInterceptors" properties can be set to add
 * additional interceptors to the mix, like
 * {@link org.springframework.aop.interceptor.PerformanceMonitorInterceptor}.
 *
 * <p><b>HINT:</b> This class is often used with parent / child bean definitions.
 * Typically, you will define the trace manager and default trace attributes
 * (for method name patterns) in an abstract parent bean definition,
 * deriving concrete child bean definitions for specific target objects.
 * This reduces the per-bean definition effort to a minimum.
 *
 * <pre code="class">
 * {@code
 * <bean id="attribute" class="org.xbdframework.context.trace.interceptor.DefaultTraceAttribute">
 *      <property name="enabled" value="true"></property>
 *      <property name="loggerEnabled" value="true"></property>
 *      ...
 * </bean>
 *
 * <bean id="baseTransactionProxy" class="org.xbdframework.context.trace.interceptor.TraceProxyFactoryBean"
 *     abstract="true">
 *   <property name="traceAttributeSource">
 *     <bean class="org.xbdframework.context.trace.interceptor.NameMatchTraceAttributeSource">
 *       <property name="nameMap">
 *          <map>
 *              <entry key="save*" value-ref="attribute"></entry>
 *              <entry key="*" value-ref="attribute"></entry>
 *          </map>
 *       </property>
 *     </bean>
 *   </property>
 * </bean>
 *
 * <bean id="myProxy" parent="baseTransactionProxy">
 *   <property name="target" ref="myTarget"/>
 * </bean>
 *
 * <bean id="yourProxy" parent="baseTransactionProxy">
 *   <property name="target" ref="yourTarget"/>
 * </bean>}</pre>
 *
 * @author luas
 * @since 4.3
 * @see #setTarget
 * @see #setTraceAttributeSource
 * @see TraceInterceptor
 * @see org.springframework.aop.framework.ProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TraceProxyFactoryBean extends AbstractSingletonProxyFactoryBean
        implements BeanFactoryAware {

    private final TraceInterceptor traceInterceptor = new TraceInterceptor();

    private Pointcut pointcut;

    /**
     * Set the trace attribute source which is used to find trace
     * attributes. If specifying a String property value, a PropertyEditor
     * will create a MethodMapTraceAttributeSource from the value.
     * @see TraceInterceptor#setTraceAttributeSource
     * @see MethodMapTraceAttributeSource
     * @see NameMatchTraceAttributeSource
     * @see org.xbdframework.context.trace.annotation.AnnotationTraceAttributeSource
     */
    public void setTraceAttributeSource(TraceAttributeSource traceAttributeSource) {
        this.traceInterceptor.setTraceAttributeSource(traceAttributeSource);
    }

    /**
     * Set a pointcut, i.e a bean that can cause conditional invocation
     * of the TraceInterceptor depending on method and attributes passed.
     * Note: Additional interceptors are always invoked.
     * @see #setPreInterceptors
     * @see #setPostInterceptors
     */
    public void setPointcut(Pointcut pointcut) {
        this.pointcut = pointcut;
    }

    /**
     * This callback is optional: If running in a BeanFactory and no trace
     * manager has been set explicitly, a single matching bean of type will be fetched from the BeanFactory.
     * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.traceInterceptor.setBeanFactory(beanFactory);
    }

    /**
     * Creates an advisor for this FactoryBean's TraceInterceptor.
     */
    @Override
    protected Object createMainInterceptor() {
        this.traceInterceptor.afterPropertiesSet();
        if (this.pointcut != null) {
            return new DefaultPointcutAdvisor(this.pointcut, this.traceInterceptor);
        }
        else {
            // Rely on default pointcut.
            return new TraceAttributeSourceAdvisor(this.traceInterceptor);
        }
    }

    /**
     * this method adds {@link TraceProxy} to the set of proxy interfaces
     * in order to avoid re-processing of trace metadata.
     */
    @Override
    protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
        proxyFactory.addInterface(TraceProxy.class);
    }

}
