package org.xbdframework.context.trace.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xbdframework.context.trace.service.UserService;

public class UserServiceImpl implements UserService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void getUser(String id) {
        this.logger.info("id is {}", id);
    }
}
