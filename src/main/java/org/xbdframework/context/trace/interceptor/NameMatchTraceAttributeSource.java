package org.xbdframework.context.trace.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * Simple {@link TraceAttributeSource} implementation that
 * allows attributes to be matched by registered name.
 *
 * @author luas
 * @since 4.3
 * @see #isMatch
 * @see MethodMapTraceAttributeSource
 */
@SuppressWarnings("serial")
public class NameMatchTraceAttributeSource implements TraceAttributeSource, Serializable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Keys are method names; values are TraceAttributes */
    private Map<String, TraceAttribute> nameMap = new HashMap<String, TraceAttribute>();

    /**
     * Set a name/attribute map, consisting of method names
     * (e.g. "myMethod") and TraceAttribute instances
     * (or Strings to be converted to TraceAttribute instances).
     * @see TraceAttribute
     */
    public void setNameMap(Map<String, TraceAttribute> nameMap) {
        for (Map.Entry<String, TraceAttribute> entry : nameMap.entrySet()) {
            addTraceableMethod(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Add an attribute for a traced method.
     * <p>Method names can be exact matches, or of the pattern "xxx*",
     * "*xxx" or "*xxx*" for matching multiple methods.
     * @param methodName the name of the method
     * @param attr attribute associated with the method
     */
    public void addTraceableMethod(String methodName, TraceAttribute attr) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding traceable method [" + methodName + "] with attribute [" + attr + "]");
        }
        this.nameMap.put(methodName, attr);
    }

    @Override
    public TraceAttribute getTraceAttribute(Method method, Class<?> targetClass) {
        if (!ClassUtils.isUserLevelMethod(method)) {
            return null;
        }

        // Look for direct name match.
        String methodName = method.getName();
        TraceAttribute attr = this.nameMap.get(methodName);

        if (attr == null) {
            // Look for most specific name match.
            String bestNameMatch = null;
            for (String mappedName : this.nameMap.keySet()) {
                if (isMatch(methodName, mappedName) &&
                        (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
                    attr = this.nameMap.get(mappedName);
                    bestNameMatch = mappedName;
                }
            }
        }

        return attr;
    }

    /**
     * Return if the given method name matches the mapped name.
     * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
     * as well as direct equality. Can be overridden in subclasses.
     * @param methodName the method name of the class
     * @param mappedName the name in the descriptor
     * @return if the names match
     * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
     */
    protected boolean isMatch(String methodName, String mappedName) {
        return PatternMatchUtils.simpleMatch(mappedName, methodName);
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NameMatchTraceAttributeSource)) {
            return false;
        }
        NameMatchTraceAttributeSource otherTas = (NameMatchTraceAttributeSource) other;
        return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
    }

    @Override
    public int hashCode() {
        return NameMatchTraceAttributeSource.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + this.nameMap;
    }

}
