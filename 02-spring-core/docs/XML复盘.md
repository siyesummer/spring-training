# XML复盘

1. XML 中的 `<bean>` 描述的是由 Spring 容器创建和管理的对象。`new ClassPathXmlApplicationContext(...)` 本身是业务代码创建 Spring 容器；容器创建完成后，再根据 XML 中的 Bean 定义创建 Repository、Service 等对象。

   更完整的链路是：

   ```text
   业务代码 new ApplicationContext
       -> Spring 读取 XML
       -> XML 被解析为 BeanDefinition 元数据
       -> BeanFactory 根据 BeanDefinition 创建对象
       -> 完成依赖注入和生命周期回调
       -> 必要时创建 AOP 代理
   ```

2. `getBean` 返回的是容器中注册的对象。普通 Bean 没有匹配 AOP 切点时，通常返回目标对象；匹配 AOP 配置时，返回代理对象。当前 `accountService` 匹配了切点，所以获取到的类型类似：

   ```text
   AccountService$$SpringCGLIB$$0
   ```

   这说明拿到的是 CGLIB 代理对象，而不是原始 `AccountService` 对象。直接 `new AccountService(...)` 不会经过 Spring 的代理处理。

3. XML 配置节点和 Spring 行为的对应关系：

   ```text
   <constructor-arg>  -> 构造器注入
   <property>         -> Setter 注入
   transactionManager -> 事务管理器 Bean，负责管理数据库连接、提交和回滚
   <tx:method>        -> 描述方法的事务传播、隔离级别和回滚规则
   <aop:pointcut>     -> 定义哪些方法匹配切点
   <aop:advisor>      -> 把事务或耗时通知绑定到切点
   <aop:config>       -> 配置 Spring AOP 代理关系
   ```

   事务的实际调用关系是：

   ```text
   transfer()
       -> pointcut 匹配
       -> advisor 绑定 transactionAdvice
       -> TransactionInterceptor 开启事务
       -> transactionManager 管理数据库连接
       -> 正常返回 commit，异常返回 rollback
   ```

4. 注释掉 `accountService` Bean 定义后，执行：

   ```java
   context.getBean("accountService");
   ```

   会出现：

   ```text
   NoSuchBeanDefinitionException
   No bean named 'accountService' available
   ```

   更准确地说，这个异常通常发生在调用 `getBean` 查找 Bean 时，不一定是在 `ApplicationContext` 刷新阶段立刻发生。因为 XML 中已经没有名为 `accountService` 的 Bean 定义，容器无法根据名称返回对象。

5. Spring AOP 和事务代理依赖“通过代理对象进入方法”。如果业务类内部通过 `this` 调用自己的另一个方法，不会再次经过 Spring 代理：

   ```java
   public void transfer() {
       this.writeLog();
   }

   public void writeLog() {
   }
   ```

   调用链实际是：

   ```text
   外部调用代理对象.transfer()
       -> 事务拦截器
       -> 目标对象.transfer()
       -> this.writeLog()
   ```

   `this.writeLog()` 是目标对象内部的直接调用，不会再次经过代理。因此：

   - 如果 `transfer()` 已经开启事务，`writeLog()` 会运行在已有事务中，但不会重新应用它自己的事务规则。
   - 如果只有 `writeLog()` 配置了事务，而外部调用的是 `transfer()`，`writeLog()` 的事务通知不会因为内部调用自动触发。
   - 如果外部通过 Spring 容器获取的代理对象直接调用 `writeLog()`，才会经过代理。

   需要让内部方法也独立经过代理时，通常将它拆分到另一个 Spring Bean 中，由当前 Service 注入并调用，例如：

   ```text
   TransferService -> AuditService
   ```

   这样 `TransferService` 调用 `AuditService` 时，会重新经过 Spring 代理。

6. 本轮使用 `JdbcTemplate` 执行 SQL，Repository 不直接调用 `commit()` 或 `rollback()`。事务边界由 Service 方法和 XML `<tx:advice>` 描述，`DataSourceTransactionManager` 负责实际提交或回滚。

   正常转账时，扣款、入账和日志写入全部提交；在日志写入后抛出异常时，三步修改全部回滚。这说明多个 Repository 使用了 Spring 事务管理器绑定的同一条数据库连接。

   ```xml
   <tx:method name="transfer"
              propagation="REQUIRED"
              isolation="READ_COMMITTED"
              rollback-for="java.lang.Exception"/>
   ```

## 7. Spring 后置处理器体系

### 7.1 为什么需要后置处理器

Spring 不可能预先知道所有项目和框架需要怎样修改 Bean，因此在容器启动和 Bean 创建过程中预留了扩展点。框架代码可以在不修改 Spring 容器源码、也不修改业务类核心逻辑的情况下：

- 动态增加 BeanDefinition。
- 修改 XML 或注解解析出的 BeanDefinition。
- 在 Bean 初始化前后调整实例。
- 将原始对象包装成代理对象。

“PostProcessor”不能统一翻译成“Bean 创建后的处理器”。这里的 `post` 是相对于某个特定阶段而言：

```text
BeanDefinitionRegistryPostProcessor
  -> BeanDefinition 注册后的处理

BeanFactoryPostProcessor
  -> BeanFactory 已装载 BeanDefinition 后的处理

BeanPostProcessor
  -> 单个 Bean 实例化和属性注入后的处理
```

它们与 Servlet Filter 不同。Filter 在每次 HTTP 请求时执行；这些处理器主要在 Spring 容器启动和 Bean 创建阶段执行。

### 7.2 整体执行顺序

忽略循环依赖等高级场景，一个普通 singleton 的主线可以简化为：

```text
读取 XML
  -> BeanDefinitionReader 解析并注册 BeanDefinition
  -> BeanDefinitionRegistryPostProcessor
       -> 可以新增或修改 BeanDefinition
  -> BeanFactoryPostProcessor
       -> 修改已经注册的 BeanDefinition
  -> 注册 BeanPostProcessor
  -> 实例化普通 Bean（调用构造器）
  -> 属性填充和依赖注入
  -> Aware 等容器回调
  -> BeanPostProcessor.beforeInitialization
  -> InitializingBean / init-method
  -> BeanPostProcessor.afterInitialization
  -> 容器保存并对外提供最终 singleton 对象
```

前三个处理阶段要分成两层理解：

```text
BeanDefinition 层：描述将来怎么创建对象
Bean 实例层：已经存在的 Java 对象
```

`BeanDefinitionRegistryPostProcessor` 和 `BeanFactoryPostProcessor` 工作在定义层；`BeanPostProcessor` 工作在实例层。

### 7.3 BeanDefinitionRegistryPostProcessor

`BeanDefinitionRegistryPostProcessor` 继承自 `BeanFactoryPostProcessor`，所以实现类有两个回调：

```java
void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)

void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
```

第一个方法先执行，重点是操作 BeanDefinition 注册表。常见能力包括：

- `registerBeanDefinition`：动态注册新定义。
- `containsBeanDefinition`：判断名称是否已经存在。
- `getBeanDefinition`：取得并修改已有定义。
- `removeBeanDefinition`：删除定义，实际使用时必须谨慎。

本轮通过 `RootBeanDefinition` 注册 `dynamicProbe`：

```java
RootBeanDefinition definition =
        new RootBeanDefinition(PostProcessorProbe.class);

definition.getPropertyValues().addPropertyValue(
        "label", "from-registry-post-processor");

registry.registerBeanDefinition("dynamicProbe", definition);
```

这段代码只注册了元数据，没有在注册阶段创建 `PostProcessorProbe` 对象。后续仍然由 Spring 完成构造器调用、Setter 注入、初始化和 singleton 管理。

使用场景通常是框架基础设施或插件机制，例如根据配置动态注册一批 Bean。普通业务对象一般直接使用 XML、组件扫描或 `@Bean`，不需要专门编写 RegistryPostProcessor。

### 7.4 BeanFactoryPostProcessor

`BeanFactoryPostProcessor` 在 BeanDefinition 已经装入 BeanFactory、普通业务 Bean 还没有实例化时执行：

```java
void postProcessBeanFactory(
        ConfigurableListableBeanFactory beanFactory)
```

它可以取得 BeanDefinition 并修改定义中的信息：

```java
BeanDefinition definition =
        beanFactory.getBeanDefinition("postProcessorProbe");

definition.getPropertyValues().addPropertyValue(
        "label", "from-bean-factory-post-processor");
```

Spring 随后实例化 Bean 时，Setter 收到的是修改后的值。因此它修改的不是现有对象字段，而是“将来创建对象时使用的配置”。

`<context:property-placeholder/>` 背后使用的占位符处理器就是这一类扩展思路：在业务 Bean 创建前，把 `${DB_URL}` 等占位符解析为环境变量中的真实值。

不要在普通 BeanFactoryPostProcessor 中随意调用 `getBean()`。这样可能导致业务 Bean 被提前实例化，而此时 BeanPostProcessor 还没有全部注册，Bean 可能错过代理或其他生命周期处理。

### 7.5 BeanPostProcessor

`BeanPostProcessor` 针对的是已经实例化并完成属性注入的 Bean：

```java
Object postProcessBeforeInitialization(Object bean, String beanName)

Object postProcessAfterInitialization(Object bean, String beanName)
```

两个方法的区别：

```text
beforeInitialization
  -> 属性注入已经完成
  -> init-method 还没有执行

afterInitialization
  -> init-method 已经执行
  -> 即将确定容器后续使用的最终对象
```

每个符合条件的 Bean 在创建过程中都会经过已注册的 BeanPostProcessor。处理器通常先根据 `beanName`、类型或注解判断是否需要处理：

```java
if (!"postProcessorProbe".equals(beanName)) {
    return bean;
}
```

不处理的 Bean 必须原样返回。处理目标 Bean 时有两种常见做法：

1. 修改原对象并返回原对象。
2. 返回包装对象或代理对象。

本轮的 `ProbeViewWrapper` 属于第二种：

```java
return new ProbeViewWrapper((ProbeView) bean);
```

原始 `PostProcessorProbe` 仍然存在，并被 Wrapper 作为 delegate 持有；但 Spring 后续对外提供的是 Wrapper。因此按 `ProbeView` 接口获取成功，强制转换为 `PostProcessorProbe` 会出现 `ClassCastException`。

### 7.6 与 singletonObjects 的关系

后置处理器不应该直接访问或修改 `singletonObjects`。它是 Spring 容器内部用于管理 singleton 的缓存，不是公开的业务扩展 API。

对普通、没有循环依赖的 singleton，可以这样理解：

```text
创建原始对象
  -> 属性注入
  -> BeanPostProcessor 前置处理
  -> 初始化
  -> BeanPostProcessor 后置处理
  -> 得到最终返回对象
  -> 容器缓存最终结果
  -> 后续 getBean 返回缓存结果
```

所以 `BeanPostProcessor` 是“通过返回值间接影响最终缓存对象”，不是进入 Map 手动替换对象。循环依赖涉及 `earlySingletonObjects` 和 `singletonFactories`，属于更深入的容器实现，本阶段不需要展开。

singleton 和 prototype 都会在创建时经过 BeanPostProcessor，但有区别：

- singleton 通常只创建并处理一次，之后重复 `getBean` 返回缓存对象。
- prototype 每次 `getBean` 都重新创建，所以每次都会重新经过创建和后置处理流程。
- prototype 的完整销毁阶段默认不由容器负责。

### 7.7 与 Spring AOP 的关系

Spring AOP 的自动代理创建建立在 BeanPostProcessor 体系之上。可以简化理解为：

```text
目标 Bean 创建并初始化
  -> 自动代理创建器判断是否匹配 Advisor / Pointcut
  -> 不匹配：返回原始 Bean
  -> 匹配：返回 JDK 或 CGLIB 代理
  -> getBean 最终取得代理对象
```

自动代理创建器属于更专门的 BeanPostProcessor 实现。我们手写 `ProbeViewWrapper` 不是重新实现完整 AOP，而是用最小代码观察“afterInitialization 可以返回另一个对象”这一核心机制。

还要区分执行频率：

```text
BeanPostProcessor：主要在 Bean 创建时执行
AOP MethodInterceptor：代理对象的匹配方法每次被调用时执行
```

因此 `ProbeBeanPostProcessor` 不会在每次 `print()` 时重新执行；真正每次执行的是 Wrapper 的 `print()` 或代理中的 MethodInterceptor。

### 7.8 三种处理器的范围对比

| 扩展点 | 处理对象 | 主要时机 | 调用范围 | 能否替换最终 Bean |
| --- | --- | --- | --- | --- |
| `BeanDefinitionRegistryPostProcessor` | BeanDefinition 注册表 | 普通 Bean 实例化前 | 通常每个处理器每个容器调用一次 | 不能直接替换实例，但能改变创建定义 |
| `BeanFactoryPostProcessor` | 已注册 BeanDefinition | 普通 Bean 实例化前 | 通常每个处理器每个容器调用一次 | 不能直接替换实例，但能改变创建配置 |
| `BeanPostProcessor` | Bean 实例 | 属性注入后、初始化前后 | 每个符合条件的 Bean 创建时调用 | 可以通过返回包装或代理对象替换对外结果 |

多个处理器同时存在时，可以通过 `PriorityOrdered` 或 `Ordered` 控制顺序。没有指定顺序时，不应让业务正确性依赖处理器恰好按某个偶然顺序执行。

### 7.9 本轮实例如何串起来

本轮两个 Probe 的差异来自处理条件：

```text
postProcessorProbe
  -> XML 注册 BeanDefinition
  -> BeanFactoryPostProcessor 修改 label
  -> BeanPostProcessor before 修改实例
  -> init-method
  -> BeanPostProcessor after 返回 ProbeViewWrapper
  -> getBean 返回 Wrapper

dynamicProbe
  -> RegistryPostProcessor 动态注册 BeanDefinition
  -> Spring 根据定义创建对象并注入 label
  -> ProbeBeanPostProcessor 因 Bean 名称不匹配而原样返回
  -> getBean 返回 PostProcessorProbe
```

这说明“动态注册”和“是否被包装”是两个独立问题。动态 Bean 同样会经过 BeanPostProcessor，只是当前处理器主动跳过了它。

### 7.10 常见误区和使用边界

- `BeanFactoryPostProcessor` 不是在 BeanDefinition 生成前执行，而是在定义已经注册后执行。
- `BeanPostProcessor` 不是每次业务方法调用的拦截器，它处理的是 Bean 创建过程。
- 包装对象与原始类没有继承关系时，调用方应依赖共同接口，不能强制转换为原始实现类。
- 不要在处理器中无条件强转所有 Bean；Spring 自己也有大量基础设施 Bean。
- 不要在 BeanFactoryPostProcessor 中随意提前调用 `getBean()`。
- 不要直接操作 `singletonObjects` 等内部缓存。
- 普通业务属性修改优先放在业务代码或配置中；横切方法行为优先使用 AOP。后置处理器更适合框架级、容器级扩展。
- 后置处理器本身是容器基础设施，可能不会像普通业务 Bean 一样经过所有其他后置处理器。

控制台出现 `not eligible for getting processed by all BeanPostProcessors` 一类警告，通常表示某些基础设施 Bean 在全部 BeanPostProcessor 注册完成前被提前创建。它不是所有场景都可以忽略；应检查是否在处理器初始化阶段提前调用了普通 Bean。本次涉及的是 Spring AOP 基础设施对象，业务 Probe 创建和包装结果正常，但仍应理解这条警告表达的生命周期边界。
