# Annotation复盘

> 完成状态：`已完成`（2026-08-20）
> 注解 / Java 配置和 XML 的主要差异，是 BeanDefinition 的来源和注册方式不同；容器创建对象、依赖注入、生命周期、后置处理器、AOP 和事务代理等核心机制并没有因此改变。注解方式通常更贴近 Java 代码、重构更方便，但也更依赖约定、组件扫描范围和开发者对注解语义的理解。

## 基本链路

### 开始

```java
final AnnotationConfigApplicationContext annotationContext = new AnnotationConfigApplicationContext(AnnotationConfig.class);
```
- 这里不是“加载配置文件”，而是把 `AnnotationConfig.class` 注册为配置类，并在构造 `AnnotationConfigApplicationContext` 时触发容器刷新。
- Spring 解析 `@Configuration`、`@ComponentScan`、`@Bean` 等元数据，生成或注册 BeanDefinition，再按 BeanDefinition 创建对象。
- 它与 `ClassPathXmlApplicationContext` 的入口不同，但后续仍进入同一套 BeanFactory、依赖注入、生命周期和代理处理流程。

### 注解作用

- `@Configuration` 标记这是一个 Spring Java 配置类。它不是普通的业务组件；Spring 会解析其中的 `@Bean` 方法，并在必要时增强配置类以保持 Bean 方法的容器语义。
- `@ComponentScan("cn.siyes.training.spring.annotation")` 指定组件扫描范围。Spring 扫描符合条件的组件类并注册 BeanDefinition，不是把目录下所有 `.java` 文件都直接生成 Bean。
- `@Bean` 表示通过一个工厂方法显式注册 Bean。它特别适合 `DataSource`、`JdbcTemplate` 等第三方类，也适合需要自行控制构造过程的对象，并不只用于第三方类。
- `@EnableTransactionManagement` 注册注解事务所需的基础设施，使 Spring 能识别 `@Transactional` 并创建事务拦截器；它不会自己创建数据库连接，也不会让所有方法自动开启事务。
- `@EnableAspectJAutoProxy` 注册自动代理创建器；`@Aspect` 类本身还必须是 Spring Bean，`@Around` 方法则同时包含通知逻辑和切点表达式。
- `@Component` 是通用组件标记；`@Service`、`@Repository`、`@Controller` 是带有分层语义的派生构造型注解。它们都能参与组件扫描，但 `@Controller` 还会被 Spring MVC 识别为 Web 控制器，不能简单理解成完全相同的别名。
- `@PostConstruct`（不是 `@PostConstructor`）在依赖注入完成后、初始化后置处理阶段中执行，和 XML 的 `init-method` 处于相近生命周期位置。
- `@PreDestroy` 在容器销毁 singleton Bean 前执行，和 XML 的 `destroy-method` 处于相近生命周期位置；prototype Bean 的销毁回调默认不由容器自动负责。
- 后置处理器可以通过 `@Component` 扫描或 `@Bean` 注册，但注册方式改变的是“Spring 如何发现处理器”，不改变 `BeanFactoryPostProcessor`、`BeanPostProcessor` 等接口的处理对象和时机。

### 使用

```java
  final AccountService accountService = annotationContext.getBean(AccountService.class);
```

- `getBean` 返回的是容器当前对外提供的对象；如果 Bean 命中了 AOP 或事务切点，返回值可能是代理对象，而不是原始实现类。
- `AccountService` 的构造器参数由 Spring 根据 Bean 类型解析并注入；这不是 `getBean` 的特殊语法，而是 Bean 创建阶段的依赖注入结果。
- 如果使用 CGLIB 类代理，按 `AccountService.class` 获取通常可以工作；如果改用 JDK 动态代理，应优先按业务接口获取。因此不应把“返回值一定是原始实现类”作为设计前提。
- 注解方式通常让代码更集中、更适合重构，但代价是需要理解组件扫描范围、Bean 名称、条件注册、配置类和各种基础设施注解的语义。

## 注意的问题

- 我的感觉就是使用时需要对注解和pom包需要有一定了解。
- 还有就是自调用代理的边界问题。@Transactional、@Aspect 等基于 Spring 代理的能力，只能拦截“从代理对象进入”的方法调用。目标对象内部使用 this 调用时，不会再次经过代理。

## 关键机制补充

### 1. 注解方式没有替换 Spring 容器

两轮的差异可以这样对照：

```text
XML：
XML 解析器 -> BeanDefinition

注解 / Java 配置：
配置类解析器、组件扫描器、@Bean 方法 -> BeanDefinition

两者之后共同进入：
BeanFactory -> 依赖注入 -> 生命周期 -> 后置处理器 -> AOP / 事务代理 -> getBean
```

所以 `@Service` 并不是直接执行 `new AccountService(...)` 的语法糖。它先提供组件元数据，Spring 再根据 BeanDefinition 决定如何创建、注入和增强对象。

### 2. BeanDefinition、目标对象和代理对象是三个层次

```text
BeanDefinition
  -> 描述“如何创建 AccountService”

目标对象
  -> 真正执行 AccountService 方法的实例

代理对象
  -> 对外拦截方法调用，再决定是否进入目标对象
```

在转账练习中，外部调用的实际链路不是简单的：

```text
accountService.transfer()
```

而是：

```text
AccountService 代理
  -> 事务拦截器：开启事务
  -> AOP 切面：记录开始时间
  -> AccountService 目标对象.transfer()
  -> AOP 切面：记录耗时
  -> 事务拦截器：commit 或 rollback
```

`getBean(AccountService.class)` 能否赋给具体实现类，取决于代理类型。当前配置使用 `proxyTargetClass = true`，通常得到 CGLIB 类代理；如果使用 JDK 动态代理，调用方应面向接口获取，不应依赖具体实现类。

### 3. `@ComponentScan` 与 `@Bean` 解决的是不同来源

```text
@ComponentScan
  -> 扫描业务代码中的 @Component / @Service / @Repository

@Bean
  -> 通过配置方法显式创建并注册对象
```

组件扫描适合项目自己编写、可以添加注解的类；`@Bean` 适合第三方类、需要自定义构造参数的对象，或需要清楚表达注册过程的基础设施。

`@Bean` 方法的参数也由 Spring 注入：

```java
@Bean
public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
}
```

这里的 `dataSource` 不是方法调用者手动传入的参数，而是容器根据类型找到的 DataSource Bean。配置类使用 `@Configuration` 时，Spring 还会保证配置类中的 Bean 方法遵守容器的单例语义；不要把 `@Bean` 方法简单当作普通工厂方法理解。

### 4. AOP 和事务为什么可以同时生效

`@Aspect` 和 `@Transactional` 不是把业务代码复制一份，而是向容器注册不同的拦截规则。自动代理创建器发现同一个目标 Bean 同时匹配多个规则后，可以把多个拦截器组装到一个代理调用链中：

```text
外部调用代理.transfer()
  -> 事务拦截器
  -> TimingAspect around 前置逻辑
  -> 目标方法
  -> TimingAspect around finally
  -> 事务拦截器根据结果 commit / rollback
```

具体拦截器先后顺序可能受 `Ordered`、`@Order` 和基础设施配置影响，不能只凭一次日志把顺序当作永远固定的业务约定。当前练习重点是理解它们都依赖代理，并且异常必须穿过事务拦截器才能触发回滚。

### 5. `JdbcTemplate` 如何参与 Spring 事务

Repository 没有手动调用 `commit()`，并不代表 SQL 没有事务。事务代理先通过 `DataSourceTransactionManager` 建立事务，并将当前 Connection 绑定到当前线程；同一事务中的 `JdbcTemplate` 会从 DataSource 获取这条事务绑定的 Connection：

```text
@Transactional 方法进入
  -> DataSourceTransactionManager 获取并绑定 Connection
  -> JdbcTemplate 执行 debit
  -> JdbcTemplate 执行 credit
  -> JdbcTemplate 执行 insert
  -> 正常返回：事务管理器 commit
  -> 抛出匹配异常：事务管理器 rollback
```

因此事务边界应放在组合多个 Repository 操作的 Service 层。Repository 负责数据访问，不应知道 HTTP 状态码，也不应自行决定整个业务事务的提交时机。

本练习中的 `rollbackFor = Exception.class` 是有意配置的，因为 `TransferException` 是受检异常。Spring 默认主要对 `RuntimeException` 和 `Error` 回滚；如果没有 `rollbackFor`，受检异常不一定触发回滚。

### 6. 后置处理器与 AOP 代理的关系

你在后置处理器练习中手写的 Wrapper 与 Spring AOP 代理虽然不是同一个实现，但观察到了同一类容器扩展动作：

```text
BeanPostProcessor.afterInitialization
  -> 返回原对象：容器继续使用原对象
  -> 返回 Wrapper / Proxy：容器后续对外提供新返回对象
```

Spring 的自动代理创建器本身也是后置处理器体系中的基础设施。它会根据切点判断是否需要返回代理。因此，`BeanPostProcessor` 练习不是与 AOP 无关的孤立知识，而是在观察 AOP 代理能够进入容器的一个底层入口。

但两者的执行频率不同：

```text
BeanPostProcessor：Bean 创建时执行
AOP MethodInterceptor：代理方法每次被调用时执行
```

这也是为什么 Probe 的包装日志只出现一次，而 `TimingAspect` 会在每次 `transfer()` 或 `print()` 调用时出现。

### 7. 注解方式的隐式约定和边界

注解让配置更短，但把一部分信息从显式 XML 变成了约定，排查问题时需要主动检查：

- 组件是否在 `@ComponentScan` 的包范围内。
- Bean 名称是否与按名称查找、后置处理器判断条件一致。
- 配置类是否真的使用 `@Configuration`，而不是相似但用途不同的注解。
- 第三方类是否通过 `@Bean` 注册。
- AOP 切点是否匹配实际包名、方法可见性和代理类型。
- `@Transactional` 是否位于从代理进入的 public 方法上。
- 依赖是否加入正确 Module 的 POM，作用域是否允许运行时看到它。

这解释了为什么注解方式“代码更少”不等于“机制更简单”：它减少了重复配置，却要求开发者理解扫描、命名、代理和基础设施注册的边界。

### 8. 本轮复盘的客观边界

目前已经通过手写和运行证明：

- 能使用 `AnnotationConfigApplicationContext` 启动容器。
- 能用组件扫描、构造器注入和 `@Bean` 完成对象组装。
- 能观察注解生命周期回调。
- 能配置并解释 AOP、声明式事务和事务回滚。
- 能区分 BeanDefinition 层处理器与 Bean 实例层处理器。
- 能通过日志验证代理成功和自调用绕过代理的边界。

当前仍不应推断已经掌握：

- Spring Boot 自动配置和 Starter 机制。
- Spring MVC 请求映射、参数绑定和统一异常处理。
- 连接池、并发事务和生产级数据访问优化。
- 复杂切面排序、循环依赖和高级代理创建细节。

这些内容留到后续 Module 学习，不影响本轮对 Spring Core 基本设计和动手能力的判断。
