package org.xbdframework.context.trace.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ClassUtils;

/**
 * Abstract implementation of {@link TraceAttributeSource} that caches
 * attributes for methods and implements a fallback policy: 1. specific target
 * method; 2. target class; 3. declaring method; 4. declaring class/interface.
 *
 * <p>Defaults to using the target class's trace attribute if none is
 * associated with the target method. Any trace attribute associated with
 * the target method completely overrides a class trace attribute.
 * If none found on the target class, the interface that the invoked method
 * has been called through (in case of a JDK proxy) will be checked.
 *
 * <p>This implementation caches attributes by method after they are first used.
 * If it is ever desirable to allow dynamic changing of trace attributes
 * (which is very unlikely), caching could be made configurable. Caching is
 * desirable because of the cost of evaluating rollback rules.
 *
 * @author luas
 * @since 4.3
 */
public abstract class AbstractFallbackTraceAttributeSource implements TraceAttributeSource {

    /**
     * Canonical value held in cache to indicate no trace attribute was
     * found for this method, and we don't need to look again.
     */
    private final static TraceAttribute NULL_TRACE_ATTRIBUTE = new DefaultTraceAttribute();


    /**
     * Logger available to subclasses.
     * <p>As this base class is not marked Serializable, the logger will be recreated
     * after serialization - provided that the concrete subclass is Serializable.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Cache of TraceAttributes, keyed by method on a specific target class.
     * <p>As this base class is not marked Serializable, the cache will be recreated
     * after serialization - provided that the concrete subclass is Serializable.
     */
    private final Map<Object, TraceAttribute> attributeCache =
            new ConcurrentHashMap<Object, TraceAttribute>(1024);


    /**
     * Determine the trace attribute for this method invocation.
     * <p>Defaults to the class's trace attribute if no method attribute is found.
     * @param method the method for the current invocation (never {@code null})
     * @param targetClass the target class for this invocation (may be {@code null})
     * @return TraceAttribute for this method, or {@code null} if the method
     * is not traceable
     */
    @Override
    public TraceAttribute getTraceAttribute(Method method, Class<?> targetClass) {
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }

        // First, see if we have a cached value.
        Object cacheKey = getCacheKey(method, targetClass);
        Object cached = this.attributeCache.get(cacheKey);
        if (cached != null) {
            // Value will either be canonical value indicating there is no transaction attribute,
            // or an actual trace attribute.
            if (cached == NULL_TRACE_ATTRIBUTE) {
                return null;
            }
            else {
                return (TraceAttribute) cached;
            }
        }
        else {
            // We need to work it out.
            TraceAttribute txAttr = computeTraceAttribute(method, targetClass);
            // Put it in the cache.
            if (txAttr == null) {
                this.attributeCache.put(cacheKey, NULL_TRACE_ATTRIBUTE);
            }
            else {
                String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
                if (txAttr instanceof DefaultTraceAttribute) {
                    ((DefaultTraceAttribute) txAttr).setDescriptor(methodIdentification);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding traceable method '" + methodIdentification + "' with attribute: " + txAttr);
                }
                this.attributeCache.put(cacheKey, txAttr);
            }
            return txAttr;
        }
    }

    /**
     * Determine a cache key for the given method and target class.
     * <p>Must not produce same key for overloaded methods.
     * Must produce same key for different instances of the same method.
     * @param method the method (never {@code null})
     * @param targetClass the target class (may be {@code null})
     * @return the cache key (never {@code null})
     */
    protected Object getCacheKey(Method method, Class<?> targetClass) {
        return new MethodClassKey(method, targetClass);
    }

    /**
     * Same signature as {@link #getTraceAttribute}, but doesn't cache the result.
     * {@link #getTraceAttribute} is effectively a caching decorator for this method.
     * @see #getTraceAttribute
     */
    protected TraceAttribute computeTraceAttribute(Method method, Class<?> targetClass) {
        // Don't allow no-public methods as required.
        if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }

        // Ignore CGLIB subclasses - introspect the actual user class.
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        // The method may be on an interface, but we need attributes from the target class.
        // If the target class is null, the method will be unchanged.
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);
        // If we are dealing with method with generic parameters, find the original method.
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

        // First try is the method in the target class.
        TraceAttribute txAttr = findTraceAttribute(specificMethod);
        if (txAttr != null) {
            return txAttr;
        }

        // Second try is the trace attribute on the target class.
        txAttr = findTraceAttribute(specificMethod.getDeclaringClass());
        if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
            return txAttr;
        }

        if (specificMethod != method) {
            // Fallback is to look at the original method.
            txAttr = findTraceAttribute(method);
            if (txAttr != null) {
                return txAttr;
            }
            // Last fallback is the class of the original method.
            txAttr = findTraceAttribute(method.getDeclaringClass());
            if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
                return txAttr;
            }
        }

        return null;
    }


    /**
     * Subclasses need to implement this to return the trace attribute
     * for the given method, if any.
     * @param method the method to retrieve the attribute for
     * @return all trace attribute associated with this method
     * (or {@code null} if none)
     */
    protected abstract TraceAttribute findTraceAttribute(Method method);

    /**
     * Subclasses need to implement this to return the trace attribute
     * for the given class, if any.
     * @param clazz the class to retrieve the attribute for
     * @return all trace attribute associated with this class
     * (or {@code null} if none)
     */
    protected abstract TraceAttribute findTraceAttribute(Class<?> clazz);


    /**
     * Should only public methods be allowed to have traceable semantics?
     * <p>The default implementation returns {@code false}.
     */
    protected boolean allowPublicMethodsOnly() {
        return false;
    }

}
