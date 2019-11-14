package org.xbdframework.context.trace.interceptor;

import org.xbdframework.context.trace.support.DefaultTraceDefinition;

/**
 * default implementation of the {@link TraceAttribute}.
 *
 * @author luas
 * @since 4.3
 */
@SuppressWarnings("serial")
public class DefaultTraceAttribute extends DefaultTraceDefinition implements TraceAttribute {

    private String qualifier;

    private String descriptor;

    @Override
    public String getQualifier() {
        return this.qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public String getDescriptor() {
        return this.descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
}
