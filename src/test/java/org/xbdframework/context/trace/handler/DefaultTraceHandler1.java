package org.xbdframework.context.trace.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbdframework.context.trace.TraceHandler;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class DefaultTraceHandler1 implements TraceHandler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeHandle(Class<?> targetClass, Method method, Object[] arguments, LocalDateTime time) {
        logger.info("targetClass is {}", targetClass);
        logger.info("method is {}", method.getName());
        logger.info("arguments is {}", arguments);
        logger.info("time is {}", time);
    }

    @Override
    public void errorHandle(Class<?> targetClass, Method method, Object[] arguments, Throwable exception, LocalDateTime time) {
        logger.info("targetClass is {}", targetClass);
        logger.info("method is {}", method.getName());
        logger.info("arguments is {}", arguments);
        logger.info("time is {}", time);
        logger.info("exception is {}", exception);
    }

    @Override
    public void afterHandle(Class<?> targetClass, Method method, Object[] arguments, Object returnValue, LocalDateTime time) {
        logger.info("targetClass is {}", targetClass);
        logger.info("method is {}", method.getName());
        logger.info("arguments is {}", arguments);
        logger.info("time is {}", time);
        logger.info("returnValue is {}", returnValue);
    }
}
