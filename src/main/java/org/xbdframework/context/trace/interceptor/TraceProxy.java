package org.xbdframework.context.trace.interceptor;

import org.springframework.aop.SpringProxy;

/**
 * A marker interface for manually created transactional proxies.
 *
 * <p>{@link TraceAttributeSourcePointcut} will ignore such existing
 * traceable proxies during AOP auto-proxying and therefore avoid
 * re-processing trace metadata on them.
 *
 * @author luas
 * @since 4.3
 */
public interface TraceProxy extends SpringProxy {

}
