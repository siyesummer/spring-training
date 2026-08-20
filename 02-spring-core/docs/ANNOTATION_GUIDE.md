# Module 02：纯注解 / Java 配置实战引导

> 当前轮次：第二轮，纯注解 / Java 配置
> 当前状态：`已完成`（2026-08-20）
> 前置条件：已完成 XML 轮，并完成 `XML复盘.md` 中的容器、AOP、事务和后置处理器复盘。
> 目标：使用注解和 Java 配置重新实现同一个账户转账业务，比较 Bean 定义来源变化后，IoC、DI、生命周期、AOP 和事务机制是否改变。

## 1. 本轮规则和边界

本轮不加载 XML Spring 配置，不使用：

```java
ClassPathXmlApplicationContext
```

也不要在 `annotation` 包中使用：

```java
<bean>
<tx:advice>
<aop:config>
```

本轮使用：

```java
AnnotationConfigApplicationContext
@Configuration
@ComponentScan
@Component
@Service
@Repository
@Bean
@Aspect
@Around
@EnableAspectJAutoProxy
@Transactional
@EnableTransactionManagement
```

注意“纯注解”不等于所有对象都必须使用组件注解。业务类适合使用 `@Service`、`@Repository`，DataSource、JdbcTemplate、事务管理器等第三方对象不能修改源码，所以使用 `@Bean` 显式注册。两种 Bean 来源都属于 Java 配置方式的一部分。

XML 轮的源码保留在：

```text
cn.siyes.training.spring.xml
```

注解轮的源码全部放在：

```text
cn.siyes.training.spring.annotation
```

不要把 XML 轮的 `AccountService`、`TimingInterceptor` 或后置处理器直接改成带注解的类。保留两套代码，才能真正比较两种配置方式。

## 2. 与 XML 轮的对照主线

XML 轮的对象组装是：

```text
applicationContext.xml
  -> <bean> 定义
  -> <constructor-arg> / <property>
  -> <aop:config>
  -> <tx:advice>
  -> Spring 创建目标对象和代理
```

注解轮改为：

```text
AnnotationConfig.class
  -> @ComponentScan 发现业务类
  -> @Component / @Service / @Repository 形成 BeanDefinition
  -> @Bean 注册第三方对象
  -> @EnableAspectJAutoProxy 创建 AOP 代理基础设施
  -> @EnableTransactionManagement 创建事务代理基础设施
  -> Spring 创建目标对象和代理
```

核心机制没有变：Spring 仍然先得到 BeanDefinition，再实例化、注入、初始化，最后可能返回代理对象。变化的是 BeanDefinition 的来源和事务/AOP 的声明方式。

## 3. 手动补充依赖

你需要手动编辑 `02-spring-core/pom.xml`。XML 轮已有以下依赖：

- `spring-context`
- `spring-aop`
- `spring-tx`
- `spring-jdbc`
- `aspectjweaver`
- `mysql-connector-j`

本轮新增 `@PostConstruct`、`@PreDestroy` 生命周期练习，需要补充：

```xml
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>3.0.0</version>
</dependency>
```

依赖用途：

| 依赖 | 本轮用途 |
| --- | --- |
| `spring-context` | `AnnotationConfigApplicationContext`、组件扫描和配置类 |
| `spring-aop` | AOP 代理和通知基础接口 |
| `spring-tx` | `@Transactional` 和事务拦截器 |
| `spring-jdbc` | `JdbcTemplate` 和 JDBC 事务管理器 |
| `aspectjweaver` | `@Aspect`、切点表达式和代理运行时支持 |
| `mysql-connector-j` | 连接现有 MySQL 数据库 |
| `jakarta.annotation-api` | `@PostConstruct`、`@PreDestroy` |

本轮不加入 Spring Boot、Spring MVC、MyBatis 或 JPA。依赖版本仍以当前 XML 轮已经验证的 Spring `6.2.8` 为准，不要在注解轮随意更换版本。

## 4. 建议包结构

先手动创建目录：

```text
src/main/java/cn/siyes/training/spring/annotation/
├─ AnnotationApplication.java
├─ config/
│  └─ AnnotationConfig.java
├─ aspect/
│  └─ TimingAspect.java
├─ lifecycle/
│  └─ AnnotationLifecycleProbe.java
├─ postprocessor/
│  ├─ AnnotationProbeView.java
│  ├─ AnnotationPostProcessorProbe.java
│  ├─ AnnotationProbeViewWrapper.java
│  ├─ AnnotationDefinitionPostProcessor.java
│  ├─ AnnotationBeanPostProcessor.java
│  └─ AnnotationDynamicRegistryPostProcessor.java  # 可选动态注册练习
├─ repository/
│  ├─ AccountRepository.java
│  └─ AuditLogRepository.java
├─ service/
│  ├─ AccountService.java
│  └─ proxy/
│     ├─ SelfInvocationService.java
│     └─ ExternalInvocationService.java
└─ exception/
   └─ TransferException.java
```

本轮可以复用数据库表：

```text
spring_training_core.accounts
spring_training_core.transfer_logs
```

不需要重新建数据库，也不要让注解轮和 XML 轮同时启动并修改同一笔数据。每次运行前先在 Navicat 中执行 XML 轮引导中的重置 SQL。

## 5. 第一步：使用 AnnotationConfigApplicationContext

### 5.1 先写最小配置类

创建：

```text
src/main/java/cn/siyes/training/spring/annotation/config/AnnotationConfig.java
```

添加 `@Configuration`：

```java
package cn.siyes.training.spring.annotation.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AnnotationConfig {
}
```

`@Configuration` 的作用不是“启动 Spring”，而是告诉 Spring：这是一个包含 Bean 定义方法的配置类。当前还没有写 `@Bean`，所以它只是一条最小配置定义。

创建启动类：

```text
src/main/java/cn/siyes/training/spring/annotation/AnnotationApplication.java
```

```java
package cn.siyes.training.spring.annotation;

import cn.siyes.training.spring.annotation.config.AnnotationConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AnnotationApplication {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AnnotationConfig.class)) {
            System.out.println("Annotation Spring Context started");
        }
    }
}
```

这里与 XML 轮的差异是：

```java
new ClassPathXmlApplicationContext("...")
```

变成：

```java
new AnnotationConfigApplicationContext(AnnotationConfig.class)
```

业务代码仍然负责创建 ApplicationContext；Context 内部仍然负责创建和管理 Bean。

## 6. 第二步：组件扫描和构造器注入

### 6.1 开启组件扫描

在 `AnnotationConfig` 上添加：

```java
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan("cn.siyes.training.spring.annotation")
public class AnnotationConfig {
}
```

`@ComponentScan` 会扫描指定包下带有组件注解的类，并把它们转为 BeanDefinition。扫描范围要足够窄：不要扫描 `cn.siyes.training.spring`，否则可能把 XML 轮的类或其他实验类一起纳入容器。

### 6.2 编写 Repository

创建注解轮的 `AccountRepository`：

```text
cn.siyes.training.spring.annotation.repository.AccountRepository
```

添加：

```java
@Repository
public class AccountRepository {
}
```

创建 `AuditLogRepository`，同样使用：

```java
@Repository
public class AuditLogRepository {
}
```

`@Repository` 有两层意义：

1. 它是组件扫描识别的 Bean 注册标记。
2. 它表达类属于持久化层，后续可以参与 Spring 数据访问异常转换等基础设施。

### 6.3 编写 Service 并使用构造器注入

创建：

```text
cn.siyes.training.spring.annotation.service.AccountService
```

使用：

```java
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;

    public AccountService(AccountRepository accountRepository,
                          AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
    }
}
```

Spring 4.3 之后，如果类只有一个构造器，可以不写 `@Autowired`，Spring 会自动使用它进行构造器注入。你可以先显式写 `@Autowired` 观察，再删除它，确认单构造器规则：

```java
@Autowired
public AccountService(...) {
}
```

长期建议保留单构造器不加 `@Autowired`，这样依赖关系由 Java 结构直接表达。

### 6.4 最小验收

在启动类中获取：

```java
AccountService service =
        context.getBean(AccountService.class);

System.out.println(service.getClass().getName());
```

预期可以取得 `AccountService`，并且构造器被 Spring 调用。此时还没有 AOP，拿到的应该是普通类实例。

对照 XML：

```text
XML 的 <bean class="...AccountService">
  -> 注解的 @Service

XML 的 <constructor-arg ref="...">
  -> 注解轮的单构造器
```

## 7. 第三步：用 @Bean 注册第三方对象和环境配置

业务类可以加 `@Service`，但 `DriverManagerDataSource`、`JdbcTemplate` 和 `DataSourceTransactionManager` 是第三方类，不能在它们的源码上添加 `@Component`。它们由 `@Bean` 方法注册。

### 7.1 使用 Environment 读取环境变量

在 `AnnotationConfig` 中添加三个 `@Bean`：

```java
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

@Bean
public DataSource dataSource(Environment environment) {
    DriverManagerDataSource dataSource =
            new DriverManagerDataSource();
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(environment.getRequiredProperty("DB_URL"));
    dataSource.setUsername(environment.getRequiredProperty("DB_USERNAME"));
    dataSource.setPassword(environment.getRequiredProperty("DB_PASSWORD"));
    return dataSource;
}
```

`Environment` 是 Spring 对环境变量、系统属性和配置源的统一抽象。`getRequiredProperty` 找不到变量时会立即报错，比返回 `null` 后在 JDBC 连接阶段才失败更容易定位。

启动配置仍使用 XML 轮相同的环境变量：

```text
DB_URL=jdbc:mysql://localhost:3306/spring_training_core?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=spring_training
DB_PASSWORD=你的密码
```

注意：这里的 `Environment` 是 Spring 容器提供的参数，不需要 `@Autowired`。Spring 会解析 `@Bean` 方法参数并传入容器中的 `Environment` 对象。

### 7.2 注册 JdbcTemplate 和事务管理器

继续在 `AnnotationConfig` 中添加：

```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Bean
public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
}

@Bean
public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
}
```

这里的三个 `@Bean` 方法形成：

```text
Environment
  -> dataSource
  -> jdbcTemplate
  -> transactionManager
```

`@Bean` 方法的参数不是普通方法调用参数，而是 Spring 解析依赖后传入的 Bean 引用。不要在 `jdbcTemplate` 方法里手动 `new DriverManagerDataSource()`，否则会绕过容器中已经配置好的 DataSource。

### 7.3 最小连接验证

先让两个 Repository 接收 `JdbcTemplate` 构造器参数，但暂时只增加一个查询方法：

```java
public BigDecimal findBalance(long accountId) {
    return jdbcTemplate.queryForObject(
            "SELECT balance FROM accounts WHERE id = ?",
            BigDecimal.class,
            accountId);
}
```

启动类中调用：

```java
AccountRepository repository =
        context.getBean(AccountRepository.class);
System.out.println(repository.findBalance(1L));
```

看到数据库中的余额，说明：

```text
环境变量 -> Environment -> DataSource -> JdbcTemplate -> Repository
```

这一步通过后，再补扣款、入账和日志方法。不要一开始就同时调试 AOP 和事务。

## 8. 第四步：注解轮的数据库 Repository 和转账 Service

### 8.1 Repository 方法

把注解轮 `AccountRepository` 改成：

```java
@Repository
public class AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int debit(long accountId, BigDecimal amount) {
        return jdbcTemplate.update("""
                UPDATE accounts
                SET balance = balance - ?
                WHERE id = ? AND balance >= ?
                """, amount, accountId, amount);
    }

    public int credit(long accountId, BigDecimal amount) {
        return jdbcTemplate.update("""
                UPDATE accounts
                SET balance = balance + ?
                WHERE id = ?
                """, amount, accountId);
    }
}
```

`AuditLogRepository` 使用：

```java
@Repository
public class AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(long fromAccountId,
                      long toAccountId,
                      BigDecimal amount) {
        return jdbcTemplate.update("""
                INSERT INTO transfer_logs
                    (from_account_id, to_account_id, amount)
                VALUES (?, ?, ?)
                """, fromAccountId, toAccountId, amount);
    }
}
```

SQL、`BigDecimal`、受影响行数判断和 XML 轮完全相同。改变的是 Repository 如何被注册和获得 `JdbcTemplate`。

### 8.2 Service 业务边界

在 `AccountService` 中实现：

```java
@Service
public class AccountService {
    // 构造器依赖

    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public void transfer(long fromAccountId,
                         long toAccountId,
                         BigDecimal amount) {
        // 复用 XML 轮已经验证过的六步业务顺序
    }
}
```

方法体仍然按这个顺序手写：

1. 校验两个账户不能相同。
2. 校验金额大于零。
3. `debit` 返回值不是 `1` 时抛出 `TransferException`。
4. `credit` 返回值不是 `1` 时抛出 `TransferException`。
5. `insert` 返回值不是 `1` 时抛出 `TransferException`。
6. 不在 Service 中手写 `commit()`。

`@Transactional` 只是把 XML 的事务规则移动到了方法上；事务仍然由 `PlatformTransactionManager` 管理，仍然通过代理生效。

## 9. 第五步：启用注解式事务和 AOP

### 9.1 启用事务管理

在 `AnnotationConfig` 上添加：

```java
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan("cn.siyes.training.spring.annotation")
@EnableTransactionManagement
public class AnnotationConfig {
}
```

`@EnableTransactionManagement` 会向容器注册事务基础设施，使 Spring 查找 `@Transactional` 并创建事务代理。它不会自己创建数据库连接；仍然需要 `@Bean transactionManager`。

### 9.2 手写注解切面

创建：

```text
cn.siyes.training.spring.annotation.aspect.TimingAspect
```

使用 `@Aspect` 和 `@Component`：

```java
@Aspect
@Component
public class TimingAspect {
    @Around("execution(* cn.siyes.training.spring.annotation.service..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long cost = (System.nanoTime() - start) / 1_000_000;
            System.out.println(
                    joinPoint.getSignature().getName() + " cost=" + cost + "ms");
        }
    }
}
```

这个 `@Around` 与 XML 轮的 `MethodInterceptor` 对照：

```text
MethodInterceptor.invoke(MethodInvocation)
  -> ProceedingJoinPoint.proceed()

XML pointcut
  -> @Around("execution(...)")
```

`proceed()` 仍然是放行点；不调用它，目标方法仍然不会执行。

### 9.3 启用自动代理

在 `AnnotationConfig` 上添加：

```java
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(proxyTargetClass = true)
```

完整配置类此时至少包含：

```java
@Configuration
@ComponentScan("cn.siyes.training.spring.annotation")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
public class AnnotationConfig {
    // dataSource、jdbcTemplate、transactionManager @Bean
}
```

`proxyTargetClass = true` 对应 XML 的 `proxy-target-class="true"`，使用 CGLIB 类代理。当前 `AccountService` 没有实现接口，所以仍按 `AccountService.class` 获取更直观。若改用接口，则可以比较 JDK 动态代理和 CGLIB 的差异。

### 9.4 代理验收

启动类中获取注解轮 Service：

```java
AccountService service = context.getBean(AccountService.class);
System.out.println(service.getClass().getName());
service.transfer(1L, 2L, new BigDecimal("100.00"));
```

先观察：

- 是否打印 CGLIB 代理类。
- 是否打印 `transfer` 耗时。
- 是否正常扣款、入账和插入日志。
- 是否能在 Navicat 中看到余额和日志变化。

此时 AOP 和事务可能叠加在同一个代理对象上。不要只看“出现了代理类”，还要解释 `@Around` 和 `@Transactional` 如何成为代理中的多个拦截器。

## 10. 第六步：注解生命周期

创建：

```text
cn.siyes.training.spring.annotation.lifecycle.AnnotationLifecycleProbe
```

```java
@Component
public class AnnotationLifecycleProbe {
    @PostConstruct
    public void init() {
        System.out.println("annotation probe init");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("annotation probe destroy");
    }
}
```

启动并关闭 `AnnotationConfigApplicationContext`，观察：

```text
Context 创建
  -> 构造器
  -> 依赖注入
  -> @PostConstruct
  -> 业务调用
Context close
  -> @PreDestroy
```

这与 XML 的 `init-method` / `destroy-method` 是同一生命周期位置的两种声明方式：

```text
XML：在 <bean> 上写方法名
注解：在类的方法上写 @PostConstruct / @PreDestroy
```

不要在业务代码中手动调用 `init()` 或 `destroy()`。它们是容器生命周期回调。

## 11. 第七步：注解方式的后置处理器

XML 轮的后置处理器不是只能用 XML 注册。注解轮通过组件扫描或 `@Bean` 把处理器注册到容器，底层仍然调用同一组 Spring 扩展接口。

这一节不要修改 `AccountService`、Repository 或事务代码。它们已经同时受到 AOP 和事务代理影响，再拿它们测试包装对象会把多个机制混在一起。单独创建 Probe，观察结果更清楚。

### 11.1 先建立执行时序

本节会用到三个名字相近、但处理对象完全不同的接口：

| 扩展接口 | 处理对象 | 执行时机 | 本次练习 |
| --- | --- | --- | --- |
| `BeanDefinitionRegistryPostProcessor` | BeanDefinition 注册表 | 已有配置开始注册后、普通 Bean 实例化前 | 动态增加一个 Probe 定义 |
| `BeanFactoryPostProcessor` | 已注册的 BeanDefinition | 普通 Bean 实例化前 | 修改 Probe 的 `label` 属性值 |
| `BeanPostProcessor` | 已经实例化并完成属性注入的 Bean | 初始化方法前后 | 修改实例并在初始化后返回包装对象 |

简化时序：

```text
读取 AnnotationConfig
  -> @ComponentScan 扫描并注册 BeanDefinition
  -> BeanDefinitionRegistryPostProcessor 可继续注册定义
  -> BeanFactoryPostProcessor 修改已有定义
  -> 注册 BeanPostProcessor
  -> 创建普通 Bean
  -> 属性注入
  -> BeanPostProcessor.beforeInitialization
  -> @PostConstruct
  -> BeanPostProcessor.afterInitialization
  -> singleton 最终对象供 getBean() 使用
```

需要准确理解：

- `BeanFactoryPostProcessor` 不是在 BeanDefinition 生成前执行，而是在定义已经注册、普通 Bean 尚未实例化时执行。
- `BeanPostProcessor` 不直接访问 `singletonObjects`。它把处理后的对象返回给容器，Spring 再使用最终返回值。
- `BeanPostProcessor` 处理的是 Bean 创建过程，不是每次业务方法调用；每次调用拦截属于 AOP 代理的职责。

### 11.2 创建专用 Probe、接口和包装器

在注解包下创建：

```text
cn.siyes.training.spring.annotation.postprocessor
├─ AnnotationProbeView.java
├─ AnnotationPostProcessorProbe.java
├─ AnnotationProbeViewWrapper.java
├─ AnnotationDefinitionPostProcessor.java
└─ AnnotationBeanPostProcessor.java
```

先定义共同接口。后面 `BeanPostProcessor` 会返回包装对象，因此调用方必须面向共同接口，而不是强转为原始实现类：

```java
public interface AnnotationProbeView {
    void print();
}
```

创建被处理的 Probe，并显式指定 Bean 名称，避免依赖默认命名规则：

```java
@Component("annotationProbe")
public class AnnotationPostProcessorProbe
        implements AnnotationProbeView {

    private String label = "from-component";

    public AnnotationPostProcessorProbe() {
        System.out.println("1. AnnotationProbe 构造器");
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @PostConstruct
    public void init() {
        System.out.println("3. @PostConstruct, label=" + label);
    }

    @Override
    public void print() {
        System.out.println("6. Probe.print, label=" + label);
    }
}
```

创建包装器。它不是组件，不需要加 `@Component`；它由后置处理器手动创建：

```java
public class AnnotationProbeViewWrapper
        implements AnnotationProbeView {

    private final AnnotationProbeView target;

    public AnnotationProbeViewWrapper(AnnotationProbeView target) {
        this.target = target;
    }

    @Override
    public void print() {
        System.out.println("5. Wrapper before");
        target.print();
    }
}
```

阶段检查：此时还没有后置处理器。运行并按接口获取：

```java
AnnotationProbeView probe = context.getBean(
        "annotationProbe",
        AnnotationProbeView.class
);
probe.print();
```

预期只看到构造、`@PostConstruct` 和 `print()`，`label` 为 `from-component`。

### 11.3 手写 `BeanFactoryPostProcessor`

创建 `AnnotationDefinitionPostProcessor`：

```java
@Component
public class AnnotationDefinitionPostProcessor
        implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory
    ) throws BeansException {
        BeanDefinition definition =
                beanFactory.getBeanDefinition("annotationProbe");

        definition.getPropertyValues().add(
                "label",
                "from-BeanFactoryPostProcessor"
        );

        System.out.println(
                "0. BeanFactoryPostProcessor 修改 BeanDefinition"
        );
    }
}
```

需要的主要导包：

```java
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
```

这里修改的是创建规则中的属性值，不是已经创建的 Probe。随后 Spring 实例化 Probe 时，会根据该 BeanDefinition 调用 `setLabel(...)`。

不要在这个回调中写：

```java
beanFactory.getBean("annotationProbe");
```

这会让普通 Bean 提前实例化，此时其他 `BeanPostProcessor` 可能还没有全部注册，Bean 可能错过代理或生命周期处理。

阶段检查：重新运行后，`@PostConstruct` 和 `print()` 中的 `label` 应变成：

```text
from-BeanFactoryPostProcessor
```

这证明修改的是 BeanDefinition，并且修改在 Probe 实例化前已经生效。

### 11.4 手写 `BeanPostProcessor`

创建 `AnnotationBeanPostProcessor`，只处理明确命名的目标 Bean：

```java
@Component
public class AnnotationBeanPostProcessor
        implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(
            Object bean,
            String beanName
    ) throws BeansException {
        if (!"annotationProbe".equals(beanName)) {
            return bean;
        }

        AnnotationPostProcessorProbe probe =
                (AnnotationPostProcessorProbe) bean;
        probe.setLabel("from-beforeInitialization");
        System.out.println("2. BeanPostProcessor.before");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName
    ) throws BeansException {
        if (!"annotationProbe".equals(beanName)) {
            return bean;
        }

        System.out.println("4. BeanPostProcessor.after 返回包装对象");
        return new AnnotationProbeViewWrapper(
                (AnnotationProbeView) bean
        );
    }
}
```

需要的主要导包：

```java
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
```

重新运行后重点观察顺序。由于 `beforeInitialization` 在 `@PostConstruct` 之前执行，初始化日志中的 `label` 应是：

```text
from-beforeInitialization
```

在入口中增加：

```java
AnnotationProbeView probe = context.getBean(
        "annotationProbe",
        AnnotationProbeView.class
);

System.out.println("getBean 类型=" + probe.getClass().getName());
probe.print();
```

预期 `getBean` 得到 `AnnotationProbeViewWrapper`，调用顺序是：

```text
Wrapper before
  -> 原始 Probe.print
```

这证明 `postProcessAfterInitialization` 的返回值成为容器最终对外提供的对象。注意：

- 处理器本身是容器基础设施。
- 不要无条件把所有 Bean 强转成 Probe 类型。
- 包装后按共同接口获取，不要按原始实现类强转。
- 这仍然是 Bean 创建阶段的一次处理，不是每次业务方法调用。

下面的获取方式会因为最终对象是 Wrapper 而失败：

```java
context.getBean(AnnotationPostProcessorProbe.class);
```

这与 XML 轮遇到的 `ClassCastException` 是同一个原因：容器最终返回的是包装器，不再是原始实现类。面向 `AnnotationProbeView` 获取可以同时兼容原始对象和包装对象。

再连续获取两次：

```java
AnnotationProbeView first = context.getBean(
        "annotationProbe", AnnotationProbeView.class
);
AnnotationProbeView second = context.getBean(
        "annotationProbe", AnnotationProbeView.class
);
System.out.println(first == second);
```

默认 singleton 下应为 `true`，而 `BeanPostProcessor.after` 只在创建阶段打印一次。这说明每次 `getBean()` 不是重新执行包装逻辑，而是取得已经缓存的最终对象。

### 11.5 对照 `@Component` 与 `@Bean` 注册处理器

本次先使用 `@Component`，让 `@ComponentScan` 发现处理器：

```text
@ComponentScan
  -> 扫描 AnnotationDefinitionPostProcessor
  -> 扫描 AnnotationBeanPostProcessor
  -> Spring 识别它们实现的扩展接口并调用
```

也可以在 `AnnotationConfig` 中通过 `@Bean` 注册。对于返回 `BeanFactoryPostProcessor` 的工厂方法，推荐使用 `static`：

```java
@Bean
public static AnnotationDefinitionPostProcessor
        annotationDefinitionPostProcessor() {
    return new AnnotationDefinitionPostProcessor();
}
```

`static @Bean` 允许 Spring 在不提前实例化配置类的情况下注册容器级后置处理器。当前练习只选一种注册方式，不要同时给类加 `@Component` 又在配置类中声明同一个 `@Bean`，否则会注册两个处理器，导致日志重复或处理两次。

### 11.6 可选：动态注册 BeanDefinition

前两项完成后，再练习更早的定义注册扩展点。创建：

```text
cn.siyes.training.spring.annotation.postprocessor.AnnotationDynamicRegistryPostProcessor
```

使用 `@Component` 注册处理器：

```java
@Component
public class AnnotationDynamicRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(
            BeanDefinitionRegistry registry
    ) throws BeansException {
        if (registry.containsBeanDefinition(
                "annotationDynamicProbe"
        )) {
            return;
        }

        RootBeanDefinition definition =
                new RootBeanDefinition(
                        AnnotationPostProcessorProbe.class
                );
        definition.getPropertyValues().add(
                "label",
                "from-registry-post-processor"
        );

        registry.registerBeanDefinition(
                "annotationDynamicProbe",
                definition
        );
        System.out.println(
                "RegistryPostProcessor 动态注册 BeanDefinition"
        );
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory
    ) throws BeansException {
        // 本练习不需要继续修改 BeanDefinition。
    }
}
```

需要的主要导包：

```java
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;
```

入口中获取动态 Bean：

```java
AnnotationProbeView dynamicProbe = context.getBean(
        "annotationDynamicProbe",
        AnnotationProbeView.class
);
dynamicProbe.print();
```

当前 `AnnotationBeanPostProcessor` 只匹配 `annotationProbe`，所以动态 Bean 会正常完成属性注入和 `@PostConstruct`，但不会被包装。为了证明它仍然经过所有 `BeanPostProcessor`，可以在处理器开头临时打印：

```java
if (bean instanceof AnnotationProbeView) {
    System.out.println("观察到 Probe Bean: " + beanName);
}
```

预期同时看到：

```text
观察到 Probe Bean: annotationProbe
观察到 Probe Bean: annotationDynamicProbe
```

动态注册和是否包装是两个独立问题。动态 Bean 同样参与标准创建流程，只是当前后置处理器根据 Bean 名称主动跳过了它。

### 11.7 本节最小验收

完成后用自己的话回答并通过控制台证明：

1. `BeanDefinitionRegistryPostProcessor`、`BeanFactoryPostProcessor` 和 `BeanPostProcessor` 分别处理什么对象？
2. 为什么 `BeanFactoryPostProcessor` 修改属性后，`@PostConstruct` 能看到新值？
3. 为什么 `postProcessAfterInitialization` 返回 Wrapper 后，不能再按原始实现类获取？
4. 为什么连续两次 `getBean()` 不会执行两次包装逻辑？
5. 注解轮与 XML 轮相比，改变的是处理器的注册方式，还是处理器接口本身的生命周期和职责？

本节不要求给事务或 AOP 再增加后置处理器。能清楚证明“定义层修改”和“实例层处理”的差异，就达到本次训练目标。

## 12. 第八步：事务成功提交与异常回滚

### 12.1 正常提交

在 Navicat 中先重置：

```sql
USE spring_training_core;

DELETE FROM transfer_logs;
UPDATE accounts SET balance = 1000.00 WHERE id = 1;
UPDATE accounts SET balance = 500.00 WHERE id = 2;
```

运行：

```java
service.transfer(1L, 2L, new BigDecimal("100.00"));
```

预期：

```text
账户 1：900.00
账户 2：600.00
日志：新增 1 条
```

### 12.2 主动异常回滚

在日志插入成功后临时添加：

```java
throw new TransferException("annotation round rollback test");
```

再次运行后，在 Navicat 查询账户和日志：

```text
账户 1：1000.00
账户 2：500.00
日志：0 条
```

验证完成后删除模拟异常。注解轮的回滚机制与 XML 轮相同：异常穿过事务代理后，`DataSourceTransactionManager` 回滚当前事务连接中尚未提交的修改。

### 12.3 事务边界复盘

```text
外部调用 AccountService 代理
  -> @Transactional 事务拦截器
  -> DataSourceTransactionManager
  -> JdbcTemplate 使用事务绑定的 Connection
  -> debit / credit / insert
  -> 正常返回 commit
  -> 异常返回 rollback
```

`@Transactional` 改变的是事务规则的声明位置，不是事务实现本身。不要在 Repository 或 Service 中手动调用 `commit()`。

## 13. 第九步：自调用代理失效复盘

这一节要验证一个非常容易被忽略的边界：`@Transactional`、`@Aspect` 等基于 Spring 代理的能力，只能拦截“从代理对象进入”的方法调用。目标对象内部使用 `this` 调用时，不会再次经过代理。

### 13.1 先画出两个调用方向

Spring AOP 的对象关系可以先简化为：

```text
外部调用：
main -> SelfInvocationService 代理 -> 目标对象方法
                              -> AOP / 事务拦截器生效

目标对象内部调用：
目标对象.outer() -> this.inner()
                  -> 直接到目标对象
                  -> 绕过代理和拦截器
```

这不是 `this` 语法特殊，而是因为 `this` 保存的是目标对象自身引用，不是 Spring 创建的代理引用。代理无法拦截没有经过自己的调用。

### 13.2 创建一个只观察代理的 Service

不要把已经连接数据库的 `AccountService` 拆改成实验代码，单独创建目录：

```text
cn.siyes.training.spring.annotation.service.proxy
├─ SelfInvocationService.java
└─ ExternalInvocationService.java
```

创建目标 Service：

```java
package cn.siyes.training.spring.annotation.service.proxy;

import org.springframework.stereotype.Service;

@Service
public class SelfInvocationService {

    public void outer() {
        System.out.println("outer body");
        this.inner();
    }

    public void inner() {
        System.out.println("inner body");
    }
}
```

当前 `TimingAspect` 的切点是：

```java
execution(* cn.siyes.training.spring.annotation.service..*(..))
```

`service..*` 中的 `..` 会匹配 `service` 包及其任意层级子包，因此 `service.proxy` 下的两个实验类会直接进入现有切点，不需要修改 `TimingAspect`。如果错误地放到 `annotation.proxy`，方法体仍然能执行，但不会出现耗时日志，因为它不在切点范围内。

### 13.3 先验证 `this.inner()` 绕过代理

在 `AnnotationApplication` 中通过容器获取目标 Bean：

```java
SelfInvocationService service = annotationContext.getBean(
        SelfInvocationService.class
);

System.out.println("--- outer 调用 this.inner ---");
service.outer();
```

不要这样写：

```java
SelfInvocationService service = new SelfInvocationService();
```

手动 `new` 得到的对象没有 Spring 代理，连 `service.outer()` 的外层 AOP 也不会执行，无法验证自调用边界。

预期现象：

```text
--- outer 调用 this.inner ---
outer body
inner body
outer cost=...ms
```

这里只有 `outer` 的耗时日志，没有 `inner` 的耗时日志。`outer()` 是从代理进入的，所以被切面拦截；`inner()` 是目标对象内部的 `this.inner()`，所以直接执行目标方法。

### 13.4 对比从代理进入的直接调用

继续在入口中调用同一个容器对象的 `inner()`：

```java
System.out.println("--- 外部直接调用 inner ---");
service.inner();
```

预期：

```text
--- 外部直接调用 inner ---
inner body
inner cost=...ms
```

这次 `inner()` 是从 `service` 代理进入的，所以切面生效。对比这两次调用：

```text
service.outer()
  -> 目标对象内部 this.inner()，不经过代理

service.inner()
  -> 外部通过代理进入，经过代理
```

### 13.5 通过另一个 Service 重新进入代理

真实项目中更推荐把需要独立事务或独立横切处理的职责拆成另一个 Bean，而不是想办法在同一个类里绕过代理。创建：

```java
package cn.siyes.training.spring.annotation.service.proxy;

import org.springframework.stereotype.Service;

@Service
public class ExternalInvocationService {

    private final SelfInvocationService target;

    public ExternalInvocationService(SelfInvocationService target) {
        this.target = target;
    }

    public void callInner() {
        target.inner();
    }
}
```

在入口中获取并调用：

```java
ExternalInvocationService external = annotationContext.getBean(
        ExternalInvocationService.class
);

System.out.println("--- 另一个 Service 调用 inner ---");
external.callInner();
```

因为构造器注入的 `target` 是 Spring 暴露的代理引用，调用链变成：

```text
main
  -> ExternalInvocationService 代理
  -> ExternalInvocationService.callInner()
  -> SelfInvocationService 代理
  -> SelfInvocationService.inner()
  -> TimingAspect
```

预期同时看到 `inner body` 和 `inner cost=...ms`。这证明拆分 Bean 后，调用重新从代理入口进入，AOP 能够生效。

### 13.6 与 `@Transactional` 的对应关系

把 `@Transactional` 标记在 `inner()` 上，可以观察同样的边界：

```java
@Transactional
public void inner() {
    System.out.println(
            TransactionSynchronizationManager
                    .isActualTransactionActive()
    );
}
```

需要导入：

```java
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
```

从 `outer()` 内部执行 `this.inner()` 时，`inner()` 上声明的事务拦截器不会被触发；从入口直接调用 `service.inner()`，或从 `ExternalInvocationService` 调用时，才会经过事务代理。这个实验只观察代理边界，不需要修改账户数据。

注意：如果 `outer()` 自己已经处于事务中，`this.inner()` 仍然没有经过 `inner()` 的代理拦截器，只是它可能继续运行在 `outer()` 已经建立的事务里。不要把“没有新建事务”和“当前没有事务”混为一谈。

### 13.7 常见错误和可选方案

- **手动 `new` Service**：不会有 Spring AOP、事务或依赖注入。
- **把 `private` 方法作为切点验证**：基于代理的 Spring AOP 通常不能拦截 private 方法；先使用 public 方法。
- **把 `final` 方法作为切点验证**：CGLIB 不能覆写 final 方法，也就无法按这种方式增强。
- **只观察业务方法打印**：`inner body` 能打印不代表 AOP 生效，必须同时观察切面日志或事务状态。
- **把自己注入自己作为首选方案**：虽然可以通过代理引用解决部分场景，但会增加循环依赖和理解成本，不作为本阶段推荐方式。

还可以在配置类中开启代理暴露：

```java
@EnableAspectJAutoProxy(
        proxyTargetClass = true,
        exposeProxy = true
)
```

再使用：

```java
((SelfInvocationService) AopContext.currentProxy()).inner();
```

这能让内部调用重新取得当前代理，但会让业务代码依赖 Spring AOP 上下文，且必须保证调用发生在代理线程中。先完成“拆分 Service”方案，再把它作为了解性对照，不要把它当成默认业务写法。

### 13.8 本节最小验收

完成后应能通过日志回答：

1. 为什么 `service.outer()` 有 `outer cost`，但 `this.inner()` 没有 `inner cost`？
2. 为什么从入口直接调用 `service.inner()` 会有 `inner cost`？
3. 为什么 `ExternalInvocationService` 调用 `target.inner()` 后，AOP 又生效了？
4. `@Transactional` 自调用失效时，是否一定代表当前没有事务？
5. 为什么手动 `new` 的对象无法验证 Spring 代理？

完成本节后，注解轮的 AOP 练习才同时覆盖“代理能生效”和“代理会失效的边界”。

## 14. 注解轮验收清单

按顺序完成，不要一次把所有注解加上：

- [x] `AnnotationConfigApplicationContext` 能加载 `@Configuration`。
- [x] `@ComponentScan` 能发现 annotation 包中的 `@Repository`、`@Service`。
- [x] Service 通过单构造器完成 Repository 注入。
- [x] `@Bean` 能创建 DataSource、JdbcTemplate 和事务管理器。
- [x] `Environment` 能读取 IDEA 运行配置中的数据库环境变量。
- [x] `@PostConstruct`、`@PreDestroy` 能观察生命周期。
- [x] `@Aspect`、`@Around` 和 `@EnableAspectJAutoProxy` 能创建耗时代理。
- [x] `@Transactional` 和 `@EnableTransactionManagement` 能完成成功提交。
- [x] 主动异常后账户和日志一起回滚。
- [x] 能通过 `BeanDefinitionRegistryPostProcessor`、`BeanFactoryPostProcessor`、`BeanPostProcessor` 解释定义层和实例层扩展点。
- [x] 能解释注解轮与 XML 轮的 BeanDefinition 来源差异。
- [x] 能通过实际日志解释自调用为什么绕过 AOP 和事务代理。

## 15. 本轮过程中的暂停点

完成每一组后暂停并确认：

1. 容器组装：`@Configuration`、`@ComponentScan`、`@Component` 和 `@Bean`。
2. 数据库组装：`Environment -> DataSource -> JdbcTemplate -> Repository`。
3. 代理组装：`@Aspect`、`@Around`、`@Transactional` 如何共同进入代理。
4. 生命周期和后置处理器：注解声明如何改变注册方式，但不改变回调时机。
5. 事务和复盘：注解只是声明位置变化，事务边界和连接管理机制没有变化。

本轮已完成 `ANNOTATION复盘.md`，并通过正常提交、主动异常回滚、故障恢复、后置处理器和自调用代理边界的手动验收。`02-spring-core` 已于 2026-08-20 标记为“已完成”。
