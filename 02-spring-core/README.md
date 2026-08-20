# Module 02：Spring Core 两轮练习总览

> 当前状态：`已完成`
> 完成日期：2026-08-20
> 完成轮次：纯 XML 配置、纯注解 / Java 配置及两轮复盘均已完成
> 阶段结论：能够分别使用 XML 与注解完成 Spring Core 的容器组装、生命周期、AOP、事务和后置处理器练习，并解释两种方式的共同机制与边界。

## 1. 为什么使用一个 Module

XML 和注解是 Spring 容器的两种配置入口，底层要理解的仍然是同一组机制：

- IoC 容器解决什么问题。
- Bean 如何被定义、创建、注入、初始化和销毁。
- Service、Repository 等对象如何协作。
- AOP 如何通过代理包住目标方法。
- 事务如何通过事务管理器和代理控制边界。

因此本阶段只创建一个 `02-spring-core` Module，分成两轮完成：

```text
第一轮：纯 XML 配置
  -> 第二轮：纯注解 / Java 配置
  -> 对照两种配置方式的差异
```

如果拆成两个 Module，同一套 Spring Core 机制会被人为分散，反而不利于比较。两轮使用同一个业务主题，但 XML 和注解实现放在不同包中，避免配置互相污染。

## 2. 项目类型和范围

这是普通 Maven Java Module，打包类型为 `jar`：

```text
02-spring-core/
├─ pom.xml
├─ README.md
├─ docs/
│  ├─ XML_GUIDE.md
│  ├─ XML复盘.md
│  ├─ ANNOTATION_GUIDE.md
│  └─ ANNOTATION复盘.md
└─ src/
   ├─ main/java/cn/siyes/training/spring/
   ├─ main/resources/
   └─ test/java/
```

本阶段暂时不使用：

- Tomcat、Servlet、JSP 或 WAR。
- Spring MVC 和 Spring Boot。
- `@SpringBootApplication` 或 Spring Boot Starter。
- MyBatis、JPA 和复杂数据库业务。

前一阶段已经练习了 Servlet、Filter、Listener、Session 和 JDBC；本阶段先观察 Spring 如何管理普通 Java 对象，再理解它如何替代手动 `new` 和手动组装依赖。

## 3. 业务主题

两轮都使用一个最小的“账户转账与操作日志”业务：

```text
AccountService
  -> AccountRepository
  -> AuditLogRepository
```

IoC、DI、生命周期和 AOP 先通过最小普通 Java 对象观察；事务练习再接入已有 MySQL 环境，模拟：

```text
扣减转出账户余额 + 增加转入账户余额 + 写入操作日志
```

三步必须作为一个事务成功；任一步失败时，前面尚未提交的修改都要回滚。

## 4. 两轮练习安排

### 第一轮：纯 XML

详细步骤见 [XML_GUIDE.md](docs/XML_GUIDE.md)。XML 轮复盘见 [XML复盘.md](docs/XML复盘.md)。

XML 轮只使用：

- `ClassPathXmlApplicationContext`。
- `<bean>` 定义 Bean。
- `<constructor-arg>` 和 `<property>` 注入依赖。
- `init-method`、`destroy-method` 和生命周期接口。
- XML AOP 命名空间和普通 Aspect 类。
- XML 事务命名空间、事务管理器和事务 Advisor。
- `BeanDefinitionRegistryPostProcessor`、`BeanFactoryPostProcessor` 和 `BeanPostProcessor` 的扩展点对照。

XML 轮暂时禁止在业务类上使用：

```java
@Component
@Service
@Repository
@Configuration
@Bean
@Autowired
@Aspect
@Transactional
```

目标不是排斥注解，而是先看清 XML 中每一项配置如何对应容器行为。

### 第二轮：纯注解 / Java 配置

XML 轮完成并复盘后，第二轮使用：

详细步骤见 [ANNOTATION_GUIDE.md](docs/ANNOTATION_GUIDE.md)。

- `AnnotationConfigApplicationContext`。
- `@Component`、`@Service`、`@Repository`。
- 构造器注入和 `@Bean`。
- `@Configuration`、`@ComponentScan`。
- `@Aspect`、`@Before`、`@Around`、`@EnableAspectJAutoProxy`。
- `@Transactional` 和注解式事务配置。

第二轮不是把 XML 文件机械翻译成注解，而是重新观察：Bean 定义从哪里来、容器如何扫描、代理如何创建、哪些配置仍然需要显式声明。

## 5. 建议包结构

为了让两轮对照清楚，建议使用独立包：

```text
cn.siyes.training.spring
├─ common/
│  └─ model/                 # 两轮都可以复用的简单领域对象
├─ xml/
│  ├─ config/
│  ├─ repository/
│  ├─ service/
│  ├─ aspect/
│  └─ XmlApplication.java
└─ annotation/
   ├─ config/
   ├─ repository/
   ├─ service/
   ├─ aspect/
   └─ AnnotationApplication.java
```

XML 轮的 Service、Repository 和 Aspect 不使用 Spring 注解；注解轮再编写带注解的对应实现。这样可以明确比较两轮差异，而不是让上一轮的注解残留影响下一轮。

## 6. 两轮共同的核心验收

- 能解释 IoC 容器和手动 `new` 的差异。
- 能说明 Bean 定义、Bean 实例和代理对象不是同一个概念。
- 能通过构造器注入完成 Service -> Repository 的依赖组装。
- 能观察 Bean 初始化和销毁回调。
- 能证明切面在目标方法前后执行，并解释代理对象的作用。
- 能说明事务管理器、事务边界和异常回滚之间的关系。
- 能解释 XML 配置和注解配置只是 Bean 定义来源不同，核心容器机制并没有改变。

测试和 Maven 命令只用于确认工程能运行；本阶段重点是亲手写配置、观察容器行为并用自己的话解释设计。

## 7. 与前一阶段的衔接

前一阶段中你手动编写了：

```text
Servlet -> 手动 new Service -> 手动 new DAO -> JDBC
```

本阶段先用 XML 改变对象组装方式：

```text
Spring 容器 -> 创建 Service 和 DAO -> 注入依赖 -> 业务调用
```

第二轮再改成：

```text
Spring 容器扫描注解 / 读取 Java 配置
  -> 创建 Bean -> 注入依赖 -> 创建 AOP 代理 -> 业务调用
```

这条对照关系是本 Module 最重要的学习主线。

## 8. 本阶段完成情况

- 完成日期：2026-08-20。
- Module 形式：普通 Maven `jar`，不依赖 Tomcat、Spring MVC 或 Spring Boot。
- 独立实现：XML 与注解两套 AccountService、Repository、JdbcTemplate、AOP、声明式事务、生命周期 Probe 和后置处理器练习。
- 配置方式：XML 轮使用 `ClassPathXmlApplicationContext`、`<bean>`、XML AOP 和事务 Advisor；注解轮使用 `AnnotationConfigApplicationContext`、组件扫描、`@Bean`、`@Aspect` 和 `@Transactional`。
- 数据链路：Service 组合扣款、入账和转账日志写入，Repository 通过 `JdbcTemplate` 访问 MySQL，事务边界位于 Service。
- 后置处理器：实际验证 `BeanDefinitionRegistryPostProcessor` 动态注册定义、`BeanFactoryPostProcessor` 修改定义，以及 `BeanPostProcessor` 修改和包装实例。
- 代理边界：实际验证外部调用经过 AOP/事务代理，`this.inner()` 自调用绕过代理，拆分到另一个注入的 Service 后重新经过代理。

### 8.1 核心验收证据

- XML AOP：目标方法正常执行并输出耗时；去掉 `invocation.proceed()` 后目标方法被截断；获取到 CGLIB 代理类型。
- XML 事务：正常转账后余额为 `900.00`、`600.00` 并新增 `100.00` 日志；日志写入后主动抛出异常时余额恢复为 `1000.00`、`500.00` 且日志回滚；删除模拟异常后恢复成功提交。
- 注解生命周期：控制台观察到 `@PostConstruct` 和 `@PreDestroy` 回调。
- 注解 AOP：`transfer`、`outer`、`inner` 和 `callInner` 的耗时日志符合切点与代理入口预期。
- 注解事务：正常转账成功提交；日志写入后主动抛出受检 `TransferException` 时，账户和日志一起回滚；删除模拟异常后恢复提交。
- 自调用边界：`outer -> this.inner()` 中事务状态为 `false` 且没有独立 `inner` AOP 日志；从代理直接调用或通过另一个 Service 调用时事务状态为 `true`，并输出 `inner` AOP 日志。
- 后置处理器：动态 BeanDefinition 注册成功；定义属性在实例化前被修改；初始化后返回 Wrapper；两次获取 singleton 得到同一最终对象。
- 构建证据：2026-08-20 执行 `mvn -s C:\Users\siyesummer\.m2\settings.xml clean verify`，4 个既有测试全部通过，Java Web WAR 与 Spring Core JAR 均重新生成，Reactor 中三个项目均为 `SUCCESS`。

### 8.2 能力结论与边界

本阶段能够证明已具备 Spring Core 的基础独立实践能力：能手写最小容器配置，理解 BeanDefinition、Bean 实例与代理对象的区别，并能解释依赖注入、生命周期、AOP 和声明式事务的基本调用链。

本阶段不证明已经掌握 Spring MVC、Spring Boot 自动配置、连接池调优、复杂事务传播、循环依赖或生产级 Spring 工程设计。下一阶段进入 `03-spring-mvc`，重点理解 `DispatcherServlet`、Controller、参数绑定、JSON 转换、校验和统一异常处理。
