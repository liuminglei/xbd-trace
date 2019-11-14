package org.xbdframework.context.trace.interceptor;

import org.xbdframework.context.trace.TraceDefinition;

/**
 * trace attribute metadata.
 *
 * @author luas
 * @since 4.3
 */
public interface TraceAttribute extends TraceDefinition {

    /**
     * Extended use later.
     */
    String getQualifier();

    /**
     * Description of the trace.
     * @return Description of the trace.
     */
    String getDescriptor();

}
