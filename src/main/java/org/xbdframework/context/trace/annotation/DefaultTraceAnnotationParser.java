package org.xbdframework.context.trace.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;

import org.xbdframework.context.trace.TraceHandler;
import org.xbdframework.context.trace.interceptor.DefaultTraceAttribute;
import org.xbdframework.context.trace.interceptor.TraceAttribute;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;

/**
 * Default strategy implementation for parsing {@link Traceable} annotation.
 *
 * @author luas
 * @since 4.3
 */
@SuppressWarnings("serial")
public class DefaultTraceAnnotationParser implements TraceAnnotationParser, Serializable {

    @Override
    public TraceAttribute parseTraceAnnotation(AnnotatedElement ae) {
        System.out.println("***************" + ae.toString());
        AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ae, Traceable.class);
        if (attributes != null) {
            System.out.println("----------------------");
            return parseTraceAnnotation(attributes);
        }
        else {
            return null;
        }
    }

    public TraceAttribute parseTransactionAnnotation(Traceable ann) {
        return parseTraceAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
    }

    protected TraceAttribute parseTraceAnnotation(AnnotationAttributes attributes) {
        DefaultTraceAttribute dta = new DefaultTraceAttribute();

        dta.setEnabled(attributes.getBoolean("enabled"));
        dta.setLoggerEnabled(attributes.getBoolean("loggerEnabled"));
        dta.setLoggerName(attributes.getString("loggerName"));

        String[] handlerRefs = attributes.getStringArray("handlerRefs");
        Class<? extends TraceHandler>[] handlerClasses = (Class<? extends TraceHandler>[]) attributes.getClassArray("handlers");

        if (!ObjectUtils.isEmpty(handlerRefs) && !ObjectUtils.isEmpty(handlerClasses)) {
            throw new IllegalArgumentException("handlerRefs and handlers only one can be configured.");
        }

        if (!ObjectUtils.isEmpty(handlerRefs)) {
            dta.setHandlerRefs(handlerRefs);
        }

        if (!ObjectUtils.isEmpty(handlerClasses)) {
            dta.setHandlers(handlerClasses);
        }

        dta.setPrintStackTrace(attributes.getBoolean("printStackTrace"));

        return dta;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || other instanceof DefaultTraceAnnotationParser);
    }

    @Override
    public int hashCode() {
        return DefaultTraceAnnotationParser.class.hashCode();
    }

}
