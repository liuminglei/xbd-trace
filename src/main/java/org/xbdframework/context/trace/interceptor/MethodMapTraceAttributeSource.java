package org.xbdframework.context.trace.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * Simple {@link TraceAttributeSource} implementation that
 * allows attributes to be stored per method in a {@link Map}.
 *
 * @author luas
 * @since 4.3
 * @see #isMatch
 * @see NameMatchTraceAttributeSource
 */
public class MethodMapTraceAttributeSource implements TraceAttributeSource, BeanClassLoaderAware, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Map from method name to attribute value */
    private Map<String, TraceAttribute> methodMap;

    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    private boolean eagerlyInitialized = false;

    private boolean initialized = false;

    /** Map from Method to TraceAttribute */
    private final Map<Method, TraceAttribute> traceAttributeMap =
            new HashMap<Method, TraceAttribute>();

    /** Map from Method to name pattern used for registration */
    private final Map<Method, String> methodNameMap = new HashMap<Method, String>();

    /**
     * Set a name/attribute map, consisting of "FQCN.method" method names
     * (e.g. "com.mycompany.mycode.MyClass.myMethod") and
     * {@link TraceAttribute} instances (or Strings to be converted
     * to {@code TraceAttribute} instances).
     * <p>Intended for configuration via setter injection, typically within
     * a Spring bean factory. Relies on {@link #afterPropertiesSet()}
     * being called afterwards.
     * @param methodMap said {@link Map} from method name to attribute value
     * @see TraceAttribute
     */
    public void setMethodMap(Map<String, TraceAttribute> methodMap) {
        this.methodMap = methodMap;
    }

    @Override
    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }


    /**
     * Eagerly initializes the specified
     * {@link #setMethodMap(Map) "methodMap"}, if any.
     * @see #initMethodMap(Map)
     */
    @Override
    public void afterPropertiesSet() {
        initMethodMap(this.methodMap);
        this.eagerlyInitialized = true;
        this.initialized = true;
    }

    /**
     * Initialize the specified {@link #setMethodMap(Map) "methodMap"}, if any.
     * @param methodMap Map from method names to {@code TraceAttribute} instances
     * @see #setMethodMap
     */
    protected void initMethodMap(Map<String, TraceAttribute> methodMap) {
        if (methodMap != null) {
            for (Map.Entry<String, TraceAttribute> entry : methodMap.entrySet()) {
                addTraceableMethod(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * Add an attribute for a traceable method.
     * <p>Method names can end or start with "*" for matching multiple methods.
     * @param name class and method name, separated by a dot
     * @param attr attribute associated with the method
     * @throws IllegalArgumentException in case of an invalid name
     */
    public void addTraceableMethod(String name, TraceAttribute attr) {
        Assert.notNull(name, "Name must not be null");
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("'" + name + "' is not a valid method name: format is FQN.methodName");
        }
        String className = name.substring(0, lastDotIndex);
        String methodName = name.substring(lastDotIndex + 1);
        Class<?> clazz = ClassUtils.resolveClassName(className, this.beanClassLoader);
        addTraceableMethod(clazz, methodName, attr);
    }

    /**
     * Add an attribute for a traceable method.
     * Method names can end or start with "*" for matching multiple methods.
     * @param clazz target interface or class
     * @param mappedName mapped method name
     * @param attr attribute associated with the method
     */
    public void addTraceableMethod(Class<?> clazz, String mappedName, TraceAttribute attr) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(mappedName, "Mapped name must not be null");
        String name = clazz.getName() + '.'  + mappedName;

        Method[] methods = clazz.getDeclaredMethods();
        List<Method> matchingMethods = new ArrayList<Method>();
        for (Method method : methods) {
            if (isMatch(method.getName(), mappedName)) {
                matchingMethods.add(method);
            }
        }
        if (matchingMethods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Couldn't find method '" + mappedName + "' on class [" + clazz.getName() + "]");
        }

        // register all matching methods
        for (Method method : matchingMethods) {
            String regMethodName = this.methodNameMap.get(method);
            if (regMethodName == null || (!regMethodName.equals(name) && regMethodName.length() <= name.length())) {
                // No already registered method name, or more specific
                // method name specification now -> (re-)register method.
                if (logger.isDebugEnabled() && regMethodName != null) {
                    logger.debug("Replacing attribute for traceable method [" + method + "]: current name '" +
                            name + "' is more specific than '" + regMethodName + "'");
                }
                this.methodNameMap.put(method, name);
                addTraceableMethod(method, attr);
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Keeping attribute for traceable method [" + method + "]: current name '" +
                            name + "' is not more specific than '" + regMethodName + "'");
                }
            }
        }
    }

    /**
     * Add an attribute for a traceable method.
     * @param method the method
     * @param attr attribute associated with the method
     */
    public void addTraceableMethod(Method method, TraceAttribute attr) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(attr, "TraceAttribute must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Adding traceable method [" + method + "] with attribute [" + attr + "]");
        }
        this.traceAttributeMap.put(method, attr);
    }

    /**
     * Return if the given method name matches the mapped name.
     * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*"
     * matches, as well as direct equality.
     * @param methodName the method name of the class
     * @param mappedName the name in the descriptor
     * @return if the names match
     * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
     */
    protected boolean isMatch(String methodName, String mappedName) {
        return PatternMatchUtils.simpleMatch(mappedName, methodName);
    }

    @Override
    public TraceAttribute getTraceAttribute(Method method, Class<?> targetClass) {
        if (this.eagerlyInitialized) {
            return this.traceAttributeMap.get(method);
        }
        else {
            synchronized (this.traceAttributeMap) {
                if (!this.initialized) {
                    initMethodMap(this.methodMap);
                    this.initialized = true;
                }
                return this.traceAttributeMap.get(method);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodMapTraceAttributeSource)) {
            return false;
        }
        MethodMapTraceAttributeSource otherTas = (MethodMapTraceAttributeSource) other;
        return ObjectUtils.nullSafeEquals(this.methodMap, otherTas.methodMap);
    }

    @Override
    public int hashCode() {
        return MethodMapTraceAttributeSource.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + this.methodMap;
    }

}
