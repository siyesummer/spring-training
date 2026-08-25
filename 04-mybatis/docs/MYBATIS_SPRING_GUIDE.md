# 第二段：Spring + MyBatis + Spring MVC 详细引导

> 前置条件：第一段原生 MyBatis 验收完成。
> 本段运行方式：Maven WAR 部署到 Tomcat 10.1。
> 本段验收目标：由 Spring 管理数据源、Mapper 代理、`SqlSession` 和事务，并跑通 Spring MVC -> Service -> MyBatis -> MySQL 完整链路。

## 1. 为什么必须先完成第一段

第二段会把第一段手写的很多代码隐藏到框架内部：

| 第一段手动完成 | 第二段由谁接管 |
| --- | --- |
| 创建 `SqlSessionFactory` | Spring 配置中的 `SqlSessionFactoryBean` |
| `openSession()` | `SqlSessionTemplate` / Mapper 代理 |
| `getMapper()` | `@MapperScan` 注册 Mapper Bean |
| `commit()` / `rollback()` | Spring `PlatformTransactionManager` |
| `close()` | `SqlSessionTemplate` 与事务同步机制 |
| 普通 Java 入口 | Tomcat + `DispatcherServlet` + Controller |

这些动作不是消失了，而是由 `mybatis-spring` 和 Spring 在稳定的生命周期边界中协调执行。

## 2. 本段设计取舍

### 2.1 继续使用同一个 Module

不创建第二个 Module。第一段的 Model、Mapper 接口、Mapper XML 和数据库表继续复用，新增 Spring 配置、Service、Controller 和 Web 层 DTO。

### 2.2 不复制 `JdbcTaskRepository`

上一阶段的数据访问链：

```text
TaskService
  -> TaskRepository 接口
  -> JdbcTaskRepository
  -> JdbcTemplate
```

本阶段为了直接观察 MyBatis，采用主流且更简洁的学习结构：

```text
TaskService
  -> TaskMapper 代理
  -> Mapper XML
```

Mapper 在这里承担上一阶段 Repository/DAO 的数据访问职责。对于领域复杂、需要隔离持久化框架的项目，也可以在 Mapper 外再保留 Repository 适配层；当前训练若这样做只会增加转发代码，反而削弱对 Mapper 的观察。

### 2.3 复用请求层，不重复练已掌握内容

可以参考并手动迁移你在 `03-spring-mvc` 中写过的：

- `web.xml`。
- `WebMvcConfig`。
- Controller、请求/响应 DTO。
- Bean Validation。
- `GlobalExceptionHandler`。
- `RequestTraceInterceptor`。

包名改为：

```text
cn.siyes.training.mybatis
```

不要复制：

- `JdbcTaskRepository`。
- `JdbcTemplate` Bean。
- `MVC_DB_*` 环境变量名称。

本阶段的新知识集中在 Mapper、SQL 映射和 Spring 事务。

## 3. 实施顺序

```text
检查点 A：补齐 Web/Spring/MyBatis-Spring 依赖
  -> 检查点 B：Spring 创建 DataSource 和 SqlSessionFactory
  -> 检查点 C：@MapperScan 生成 Mapper Bean
  -> 检查点 D：Service 通过 Mapper 完成查询
  -> 检查点 E：Spring MVC API 跑通
  -> 检查点 F：声明式事务提交与回滚
  -> 检查点 G：关联查询、分页和安全排序经 HTTP 验证
  -> 第二段复盘和 Module 验收
```

## 4. 最终 Maven 配置

### 4.1 根 POM 的长期管理建议

第一段已经建议将 MyBatis、MyBatis-Spring、HikariCP 和 SLF4J 版本放进根 POM。`03-spring-mvc` 与 `04-mybatis` 还共同使用 Servlet API、Jackson 和 Validation，长期也应由根 POM 管理版本。

推荐根 POM 增加版本属性：

```xml
<jakarta.servlet.version>6.0.0</jakarta.servlet.version>
<jackson.version>2.17.2</jackson.version>
<jakarta.validation.version>3.0.2</jakarta.validation.version>
<hibernate.validator.version>8.0.1.Final</hibernate.validator.version>
<jakarta.el.version>4.0.2</jakarta.el.version>
```

并在根 POM 的 `dependencyManagement` 中管理：

```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>${jakarta.servlet.version}</version>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson</groupId>
    <artifactId>jackson-bom</artifactId>
    <version>${jackson.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>${jakarta.validation.version}</version>
</dependency>

<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>${hibernate.validator.version}</version>
</dependency>

<dependency>
    <groupId>org.glassfish</groupId>
    <artifactId>jakarta.el</artifactId>
    <version>${jakarta.el.version}</version>
</dependency>
```

更好的长期结果是 `03` 和 `04` 都只声明依赖，不各自重复版本。当前不必为了开始第一段立即重构 `03-spring-mvc/pom.xml`；可以在第二段补依赖时统一整理并重新构建两个 Module。

### 4.2 `04-mybatis` 最终依赖

保留第一段依赖，再加入：

```xml
<!-- Spring MVC 和事务基础设施 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
</dependency>

<!-- MyBatis 与 Spring 的适配层 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
</dependency>

<!-- 运行时连接池 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- Tomcat 运行时提供 Servlet API，因此不能打进 WAR -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>

<!-- JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>

<!-- DTO 校验 -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>

<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
</dependency>

<dependency>
    <groupId>org.glassfish</groupId>
    <artifactId>jakarta.el</artifactId>
</dependency>
```

WAR 插件：

```xml
<build>
    <finalName>04-mybatis</finalName>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-war-plugin</artifactId>
            <version>3.4.0</version>
        </plugin>
    </plugins>
</build>
```

不要引入：

- `mybatis-spring-boot-starter`：本阶段没有 Spring Boot。
- `mybatis-plus-*`：会提前隐藏基础 Mapper 和 SQL 映射机制。
- `spring-boot-starter-web`：会改变当前阶段的启动与自动配置边界。

检查点 A：Maven Reload 后确认没有重复版本冲突，`jakarta.servlet-api` 仍为 `provided`。

## 5. Spring 数据源配置

创建 `DatabaseConfig`：

```java
@Configuration
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
                environment.getRequiredProperty("MYBATIS_DB_URL")
        );
        config.setUsername(
                environment.getRequiredProperty("MYBATIS_DB_USERNAME")
        );
        config.setPassword(
                environment.getRequiredProperty("MYBATIS_DB_PASSWORD")
        );
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("mybatis-training-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setInitializationFailTimeout(5000);
        return new HikariDataSource(config);
    }
}
```

继续使用第一段的三个环境变量，但要注意 IDEA 中不同运行配置互不共享：

```text
第一段：StandaloneApplication 的 Application Run Configuration
第二段：Tomcat Server Run Configuration
```

Navicat 的保存密码也不会自动提供给 Java 进程。

不要再声明 `JdbcTemplate` Bean；本阶段数据访问由 MyBatis 完成。

## 6. Spring 创建 MyBatis 基础设施

创建 `MyBatisConfig`：

```java
@Configuration
@EnableTransactionManagement
@MapperScan("cn.siyes.training.mybatis.mapper")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource)
            throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage(
                "cn.siyes.training.mybatis.model"
        );

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(
                resolver.getResources(
                        "classpath*:cn/siyes/training/mybatis/mapper/*.xml"
                )
        );

        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(
                org.apache.ibatis.logging.slf4j.Slf4jImpl.class
        );
        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

需要的主要 import：

```java
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
```

### 为什么第二段不直接加载 `mybatis-config.xml`

第一段的 `mybatis-config.xml` 包含原生环境和数据源配置。第二段的数据源由 Spring 创建，如果同时加载原生 `<environments>`、又在 `SqlSessionFactoryBean` 中配置 Mapper，容易造成职责重复或 Mapper 重复注册。

因此本项目采用：

```text
第一段
  -> mybatis-config.xml 管理原生 environment 和 Mapper

第二段
  -> Spring Java Config 管理 DataSource、设置和 MapperLocations
  -> Mapper XML 继续复用
```

这不是说 Spring 项目不能使用 MyBatis 配置 XML，而是当前分工更清晰。

## 7. `@MapperScan` 到底做了什么

`@MapperScan` 扫描的是 Mapper 接口，不是普通 `@Component` 类。它会为接口注册类似 `MapperFactoryBean` 的 BeanDefinition，最终创建可注入的 Mapper 代理对象。

```text
Spring 启动
  -> @MapperScan 找到 TaskMapper 接口
  -> 注册 MapperFactoryBean
  -> MapperFactoryBean 获得 SqlSessionFactory
  -> 创建基于 SqlSessionTemplate 的 TaskMapper 代理
  -> TaskService 构造器注入 TaskMapper
```

因此：

- Mapper 接口仍然没有手写实现类。
- 使用 `@MapperScan` 后不必在每个接口上再写 `@Mapper`。
- Mapper XML 的 `namespace` 仍必须是接口全限定名。
- Mapper 代理已经是 Spring Bean，可以构造器注入。
- `@Mapper` 和 SQL 注解不是同一概念；`@Mapper` 标识 Mapper 接口，`@Select` 才是把 SQL 写在注解里。

检查点 C：在一个临时启动检查中，从 Spring 容器取得 `TaskMapper`，打印：

```java
System.out.println(taskMapper.getClass());
```

预期是代理类型，而不是你手写的 `TaskMapperImpl`。

## 8. Spring 如何管理 SqlSession

Service 调用 Mapper 时，大致发生：

```text
TaskMapper 代理
  -> SqlSessionTemplate
  -> 查询当前线程是否已有 Spring 事务绑定的 SqlSession
     -> 有：复用当前事务中的 SqlSession/Connection
     -> 无：创建一次方法调用所需的 SqlSession
  -> 执行 MappedStatement
  -> 按 Spring 事务状态提交、回滚或关闭
```

`SqlSessionTemplate` 是线程安全的 Spring 适配对象，但它不会把同一个原生 `SqlSession` 粗暴地共享给所有线程，而是根据当前调用和事务上下文选择正确会话。

第二段业务代码禁止继续这样写：

```java
sqlSessionFactory.openSession();
session.commit();
session.close();
```

如果 Spring 事务中又手动打开独立会话，新会话可能使用另一条连接，不参与当前事务。

## 9. Service 直接依赖 Mapper

基本结构：

```java
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskCommentMapper taskCommentMapper;

    public TaskService(
            TaskMapper taskMapper,
            TaskCommentMapper taskCommentMapper
    ) {
        this.taskMapper = taskMapper;
        this.taskCommentMapper = taskCommentMapper;
    }
}
```

建议事务只标在有明确业务边界的方法上，不要为了省事先给整个类统一加写事务：

```java
@Transactional
public TaskDetail createWithComments(
        CreateTaskRequest request
) {
    // 组装 Task
    // taskMapper.insert(task)
    // 组装评论并设置生成的 task.id
    // taskCommentMapper.insertBatch(comments)
    // 查询并返回详情
}

@Transactional(readOnly = true)
public TaskDetail findDetail(Long id) {
    // 查询；不存在时抛 TaskNotFoundException
}
```

Model 和 DTO 的边界保持上一阶段原则：

```text
Controller 接收 CreateTaskRequest
  -> Service 组装 Task / TaskComment
  -> Mapper 接收持久化对象或查询参数
  -> Mapper 返回 Model
  -> Service 转换为 TaskResponse / TaskDetailResponse
  -> Controller 返回 HTTP 响应
```

不要让 Mapper 接收 `HttpServletRequest`、`ResponseEntity` 或 Web DTO；Mapper 应保持数据访问职责。

## 10. Spring 声明式事务为什么能覆盖多个 Mapper

事务成立需要：

```text
DataSourceTransactionManager 使用的 DataSource
              ==
SqlSessionFactory 使用的 DataSource
```

调用链：

```text
Controller 调用 Spring 代理后的 TaskService
  -> 事务拦截器开启事务
  -> 从同一 DataSource 获取 Connection 并绑定到当前线程
  -> taskMapper.insert() 通过 SqlSessionTemplate 使用该 Connection
  -> taskCommentMapper.insertBatch() 继续使用该 Connection
  -> Service 正常返回：commit
  -> Service 抛出符合回滚规则的异常：rollback
```

默认情况下，`@Transactional` 对 `RuntimeException` 和 `Error` 回滚，对普通受检异常不默认回滚。需要覆盖受检异常时明确写：

```java
@Transactional(rollbackFor = Exception.class)
```

不要捕获异常后只打印而不继续抛出：

```java
try {
    // 两次写操作
} catch (Exception exception) {
    exception.printStackTrace();
    // 错误：方法正常返回，事务代理可能提交
}
```

另外仍然存在 Spring AOP 自调用边界：同一个对象中 `this.createWithComments()` 不会再次经过代理，内部方法上的新事务设置可能不生效。

## 11. 事务核心练习

业务：创建任务时允许同时提交两条初始评论。

正常路径：

```text
INSERT tasks
  -> 获得 task.id
  -> INSERT task_comments (...), (...)
  -> Service 返回
  -> Spring commit
```

回滚路径：临时在任务插入后主动抛出运行时异常：

```java
taskMapper.insert(task);

if (true) {
    throw new IllegalStateException("事务回滚练习");
}

taskCommentMapper.insertBatch(comments);
```

通过 Navicat 验证：

- `tasks` 没有残留任务。
- `task_comments` 没有残留评论。

随后立即删除临时异常，重新部署并验证正常提交。

这项练习证明的是 Spring 事务代理和 MyBatis-Spring 会话同步，而不是证明 SQL “只有 commit 后才执行”。SQL 在事务中已经执行；`commit()` 决定未提交修改是否最终持久化，`rollback()` 撤销尚未提交的修改。

检查点 F：能说明为什么两个 Mapper 共享事务，以及换成另一个 DataSource 后为什么会破坏当前事务边界。

## 12. 接回 Spring MVC

### 12.1 `web.xml`

沿用 `03-spring-mvc` 的 Servlet 6.0 配置，至少修改：

```xml
<display-name>04-mybatis</display-name>
```

以及配置类：

```xml
<param-value>cn.siyes.training.mybatis.config.WebMvcConfig</param-value>
```

`DispatcherServlet` 的映射继续使用：

```xml
<url-pattern>/</url-pattern>
```

### 12.2 单容器配置

当前继续采用单个 `WebApplicationContext`，`WebMvcConfig` 扫描：

```java
@ComponentScan("cn.siyes.training.mybatis")
```

这样会同时发现 Controller、Service、`DatabaseConfig` 和 `MyBatisConfig`。本阶段重点是 MyBatis，不再重复父子容器练习。

### 12.3 API 范围

先在 IDEA 的 Tomcat 配置中完成部署：

```text
Run -> Edit Configurations -> Tomcat Server -> Local
  -> Deployment
  -> 添加 04-mybatis:war exploded
  -> Application context 设置为 /04_mybatis
  -> 在该 Tomcat 运行配置中加入 MYBATIS_DB_* 三个环境变量
```

不要让其他 Artifact 占用相同 Context Path。启动时先确认 Hikari、`DispatcherServlet`、`SqlSessionFactory` 和 Mapper XML 均加载成功，再发送业务请求。

保留任务 API：

```text
POST   /api/tasks
GET    /api/tasks/{id}
GET    /api/tasks
PUT    /api/tasks/{id}
PATCH  /api/tasks/{id}/status
DELETE /api/tasks/{id}
```

新增用于证明关联映射的接口：

```text
GET  /api/tasks/{id}/detail
POST /api/tasks/{id}/comments
```

创建任务请求可增加可选字段：

```json
{
  "title": "学习 MyBatis",
  "description": "完成 XML 动态 SQL",
  "dueDate": "2026-08-30",
  "initialComments": [
    "先完成原生 MyBatis",
    "再接入 Spring"
  ]
}
```

`initialComments` 用于验证一次 Service 调用内的两表事务。

### 12.4 返回对象由谁组装

Mapper 返回数据库模型，Service 负责业务编排和 Model -> Response 转换，Controller 决定 HTTP 状态：

```text
Mapper：数据库数据形状
Service：业务规则与跨 Mapper 事务
Controller：HTTP 输入输出与状态码
```

不要让 Mapper 返回 `ResponseEntity`；也不要让 Service 硬编码 HTTP `201`、`404` 等状态。

## 13. 动态 SQL 经 HTTP 验证

查询示例：

```http
GET /04_mybatis/api/tasks?page=1&size=10&status=TODO&keyword=MyBatis&sortBy=dueDate&direction=asc
```

需要观察：

- Controller 的 DTO 是否正确接收查询参数。
- Service 是否校验页码、每页数量和排序范围。
- Mapper XML 是否只拼入命中的 `<if>` 条件。
- `COUNT(*)` 是否和列表使用相同条件。
- 控制台 SQL 与参数日志是否符合预期。
- Navicat 手动执行同条件 SQL 时结果是否一致。

恶意排序字段：

```text
sortBy=id desc; delete from tasks
```

预期只能进入默认白名单分支，不能被直接拼进 SQL。

## 14. 一对多查询方案边界

本轮用 JOIN + `<collection>` 查询单个任务详情，目的是练习结果映射。你还需要知道另一种方式：

```xml
<collection
    property="comments"
    column="id"
    select="cn.siyes.training.mybatis.mapper.TaskCommentMapper.findByTaskId"/>
```

它会先查任务，再按任务 ID 查评论。查询一个任务时容易理解，但查询任务列表时可能产生：

```text
1 次任务列表查询 + N 次评论查询
```

这就是常见的 N+1 查询问题。本轮要求：

- 单个详情使用 JOIN + `<collection>`。
- 分页列表不加载评论。
- 不为了演示而对分页列表使用嵌套 select。

## 15. SQL 日志应该看什么

不要只看“打印了 SQL”。至少区分：

```text
Preparing: SELECT ... WHERE status = ?
Parameters: TODO(String)
Total: 3
```

- `Preparing`：最终 SQL 结构，`#{}` 已变为 `?`。
- `Parameters`：实际绑定值和类型。
- `Total`：查询返回行数。

若日志中直接出现未经白名单处理的客户端排序文本，说明 `${}` 或字符串拼接边界有问题。

生产环境不会无条件打印所有参数，因为可能包含密码、Token 或隐私数据。本阶段开启 SQL 日志只用于学习。

## 16. 第一段与第二段的对照复盘

完成后填写：

| 问题 | 原生 MyBatis | Spring 集成后 |
| --- | --- | --- |
| 谁创建 `SqlSessionFactory` |  |  |
| 谁创建和关闭 `SqlSession` |  |  |
| 谁创建 Mapper 代理 |  |  |
| 谁决定事务提交/回滚 |  |  |
| 数据源由谁创建 |  |  |
| Mapper XML 是否变化 |  |  |
| SQL 参数与结果映射是否变化 |  |  |

预期核心结论：Spring 改变了对象创建、会话获取和事务管理方式，但 Mapper 方法如何定位 SQL、`#{}` 如何绑定参数、ResultSet 如何映射对象，仍然是 MyBatis 的能力。

## 17. 与 `linux-server` 的最小对照

本段验收后再对照，不提前复制：

- 当前 `linux-server` 使用的是 `JdbcTemplate` 还是 MyBatis。
- 数据访问 SQL 位于 Java、XML 还是资源文件。
- Service 的事务边界在哪里。
- Spring Boot 是否自动提供 DataSource、事务管理器或 Mapper 扫描。
- 如果将其改为 MyBatis，Controller 和 DTO 是否应该发生大范围变化。

对照目的是识别框架职责，不把 AI 生成项目作为你的能力证据。

## 18. 第二段手动验收

通过 Apifox：

- 创建任务和两条初始评论成功。
- 按 ID 查询任务成功。
- 动态筛选、分页和安全排序成功。
- 修改任务和状态成功。
- 新增评论成功。
- 查询任务详情能返回评论集合。
- 删除任务后评论按外键策略同步删除。
- 不存在任务返回稳定 `404 JSON`。
- DTO 校验失败返回 `400`。

通过 Navicat：

- HTTP 写入结果与两张表一致。
- 正常事务两张表同时新增。
- 主动异常时两张表都不新增。
- 删除后的外键级联结果符合设计。

通过控制台：

- 能看到 Mapper SQL 和绑定参数。
- 能看到 Spring MVC Interceptor 匹配到 Controller 方法。
- 日志中不打印真实密码。

## 19. 最终验收问题

1. Mapper 接口没有实现类，为什么可以注入和调用？
2. `@MapperScan`、`MapperFactoryBean`、`SqlSessionTemplate` 分别做什么？
3. Mapper 方法怎样定位到 XML 中的 SQL？
4. `#{}` 为什么通常能防止 SQL 注入，`${}` 为什么危险？
5. `<where>`、`<set>`、`<foreach>`、`<choose>` 分别解决什么问题？
6. 一对多 JOIN 结果为什么需要 `<id>` 和 `<collection>`？
7. 为什么不能直接对一对多 JOIN 结果行做任务分页？
8. 原生 MyBatis 和 Spring 集成后的 `SqlSession` 生命周期有什么区别？
9. 两个 Mapper 为什么能参与同一个 Spring 事务？
10. 哪些异常默认触发 `@Transactional` 回滚？捕获并吞掉异常有什么后果？
11. 从 `JdbcTemplate` Repository 换成 MyBatis Mapper 后，哪些层改变，哪些层应该保持稳定？
12. Spring Boot 将来会自动完成哪些配置，但哪些 SQL 和映射机制仍属于 MyBatis？

## 20. 第二段验收清单

- [ ] Spring 成功创建 Hikari DataSource、`SqlSessionFactory` 和事务管理器。
- [ ] `@MapperScan` 将 Mapper 代理注册为 Spring Bean。
- [ ] Service 不手动创建、提交或关闭 `SqlSession`。
- [ ] Spring MVC 任务 CRUD 使用 MyBatis 持久化。
- [ ] 动态 SQL、分页、排序白名单和 SQL 日志验证完成。
- [ ] 任务详情的一对多结果映射正确。
- [ ] 批量插入评论可用。
- [ ] 两表正常事务提交和主动异常回滚均经 Navicat 验证。
- [ ] 能解释 Spring 事务代理与 MyBatis-Spring 会话同步关系。
- [ ] 能完整回答第 19 节，并形成自己的 MyBatis 复盘。
- [ ] Maven WAR 构建成功，重新部署后核心 API 可用。

只有第一、第二段都验收后，`04-mybatis` 才能标记为 `已完成`。
