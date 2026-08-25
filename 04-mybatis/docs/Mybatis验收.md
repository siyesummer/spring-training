# MyBatis 验收与复盘

## 一、客观评价

当前已经形成了正确的 MyBatis 主线认识：Mapper 接口由动态代理实现，Mapper 方法通过 `namespace + statement id` 定位 SQL，`#{}` 与 `${}` 的处理方式不同，原生 MyBatis 与 Spring 集成后的 `SqlSession` 管理方式也不同。

结合本阶段的手写代码和实际验收，目前已经具备以下能力：

- 能使用原生 MyBatis 完成 XML CRUD、动态 SQL、分页、批量操作和手动事务。
- 能理解 `SqlSessionFactory -> SqlSession -> Mapper 代理 -> SQL -> JDBC` 的基本执行链。
- 能将 MyBatis 接入 Spring，通过 Mapper 代理替换 `JdbcTemplate` Repository。
- 能使用 `@Transactional` 让多个 Mapper 操作参与同一事务，并通过数据库结果验证回滚。
- 能完成一对多 JOIN 查询并使用 `<resultMap>`、`<id>` 和 `<collection>` 组装结果。

需要补强的不是 API 数量，而是几个框架内部边界：

- `MapperFactoryBean` 与 `SqlSessionTemplate` 各自处在代理创建和 SQL 执行链的哪个位置。
- 一对多 JOIN 结果如何折叠成对象，以及为什么不能直接按 JOIN 结果行分页。
- Spring 事务中真正被绑定和复用的连接、`SqlSession` 与 Mapper 代理之间的关系。
- 默认回滚规则、异常被捕获后为什么可能导致事务提交。

整体判断：已经达到 MyBatis 基础使用和基本机制理解的阶段要求，可以继续通过本复盘巩固概念，不需要为了形式继续堆积接口或测试命令。

## 二、Mapper 接口没有实现类，为什么可以注入和调用？

`@MapperScan` 会扫描指定包中的 Mapper 接口，并为每个接口向 Spring 容器注册一个以 `MapperFactoryBean` 为基础的 `BeanDefinition`。

Spring 创建这个 Bean 时，`MapperFactoryBean` 会通过 MyBatis 的 `SqlSession.getMapper()` 获得 Mapper 接口的动态代理对象。该代理通常是 MyBatis 基于 JDK 动态代理生成的，不是开发者手写的实现类，也不是 Spring AOP 为业务类生成的代理。

最终注入到 `TaskService` 中的是实现了 `TaskMapper` 接口的代理对象：

```text
@MapperScan
  -> 注册 MapperFactoryBean 的 BeanDefinition
  -> MapperFactoryBean 创建 Mapper 动态代理
  -> Mapper 代理作为 Spring Bean 注入 Service
```

调用 `taskMapper.findById(id)` 时，代理不会执行普通 Java 方法体，而是把接口名和方法名转换成 MyBatis statement 的唯一标识，再交给 `SqlSession` 执行。

## 三、`@MapperScan`、`MapperFactoryBean`、`SqlSessionTemplate` 分别做什么？

### `@MapperScan`

负责发现指定包中的 Mapper 接口，并为这些接口注册 Bean 定义。它解决的是“哪些接口需要成为 Mapper Bean”的问题。

### `MapperFactoryBean`

它是 Spring 的 `FactoryBean`，负责为一个 Mapper 接口创建 MyBatis 动态代理。它解决的是“接口没有实现类，Spring 应该注入什么对象”的问题。

需要区分：容器管理的是 `MapperFactoryBean`，业务代码通过它获得的是 Mapper 代理对象。

### `SqlSessionTemplate`

`SqlSessionTemplate` 是 `mybatis-spring` 提供的、线程安全的 `SqlSession` 实现，是 Mapper 代理进入 MyBatis 执行 SQL 的统一入口。它主要负责：

- 根据当前 Spring 事务获取或创建实际 `SqlSession`。
- 在同一 Spring 事务中复用与当前线程绑定的 `SqlSession` 和 JDBC `Connection`。
- 在非事务调用结束后自动释放实际 `SqlSession`。
- 将 MyBatis 持久化异常转换到 Spring 数据访问异常体系。

业务代码不应该对 `SqlSessionTemplate` 手动调用 `commit()`、`rollback()` 或 `close()`。事务提交和回滚由 Spring 事务管理器负责。

需要注意：Mapper Bean 和 `SqlSessionTemplate` 通常是单例，但它们不会永久持有一个真实数据库会话。真正的 `SqlSession` 按操作或事务取得；同一事务中复用，事务结束后释放。

## 四、Mapper 方法怎样定位到 XML 中的 SQL？

MyBatis 中一条 statement 的完整标识是：

```text
namespace + "." + statement id
```

例如：

```xml
<mapper namespace="cn.siyes.training.mybatis.mapper.TaskMapper">
    <select id="findById">...</select>
</mapper>
```

对应：

```java
public interface TaskMapper {
  Task findById(Long id);
}
```

代理调用时定位的完整名称是：

```text
cn.siyes.training.mybatis.mapper.TaskMapper.findById
```

通常要求：

- `namespace` 等于 Mapper 接口的全限定类名。
- SQL 标签的 `id` 等于接口方法名。
- 参数名称、参数类型和返回结果映射与方法签名相容。

XML 在启动时会被解析为 MyBatis 内部的 `MappedStatement`，运行时不是每次重新读取 XML。

## 五、`#{}` 为什么通常能防止 SQL 注入，`${}` 为什么危险？

`#{}` 会生成 JDBC `PreparedStatement` 的 `?` 占位符，参数值随后通过类型处理器安全绑定：

```sql
WHERE title = ?
```

用户输入会作为“值”传入，而不是作为 SQL 结构的一部分，因此通常能够避免值参数导致的 SQL 注入。

`${}` 是原始文本替换，输入内容会直接拼进 SQL：

```xml
ORDER BY ${sortBy}
```

如果把任意用户输入直接交给 `${}`，攻击者可以改变 SQL 结构。表名、列名和 `ASC`/`DESC` 这类结构不能使用 `?` 占位符时，应先在 Java 或 `<choose>` 中做白名单映射，不能直接信任请求参数。

## 六、`<where>`、`<set>`、`<foreach>`、`<choose>` 分别解决什么问题？

### `<where>`

当内部至少有一个条件成立时自动添加 `WHERE`，并去除开头多余的 `AND` 或 `OR`。如果没有条件成立，则不会生成 `WHERE`。

### `<set>`

当更新字段动态变化时自动添加 `SET`，并去除末尾多余的逗号。需要保证至少有一个字段参与更新，否则最终 SQL 仍可能无效。

### `<foreach>`

用于遍历集合或数组，常见用途是：

- 生成 `IN (?, ?, ?)`。
- 生成批量插入的多组 `VALUES`。

常用属性包括 `collection`、`item`、`open`、`separator` 和 `close`。

### `<choose>`

类似 Java 的 `if / else if / else`。MyBatis 按顺序判断 `<when>`，只选择第一个成立的分支；都不成立时进入 `<otherwise>`。它适合实现排序字段白名单等互斥分支。

## 七、一对多 JOIN 结果为什么需要 `<id>` 和 `<collection>`？

任务与评论 JOIN 后，一个任务会在 JDBC `ResultSet` 中出现多行：

```text
任务1 + 评论1
任务1 + 评论2
任务1 + 评论3
```

Java 期望的结果却是：

```text
一个 TaskDetail
  -> List<TaskComment>
```

外层 `<id property="id" column="task_id"/>` 告诉 MyBatis 哪些结果行属于同一个父对象。MyBatis 根据这个标识复用同一个 `TaskDetail`，而不是每行都创建一个新任务。

`<collection property="comments" ofType="TaskComment">` 告诉 MyBatis 把每一行中的评论列组装成 `TaskComment`，再累积到父对象的 `comments` 集合中。

集合内部的 `<id property="id" column="comment_id"/>` 用于标识评论对象，帮助 MyBatis 正确识别和去重子对象。在复杂 JOIN 中明确配置父、子对象的 `<id>` 也能提高结果映射效率和稳定性。

因此可以把它理解为：

```text
父级 <id>        -> 哪些行属于同一个任务
<collection>     -> 哪些列组成评论集合
集合内部的 <id>  -> 哪些行属于同一条评论
```

## 八、为什么不能直接对一对多 JOIN 结果行做任务分页？

数据库的 `LIMIT` 作用于 JOIN 后的“结果行”，而不是 MyBatis 折叠后的“任务对象”。

假设任务 1 有三条评论，执行：

```sql
LIMIT 0, 2
```

数据库可能只返回：

```text
任务1 + 评论1
任务1 + 评论2
```

MyBatis 折叠后这一页只有一个任务，而且任务 1 的第三条评论被截断。这样会出现：

- 每页实际任务数量不稳定。
- 一个任务的评论集合不完整。
- 总数和分页边界难以正确计算。

常见处理方式是：

1. 分页列表只查询任务，不加载一对多集合。
2. 先分页查询任务 ID，再根据这些 ID 查询任务和评论。
3. 使用子查询先完成父表分页，再 JOIN 子表，但要注意 SQL 复杂度。

本项目采用“任务分页不加载评论，单个详情使用 JOIN + `<collection>`”是清晰且合理的边界。

## 九、原生 MyBatis 和 Spring 集成后的 `SqlSession` 生命周期有什么区别？

### 原生 MyBatis

开发者负责：

- 通过 `SqlSessionFactory.openSession()` 创建会话。
- 决定何时 `commit()` 或 `rollback()`。
- 使用完成后调用 `close()`。

`SqlSession` 不是线程安全对象，通常表示一次工作单元，不能作为全局单例在多个线程间共享。

### Spring + MyBatis

业务代码注入 Mapper 代理，不再直接创建和关闭 `SqlSession`。Mapper 代理通过线程安全的 `SqlSessionTemplate` 获取实际会话。

- 有 Spring 事务时，同一事务中的 Mapper 调用复用事务关联的 `SqlSession` 和 JDBC `Connection`。
- 没有 Spring 事务时，实际会话通常按一次 Mapper 调用取得并在调用后释放，不应依赖多次调用共享同一个一级缓存。
- 提交、回滚和资源释放由 Spring 与 `mybatis-spring` 协作完成。

## 十、两个 Mapper 为什么能参与同一个 Spring 事务？

关键不只是“它们用了同一个 Mapper 或同一个固定 `SqlSession`”，而是以下配置必须连接到同一个数据源：

```text
DataSourceTransactionManager
          +
SqlSessionFactory
          |
      同一个 DataSource
```

进入 `@Transactional` 方法时，`DataSourceTransactionManager` 从 `DataSource` 获取 JDBC `Connection`，并将其绑定到当前执行线程。两个 Mapper 代理都通过 `SqlSessionTemplate` 参加当前事务，最终使用同一个事务关联的连接。

因此任务 Mapper 和评论 Mapper 的 SQL 能够一起提交或一起回滚：

```text
@Transactional Service 方法
  -> TaskMapper.insert(...)
  -> TaskCommentMapper.insertBatch(...)
  -> 方法正常结束：commit
  -> 抛出符合回滚规则的异常：rollback
```

## 十一、哪些异常默认触发 `@Transactional` 回滚？捕获并吞掉异常有什么后果？

Spring 默认对以下异常回滚：

- `RuntimeException` 及其子类。
- `Error` 及其子类。

受检异常（继承 `Exception` 但不继承 `RuntimeException`）默认不触发回滚。如需回滚，可以配置：

```java
@Transactional(rollbackFor = Exception.class)
```

如果在事务方法内部捕获异常但不再抛出，事务拦截器看到的是“方法正常返回”，通常会提交此前已经执行的修改。影响不只是全局异常处理器捕获不到，更重要的是事务可能失去原本应有的原子性。

可以选择：

- 不捕获，让运行时异常继续抛出。
- 捕获后记录必要信息，再抛出运行时异常并保留原始 `cause`。
- 有明确需求时手动把当前事务标记为仅回滚，但这不是当前阶段的首选写法。

## 十二、从 `JdbcTemplate` Repository 换成 MyBatis Mapper 后，哪些层改变，哪些层应该保持稳定？

主要改变的是数据访问层：

- 手写 Repository 实现被 Mapper 接口和 Mapper XML 替代。
- SQL 从 Java 字符串迁移到 Mapper XML 或 Mapper 注解。
- `RowMapper` 和手动参数设置由 MyBatis 的参数绑定与结果映射替代。
- Service 从调用 Repository 改为调用 Mapper；如果两者事先抽象为同一个数据访问接口，Service 甚至可以基本不变。

应该尽量保持稳定的是：

- Controller 的 URL、HTTP 方法和请求响应契约。
- DTO 的输入校验和统一异常响应。
- Service 表达的业务规则和事务边界。
- 数据库表所表达的领域关系。

MyBatis Mapper 本质上承担了原来 Repository/DAO 的数据访问职责，但“Mapper”强调的是 Java 方法、SQL 和结果映射之间的绑定。

## 十三、Spring Boot 将来会自动完成哪些配置，哪些机制仍属于 MyBatis？

引入 MyBatis Spring Boot Starter 并提供必要配置后，Spring Boot 通常可以自动完成：

- 根据配置属性创建 `DataSource`。
- 创建并配置 `SqlSessionFactory`。
- 创建 `SqlSessionTemplate`。
- 配置合适的事务管理器。
- 加载约定位置或配置位置中的 Mapper XML。

Mapper 接口仍需要通过 `@Mapper`、`@MapperScan` 或 Starter 支持的扫描规则被发现。自动配置减少的是 Java 配置样板，不代表 MyBatis 被 Spring Boot 替代。

以下能力仍然属于 MyBatis，需要开发者理解和设计：

- Mapper 接口与 statement 的定位规则。
- XML SQL 或注解 SQL。
- `#{}` 参数绑定和 `${}` 文本替换。
- `<where>`、`<set>`、`<foreach>`、`<choose>` 等动态 SQL。
- `resultType`、`resultMap`、`association` 和 `collection` 结果映射。
- 多参数命名、类型处理器、生成主键和缓存行为。

Spring Boot 解决的是“如何更快地装配和启动”，MyBatis 解决的是“Java 方法如何执行 SQL 并映射数据库结果”。

## 十四、完整调用链

### 原生 MyBatis

```text
MyBatis 配置文件
  -> SqlSessionFactoryBuilder（解析配置并构建）
  -> SqlSessionFactory（应用级工厂）
  -> SqlSession（一次工作单元）
  -> Mapper 动态代理
  -> MappedStatement
  -> Executor
  -> PreparedStatement
  -> MySQL
```

### Spring + MyBatis

```text
HTTP 请求
  -> DispatcherServlet
  -> Controller
  -> Service（@Transactional 代理开启事务）
  -> Mapper 动态代理
  -> SqlSessionTemplate
  -> 当前事务关联的 SqlSession / Connection
  -> MyBatis Executor 和 MappedStatement
  -> JDBC
  -> MySQL
```

这条链中：

- Spring MVC 负责 HTTP 请求分发和参数、响应处理。
- Spring Core 负责 Bean 创建与依赖注入。
- Spring Transaction 负责事务边界。
- `mybatis-spring` 负责把 Mapper、`SqlSession` 和 Spring 事务连接起来。
- MyBatis 负责 SQL 定位、动态 SQL、参数绑定、执行和结果映射。
- JDBC 驱动最终与 MySQL 通信。

## 十五、其他重要边界

### 一级缓存的作用域

MyBatis 一级缓存默认属于 `SqlSession`。原生模式中，同一个 `SqlSession` 内重复查询可能命中一级缓存；执行更新、提交、回滚或关闭会话会影响或清空缓存。

Spring 集成后，只有处于同一个 Spring 事务并复用同一个实际 `SqlSession` 时，才适合讨论跨 Mapper 调用的一级缓存。没有事务的多次 Mapper 调用不要默认共享一级缓存。

### 多参数必须有稳定名称

Mapper 方法存在多个参数时，应使用 `@Param` 提供 XML 可读的稳定名称：

```java
List<Task> findPage(
    @Param("query") TaskQuery query,
    @Param("offset") int offset,
    @Param("size") int size
);
```

XML 中再通过 `#{query.status}`、`#{offset}` 和 `#{size}` 访问，避免依赖 `arg0`、`param1` 这类隐式名称。

### 写入后获得自增主键

配置 `useGeneratedKeys="true" keyProperty="id"` 后，插入成功产生的数据库主键会回填到传入对象的 `id` 属性中。它不是 `insert()` 返回值：`insert()` 返回的是受影响行数，生成的主键从对象的 `id` 读取。

### 两种一对多方案

本项目最终保留 JOIN + 嵌套结果映射：

```xml
<collection property="comments" ofType="TaskComment">...</collection>
```

另一种 `column + select` 会先查询父对象，再把外层结果的某一列作为参数执行另一条 Mapper SQL：

```xml
<collection
    property="comments"
    column="task_id"
    select="cn.siyes.training.mybatis.mapper.TaskCommentMapper.findByTaskId"/>
```

它查询单个详情时容易理解，但用于任务列表时可能产生“1 次任务查询 + N 次评论查询”，即 N+1 问题。本阶段知道其机制和边界即可，不需要替换已经验证通过的 JOIN 方案。

### `@Transactional` 依赖代理边界

声明式事务通常由 Spring AOP 代理开启。外部通过 Spring Bean 代理调用事务方法时事务才会按注解生效；同一个对象内部使用 `this.xxx()` 调用另一个 `@Transactional` 方法，不会再次经过代理，也不会重新应用被调用方法上的独立事务配置。

当前代码中事务入口应以外部调用的 Service 方法为准，内部方法调用仍会执行普通 Java 方法体，并参与外层已经存在的事务。
