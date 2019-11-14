package org.xbdframework.context.trace.interceptor;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.xbdframework.context.trace.TraceHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for trace aspects, such as the {@link TraceInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Spring trace infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in the correct order.
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@code TransactionAttributeSource}
 * is used for determining trace definitions.
 *
 * <p>A trace aspect is serializable if its {@code TraceAttributeSource} are serializable.
 *
 * @author luas
 * @since 4.3
 * @see #setTraceAttributeSource
 */
public abstract class TraceAspectSupport extends CustomizableLoggingAspectSupport implements BeanFactoryAware, InitializingBean {

    // NOTE: This class must not implement Serializable because it serves as base
    // class for AspectJ aspects (which are not allowed to implement Serializable)!

    /**
     * Key to use to store the default trace manager.
     */
    private static final Object DEFAULT_TRACE_MANAGER_KEY = new Object();

    /**
     * cache to use to store the bean name`s instance or class`s instance.
     */
    private final Map<String, TraceHandler> traceHandlerCache = new ConcurrentHashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TraceAttributeSource traceAttributeSource;

    private ConfigurableListableBeanFactory beanFactory;

    /**
     * Set multiple trace attribute sources which are used to find trace attributes.
     * Will build a CompositeTransactionAttributeSource for the given sources.
     * @see CompositeTraceAttributeSource
     * @see MethodMapTraceAttributeSource
     * @see NameMatchTraceAttributeSource
     * @see org.xbdframework.context.trace.annotation.AnnotationTraceAttributeSource
     */
    public void setTraceAttributeSources(TraceAttributeSource... traceAttributeSources) {
        this.traceAttributeSource = new CompositeTraceAttributeSource(traceAttributeSources);
    }

    /**
     * Set the trace attribute source which is used to find trace attributes.
     * If specifying a String property value, a PropertyEditor
     * will create a MethodMapTransactionAttributeSource from the value.
     * @see MethodMapTraceAttributeSource
     * @see NameMatchTraceAttributeSource
     * @see org.xbdframework.context.trace.annotation.AnnotationTraceAttributeSource
     */
    public void setTraceAttributeSource(TraceAttributeSource traceAttributeSource) {
        this.traceAttributeSource = traceAttributeSource;
    }

    /**
     * Return the trace attribute source.
     */
    public TraceAttributeSource getTraceAttributeSource() {
        return this.traceAttributeSource;
    }

    /**
     * Set the BeanFactory to use for retrieving PlatformTransactionManager beans.
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * Return the BeanFactory to use for retrieving PlatformTransactionManager beans.
     */
    protected final BeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    /**
     * Check that required properties were set.
     */
    @Override
    public void afterPropertiesSet() {
        if (getTraceAttributeSource() == null) {
            throw new IllegalStateException(
                    "Either 'traceAttributeSource' or 'traceAttributes' is required: " +
                            "If there are no traceable methods, then don't use a trace aspect.");
        }
    }

    /**
     * General delegate for around-advice-based subclasses, delegating to several other template
     * methods on this class.
     * @param method the Method being invoked
     * @param targetClass the target class that we're invoking the method on
     * @param invocation the callback to use for proceeding with the target invocation
     * @return the return value of the method, if any
     * @throws Throwable propagated from the target invocation
     */
    protected Object invokeWithinTrace(Method method, Class<?> targetClass, Object[] arguments, final InvocationCallback invocation)
            throws Throwable {
        final TraceAttribute traceAttribute = getTraceAttributeSource().getTraceAttribute(method, targetClass);

        boolean enabled = traceAttribute.isEnabled();
        boolean loggerEnabled = traceAttribute.isLoggerEnabled();
        String loggerName = traceAttribute.getLoggerName();
        String[] handlerRefs = traceAttribute.getHandlerRefs();
        Class<? extends TraceHandler>[] handlerClasses = traceAttribute.getHandlers();
        boolean printStackTrace = traceAttribute.isPrintStackTrace();

        if (!enabled) {
            return invocation.proceedWithInvocation();
        }

        Logger loggerTemp = StringUtils.hasText(loggerName) ? LoggerFactory.getLogger(loggerName) : this.logger;

        Set<TraceHandler> handlers = determineTraceHandlers(handlerRefs, handlerClasses);

        LocalDateTime enterTime = null;
        LocalDateTime exceptionTime, exitTime;

        Object retVal = null;
        try {
            enterTime = LocalDateTime.now();
            enterLog(targetClass, method, arguments, loggerTemp, loggerEnabled);
            //enter handler
            enterHandle(handlers, targetClass, method, arguments, enterTime);

            retVal = invocation.proceedWithInvocation();
        } catch (Throwable ex) {
            exceptionTime = LocalDateTime.now();
            exceptionLog(targetClass, method, arguments, ex, loggerTemp, loggerEnabled && printStackTrace);
            // exception handler
            exceptionHandle(handlers, targetClass, method, arguments, ex, exceptionTime);
            throw ex;
        } finally {
            exitTime = LocalDateTime.now();
            exitLog(targetClass, method, arguments, retVal, Duration.between(enterTime, exitTime).toMillis(), loggerTemp, loggerEnabled);
            // exit handler
            exitHandle(handlers, targetClass, method, arguments, retVal, exitTime);
        }

        return retVal;
    }

    /**
     * logging before the method invocation proceed.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters.
     * @param logger the {@code Logger} instance to check.
     * @param print decide whether to log.
     */
    private void enterLog(Class<?> targetClass, Method method, Object[] arguments, Logger logger, boolean print) {
        if (print) {
            enterLog(targetClass, method, arguments, logger);
        }
    }

    /**
     * logging when the method invocation proceeding occurred exception.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters.
     * @param ex any {@code Throwable} raised during the invocation.
     * The value of {@code Throwable.toString()} is replaced for the
     * {@code $[exception]} placeholder. May be {@code null}.
     * @param logger the {@code Logger} instance to check.
     * @param print decide whether to log.
     */
    private void exceptionLog(Class<?> targetClass, Method method, Object[] arguments, Throwable ex, Logger logger, boolean print) {
        if (print) {
            exceptionLog(targetClass, method, arguments, ex, logger);
        }
    }

    /**
     * logging after the method invocation proceeded.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters.
     * @param returnValue any value returned by the invocation.
     * @param invocationTime the value to write in place of the
     * {@code $[invocationTime]} placeholder.
     * @param logger the {@code Logger} instance to check.
     * @param print decide whether to log.
     */
    private void exitLog(Class<?> targetClass, Method method, Object[] arguments, Object returnValue, long invocationTime, Logger logger, boolean print) {
        if (print) {
            exitLog(targetClass, method, arguments, returnValue, invocationTime, logger);
        }
    }

    /**
     * handle before the method invocation proceed.
     * @param handlers list {@code TraceHandler} to being handled.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters.
     * @param time current timestamp before the method invocation proceed.
     */
    private void enterHandle(Set<TraceHandler> handlers, Class<?> targetClass, Method method, Object[] arguments, LocalDateTime time) {
        if (CollectionUtils.isEmpty(handlers)) {
            return;
        }

        for (TraceHandler handler : handlers) {
            handler.beforeHandle(targetClass, method, arguments, time);
        }
    }

    /**
     * handle when the method invocation proceeding occurred exception.
     * @param handlers list {@code TraceHandler} to being handled.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters.
     * @param exception any {@code Throwable} raised during the invocation.
     * @param time current timestamp the method invocation proceed occurred exception.
     */
    private void exceptionHandle(Set<TraceHandler> handlers, Class<?> targetClass, Method method, Object[] arguments, Throwable exception, LocalDateTime time) {
        if (CollectionUtils.isEmpty(handlers)) {
            return;
        }

        for (TraceHandler handler : handlers) {
            handler.errorHandle(targetClass, method, arguments, exception, time);
        }
    }

    /**
     * logging after the method invocation proceeded.
     * @param targetClass the target class that we're invoking the method on
     * @param method the Method being invoked
     * @param arguments the Method invoke parameters.
     * @param returnValue any value returned by the invocation.
     * @param time current timestamp after the method invocation proceed.
     */
    private void exitHandle(Set<TraceHandler> handlers, Class<?> targetClass, Method method, Object[] arguments, Object returnValue, LocalDateTime time) {
        if (CollectionUtils.isEmpty(handlers)) {
            return;
        }

        for (TraceHandler handler : handlers) {
            handler.afterHandle(targetClass, method, arguments, returnValue, time);
        }
    }

    /**
     * organize {@code TraceHandler} according to {@code handlerRefs} or {@code handlerClasses}
     * @param handlerRefs the {@code TraceHandler} bean names.
     * @param handlerClasses the {@code TraceHandler} bean classes.
     * @return the list {@code TraceHandler} to be handled.
     */
    private Set<TraceHandler> determineTraceHandlers(String[] handlerRefs, Class<? extends TraceHandler>[] handlerClasses) {
        // Priority determine handlers by handlerRefs, if it isn't null.
        if (!ObjectUtils.isEmpty(handlerRefs)) {
            return determineTraceHandlers(handlerRefs);
        } else if (!ObjectUtils.isEmpty(handlerClasses)) {
            return determineTraceHandlers(handlerClasses);
        }

        return null;
    }

    /**
     * organize {@code TraceHandler} according to {@code handlerRefs}.
     * if the object corresponding to the bean name exists in
     * {@code traceHandlerRefCache}, its object is no longer obtain.
     * <p>adding bean name`s instance to {@code traceHandlerRefCache},
     * will also add to {@code traceHandlerClassCache}, the key is the
     * class of current instance.
     * @param handlerRefs the {@code TraceHandler} bean names.
     * @return the list {@code TraceHandler} to be handled.
     */
    private Set<TraceHandler> determineTraceHandlers(String[] handlerRefs) {
        if (handlerRefs == null || handlerRefs.length == 0) {
            return null;
        }

        Set<TraceHandler> handlers = new HashSet<>();

        for (String beanName : handlerRefs) {
            if (!this.beanFactory.containsBean(beanName)) {
                this.logger.warn("trace handler instance bean named {} is not exist.", beanName);
                continue;
            }

            TraceHandler instance = (TraceHandler) this.beanFactory.getBean(beanName);
            traceHandlerCache.putIfAbsent(beanName, instance);
            traceHandlerCache.putIfAbsent(instance.getClass().getName(), instance);
            handlers.add(instance);
        }

        return handlers;
    }

    /**
     * organize {@code TraceHandler} according to {@code handlerClasses}.
     * if the object corresponding to the class name exists in
     * {@code traceHandlerClassCache}, its object is no longer created.
     * @param handlerClasses the {@code TraceHandler} bean classes.
     * @return the list {@code TraceHandler} to be handled.
     */
    private Set<TraceHandler> determineTraceHandlers(Class<? extends TraceHandler>[] handlerClasses) {
        if (handlerClasses == null || handlerClasses.length == 0) {
            return null;
        }

        Set<TraceHandler> handlers = new HashSet<>();
        for (Class<? extends TraceHandler> clazz : handlerClasses) {
            TraceHandler instance = traceHandlerCache.get(clazz.getName());
            if (instance == null) {
                instance = (TraceHandler) this.beanFactory.createBean(clazz, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
                traceHandlerCache.putIfAbsent(clazz.getName(), instance);
                handlers.add(instance);
            }

            handlers.add(instance);
        }

        return handlers;
    }

    /**
     * Simple callback interface for proceeding with the target invocation.
     * Concrete interceptors/aspects adapt this to their invocation mechanism.
     */
    protected interface InvocationCallback {

        Object proceedWithInvocation() throws Throwable;
    }

}
