# Module 04：MyBatis 数据访问实战

> 当前状态：`已完成`  
> 开始日期：2026-08-24  
> 完成日期：2026-08-25  
> 项目类型：Maven WAR Module；第一段使用普通 `main()`，第二段部署到 Tomcat 10.1  
> 核心目标：理解 MyBatis 如何把 Mapper 方法、SQL、参数和查询结果连接起来，并将其接入 Spring 事务与已有的 Spring MVC 请求链。

## 1. 为什么拆成两段

本阶段使用同一个 `04-mybatis` Module，但引导拆成两份：

1. [MYBATIS_STANDALONE_GUIDE.md](docs/MYBATIS_STANDALONE_GUIDE.md)：原生 MyBatis。
2. [MYBATIS_SPRING_GUIDE.md](docs/MYBATIS_SPRING_GUIDE.md)：Spring + MyBatis + Spring MVC。
3. [Mybatis验收.md](docs/Mybatis验收.md)：阶段能力评价、核心问题订正和最终复盘。

第一段先隔离 Spring，直接观察：

```text
mybatis-config.xml
  -> SqlSessionFactoryBuilder
  -> SqlSessionFactory
  -> SqlSession
  -> Mapper 动态代理
  -> Mapper XML / SQL
  -> JDBC
  -> MySQL
```

第二段再观察 Spring 替换了哪些手动工作：

```text
Tomcat
  -> DispatcherServlet
  -> Controller
  -> Service（@Transactional）
  -> Spring 管理的 Mapper 代理
  -> SqlSessionTemplate
  -> MyBatis
  -> MySQL
```

这样可以明确区分：

- MyBatis 自身负责 SQL 映射、参数绑定、SQL 执行和结果映射。
- `mybatis-spring` 负责把 `SqlSessionFactory`、Mapper 代理和 Spring 容器连接起来。
- Spring 负责 Bean 管理和声明式事务。
- Spring MVC 负责 HTTP 请求处理，不负责执行 SQL。
- Spring Boot 本阶段仍未使用，也没有参与自动配置。

## 2. 实战主题

延续上一阶段的任务管理领域，但数据库使用独立的 `spring_training_mybatis`，避免影响已经验收完成的 `03-spring-mvc` 数据。

本轮至少使用两张有关联的表：

```text
tasks 1 ---- n task_comments
```

主要能力：

- 任务 CRUD。
- 任务条件查询、排序白名单和分页。
- 任务与评论的一对多查询及结果映射。
- 批量写入评论。
- `#{}` 参数绑定与 `${}` 文本替换边界。
- 原生 MyBatis 手动提交、回滚和资源关闭。
- Spring 管理 Mapper 代理与声明式事务。
- 保留 Spring MVC 请求层，用 MyBatis 替换上一阶段的 `JdbcTemplate` 数据访问实现。

## 3. 两段练习的边界

### 第一段：原生 MyBatis

- 不启动 Tomcat。
- 不使用 Spring、`@MapperScan`、`@Transactional`。
- 不使用 MyBatis-Plus。
- 通过普通 Java `main()` 创建 `SqlSessionFactory`。
- SQL 以 Mapper XML 为主，只使用一个简单注解 SQL 做对照。
- 亲手控制 `SqlSession`、`commit()`、`rollback()` 和 `close()`。

第一段完成后才能进入第二段。

### 第二段：Spring 集成

- 使用现有 WAR + Tomcat 运行方式。
- 保留已掌握的 Spring MVC Controller、DTO、校验和异常处理结构。
- 使用 `mybatis-spring` 将 Mapper 代理注册为 Spring Bean。
- 使用 `DataSourceTransactionManager` 与 `@Transactional` 管理事务。
- 不再由业务代码手动创建、提交或关闭 `SqlSession`。

## 4. 实际目录

```text
04-mybatis/
├─ pom.xml
├─ README.md
├─ docs/
│  ├─ MYBATIS_STANDALONE_GUIDE.md
│  ├─ MYBATIS_SPRING_GUIDE.md
│  └─ Mybatis验收.md
└─ src/
   ├─ main/
   │  ├─ java/cn/siyes/training/mybatis/
   │  │  ├─ standalone/
   │  │  ├─ config/
   │  │  ├─ controller/
   │  │  ├─ dto/
   │  │  ├─ exception/
   │  │  ├─ mapper/
   │  │  ├─ model/
   │  │  └─ service/
   │  ├─ resources/
   │  │  ├─ mybatis-config.xml
   │  │  ├─ db/schema.sql
   │  │  └─ cn/siyes/training/mybatis/mapper/
   │  │     ├─ TaskMapper.xml
   │  │     ├─ TaskCommentMapper.xml
   │  │     └─ TaskDetailMapper.xml
   │  └─ webapp/WEB-INF/web.xml
   └─ test/java/
```

## 5. 阶段完成标准

- 能画出并解释 `SqlSessionFactoryBuilder -> SqlSessionFactory -> SqlSession -> Mapper 代理` 的关系。
- 能说明 Mapper 接口为什么没有实现类也能运行。
- 能说明 `namespace + statement id` 如何定位一条 SQL。
- 能正确使用 `#{}`，并说明 `${}` 为什么可能产生 SQL 注入。
- 能完成 XML CRUD、动态 SQL、分页、排序白名单、批量操作和一对多结果映射。
- 能解释原生 MyBatis 中事务由谁提交和回滚。
- 能解释 Spring 接入后 Mapper、`SqlSession` 和事务分别由谁管理。
- 能通过 `@Transactional` 验证“任务与初始评论”同时提交或同时回滚。
- 能通过 Apifox 和 Navicat 验证 Spring MVC 到 MyBatis 再到 MySQL 的完整链路。
- 能说出从 `JdbcTemplate` Repository 换成 MyBatis Mapper 后，Controller、Service 和数据访问层分别发生了什么变化。

构建和自动化测试仍作为辅助证据。本阶段最重要的是理解 Mapper 代理、SQL 映射、结果映射和事务边界。

## 6. 实际完成内容

### 6.1 第一段：原生 MyBatis

- 使用 `mybatis-config.xml`、`SqlSessionFactoryBuilder`、`SqlSessionFactory` 和 `SqlSession` 完成独立启动。
- 使用 Mapper 动态代理连接接口方法与 XML statement，并用一个注解 SQL 做对照。
- 完成任务 CRUD、条件查询、分页、排序白名单、批量评论和任务评论详情查询。
- 使用 `<where>`、`<set>`、`<foreach>`、`<choose>` 和可复用 SQL 片段实现动态 SQL。
- 使用 JOIN、`<resultMap>` 和 `<collection>` 将一对多结果行组装为任务及评论集合。
- 在同一个 `SqlSession` 中观察一级缓存，区分会话作用域与更新后的缓存失效。
- 亲手执行 `commit()`、`rollback()` 和资源关闭，并通过 Navicat 确认事务结果。

### 6.2 第二段：Spring + MyBatis

- 使用 HikariCP 提供 `DataSource`，通过 `SqlSessionFactoryBean` 装配 MyBatis。
- 使用 `@MapperScan` 将 Mapper 接口注册为 Spring Bean，并由 Service 构造器注入。
- 使用 `SqlSessionTemplate` 参与 Spring 管理的会话和事务，不再由业务代码手动创建或关闭 `SqlSession`。
- 使用 `DataSourceTransactionManager` 与 `@Transactional` 让任务和初始评论同时提交或回滚。
- 保留 Spring MVC 的 Controller、DTO、Bean Validation、全局异常处理和 Interceptor，将数据访问实现替换为 MyBatis Mapper。
- 完成创建、按 ID 查询、条件分页、修改、状态修改、删除、详情查询和批量评论接口。
- 对集合请求完成“集合不能为空”和“集合元素不能为空/长度受限”的两层校验。

## 7. 验收证据

- 原生 MyBatis：实际运行 CRUD、动态筛选、排序白名单、批量评论、一对多 JOIN 映射和一级缓存练习；Navicat 已确认手动提交与回滚结果。
- HTTP 功能：Apifox 已验证创建任务与初始评论、按 ID 查询、条件分页、更新、状态修改、删除、详情查询和批量新增评论。
- 状态与异常：重复删除不存在任务返回 HTTP `404`；请求 DTO 校验通过 `@Valid` 和全局异常处理返回 HTTP `400`。
- 数据结果：清空数据后重新验证，Navicat 中 `tasks` 与 `task_comments` 的新增、修改、删除和关联数据与接口结果一致。
- Spring 事务：在创建任务与评论的事务方法中主动抛出运行时异常后，接口返回错误，Navicat 确认任务和评论均未新增；移除模拟异常后恢复正常提交。
- 构建结果：2026-08-25 执行 `mvn -o -s C:\Users\siyesummer\.m2\settings.xml -pl 04-mybatis -am package -DskipTests`，根项目与 `04-mybatis` 均为 `SUCCESS`，28 个主源码文件和 1 个测试源码文件编译成功，生成 `target/04-mybatis.war`。
- 打包内容：WAR 中已确认包含 Mapper 接口、Mapper XML、MyBatis、`mybatis-spring`、HikariCP 和 MySQL JDBC 驱动。

## 8. 核心机制总结

原生调用链：

```text
mybatis-config.xml
  -> SqlSessionFactoryBuilder
  -> SqlSessionFactory
  -> SqlSession
  -> Mapper 动态代理
  -> MappedStatement / Executor
  -> JDBC
  -> MySQL
```

Spring 集成调用链：

```text
Tomcat
  -> DispatcherServlet
  -> Controller
  -> Service（Spring 事务代理）
  -> Mapper 动态代理
  -> SqlSessionTemplate
  -> 事务关联的 SqlSession / Connection
  -> MyBatis
  -> JDBC / MySQL
```

职责边界：

- MyBatis 负责 statement 定位、动态 SQL、参数绑定、SQL 执行和结果映射。
- `mybatis-spring` 负责把 Mapper 代理、`SqlSession` 与 Spring 容器及事务连接起来。
- Spring 负责 Bean 管理和事务边界；Spring MVC 负责 HTTP 请求与响应。
- Spring Boot 尚未参与本 Module；下一阶段会观察自动配置如何减少上述手动装配，但不会改变 MyBatis 的 SQL 与映射机制。

## 9. 当前边界与下一阶段

本阶段已经证明 MyBatis 基础使用和机制理解，但尚未证明以下生产级或进阶能力：

- 二级缓存、缓存一致性和分布式缓存设计。
- 自定义 `TypeHandler`、插件、拦截器和复杂映射扩展。
- 大数据量分页、索引分析、批处理性能和 SQL 调优。
- 多数据源、复杂事务传播、并发更新和隔离级别设计。
- 完整的数据库集成测试与生产监控。

下一阶段进入 `05-spring-boot`，重点理解 Starter、自动配置、配置绑定、Profile、测试、可执行 JAR 和运行期诊断，并明确哪些配置由 Boot 自动完成、哪些能力仍来自 Spring MVC、事务与 MyBatis。
