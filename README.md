# xbd-trace

#### 介绍

基于Spring AOP的应用程序运行轨迹追踪记录框架

#### 特性

- 对应用程序基本零侵入
- 适配多spring版本
- 支持多场景、多方式配置方式
- 追踪数据覆盖广，包含源类、源方法、参数、返回、异常、时间等信息
- 配置方式简单易懂

## Spring Framework版本

spring version >= 4.3.x

推荐5.0系列及以上

## 使用说明

### 准备工作
编译打包，部署到maven私服

```
mvn clean deploy -Dmaven.test.skip=true 
```

引入依赖

```xml
<dependency>
     <groupId>org.xbdframework</groupId>
     <artifactId>xbd-trace</artifactId>
     <version>版本号</version>
</dependency>
```

### Java方式配置

注解方式

```java
@Configuration
public class TraceConfig {
    @Bean
    public TraceInterceptor traceInterceptor() {
        TraceInterceptor interceptor = new TraceInterceptor();

        AnnotationTraceAttributeSource traceAttributeSource = new AnnotationTraceAttributeSource(new DefaultTraceAnnotationParser());

        interceptor.setTraceAttributeSource(traceAttributeSource);

        return interceptor;
    }

    // BeanFactoryTraceAttributeSourceAdvisor Advice方式
    @Bean
    public BeanFactoryTraceAttributeSourceAdvisor beanFactoryTraceAttributeSourceAdvisor() {
        BeanFactoryTraceAttributeSourceAdvisor advisor = new BeanFactoryTraceAttributeSourceAdvisor();
        advisor.setAdvice(traceInterceptor());
        advisor.setTraceAttributeSource(new AnnotationTraceAttributeSource(new DefaultTraceAnnotationParser()));

        return advisor;
    }

    // BeanFactoryTraceAttributeSourceAdvisor AdviceBeanName方式
    @Bean
    public BeanFactoryTraceAttributeSourceAdvisor beanFactoryTraceAttributeSourceAdvisor() {
        BeanFactoryTraceAttributeSourceAdvisor advisor = new BeanFactoryTraceAttributeSourceAdvisor();
        advisor.setAdviceBeanName("traceInterceptor");
        advisor.setTraceAttributeSource(new AnnotationTraceAttributeSource(new DefaultTraceAnnotationParser()));

        return advisor;
    }

    // TraceAttributeSourceAdvisor 方式
    @Bean
    public TraceAttributeSourceAdvisor traceAttributeSourceAdvisor() {
        TraceAttributeSourceAdvisor advisor = new TraceAttributeSourceAdvisor();
        advisor.setTraceInterceptor(traceInterceptor());
        return advisor;
    }
}

@Traceable
@Service
public class UserServiceImpl implements UserService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void getUser(String id) {
        this.logger.info("id is {}", id);
    }
}
```

Aspect方式
```java
@Configuration
class TraceConfig {
    @Bean
    public TraceInterceptor traceInterceptor() {
        TraceInterceptor interceptor = new TraceInterceptor();

        // 仅日志记录
        DefaultTraceAttribute defaultTraceAttribute = new DefaultTraceAttribute();
        defaultTraceAttribute.setHandlers(DefaultTraceHandler.class);

        // 不同日志name、特殊TraceHandler
        DefaultTraceAttribute saveTraceAttribute = new DefaultTraceAttribute();
        saveTraceAttribute.setLoggerName("x.y.z");
        saveTraceAttribute.setHandlers(SaveTraceHandler.class);

        NameMatchTraceAttributeSource attributeSource = new NameMatchTraceAttributeSource();
        attributeSource.addTraceableMethod("save*", saveTraceAttribute);
        attributeSource.addTraceableMethod("*", defaultTraceAttribute);

        return interceptor;
    }

    @Bean
    public Advisor traceAttributeSourceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* org.xbdframework.context.trace.service.impl..*ServiceImpl.*(..))");

        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setAdvice(traceInterceptor());
        advisor.setPointcut(pointcut);
        
        return advisor;
    }
}

@Service
public class UserServiceImpl implements UserService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void getUser(String id) {
        this.logger.info("id is {}", id);
    }
}
```

### XML方式配置

```xml
<bean id="defaultTraceAttribute" class="org.xbdframework.context.trace.interceptor.DefaultTraceAttribute">
    <property name="handlers" value="org.xbdframework.context.trace.handler.DefaultTraceHandler"></property>
</bean>

<bean id="saveTraceAttribute" class="org.xbdframework.context.trace.interceptor.DefaultTraceAttribute">
    <property name="handlers" value="org.xbdframework.context.trace.handler.SaveTraceHandler"></property>
</bean>

<bean id="nameMatchTraceAttributeSource" class="org.xbdframework.context.trace.interceptor.NameMatchTraceAttributeSource">
    <property name="nameMap">
        <map>
            <entry key="save*" value-ref="saveTraceAttribute"></entry>
            <entry key="*" value-ref="defaultTraceAttribute"></entry>
        </map>
    </property>
</bean>

<!--  TraceInterceptor  -->
<bean id="traceInterceptor" class="org.xbdframework.context.trace.interceptor.TraceInterceptor">
    <property name="traceAttributeSource" ref="nameMatchTraceAttributeSource"></property>
</bean>

<bean id="userService" class="org.xbdframework.context.trace.service.impl.UserServiceImpl"></bean>

<aop:config>
    <aop:pointcut id="pointcut" expression="execution(* org.xbdframework.context.trace.service.impl..*ServiceImpl.*(..))"/>
    <aop:advisor advice-ref="traceInterceptor" pointcut-ref="pointcut"></aop:advisor>
</aop:config>
```

### TraceHandler

示例：

```java
public class DefaultTraceHandler implements TraceHandler {

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
```

说明：

* 可通过Autowired注入其它bean

### Traceable


* handlerRefs 与 handlers 选择其一赋值即可。如均赋值，以前者为准






