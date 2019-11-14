package org.xbdframework.context.trace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.xbdframework.context.trace.service.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-*.xml")
@WebAppConfiguration
public class TraceAnnotationTests {

    @Autowired
    private UserService userService;

    @Test
    public void test() {
        this.userService.getUser("test");
    }

}
