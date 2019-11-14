package org.xbdframework.context.trace.support;

import org.xbdframework.context.trace.TraceDefinition;
import org.xbdframework.context.trace.TraceHandler;

import java.io.Serializable;

/**
 * default implementation of the {@link TraceDefinition}.
 * @author luas
 * @since 4.3
 */
@SuppressWarnings("serial")
public class DefaultTraceDefinition implements TraceDefinition, Serializable {

    private boolean enabled = true;

    private boolean loggerEnabled = true;

    private String loggerName;

    private String[] handlerRefs;

    private Class<? extends TraceHandler>[] handlers;

    private boolean printStackTrace;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    public void setLoggerEnabled(boolean loggerEnabled) {
        this.loggerEnabled = loggerEnabled;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String[] getHandlerRefs() {
        return handlerRefs;
    }

    public void setHandlerRefs(String[] handlerRefs) {
        this.handlerRefs = handlerRefs;
    }

    public Class<? extends TraceHandler>[] getHandlers() {
        return handlers;
    }

    public void setHandlers(Class<? extends TraceHandler>[] handlers) {
        this.handlers = handlers;
    }

    public boolean isPrintStackTrace() {
        return printStackTrace;
    }

    public void setPrintStackTrace(boolean printStackTrace) {
        this.printStackTrace = printStackTrace;
    }
}
