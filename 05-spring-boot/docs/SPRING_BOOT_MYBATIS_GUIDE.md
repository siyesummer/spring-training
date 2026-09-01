# Spring Boot 第二轮：MyBatis 实战引导

> 项目：`05-spring-boot`
>
> 前置：第一轮 Spring Boot + JdbcTemplate 已完成并已复盘。
>
> 本轮目标：在不改变 HTTP 层主要接口的前提下，用 Spring Boot + MyBatis 替换第一轮的 JDBC Repository，并观察 Boot 如何自动组装 MyBatis 基础设施。

## 一、本轮真正要掌握什么

第一轮已经证明了 Boot 可以自动配置 Web、DataSource、JdbcTemplate 和事务。本轮不再重复这些内容，而是回答：

- `mybatis-spring-boot-starter` 只是依赖集合，还是也会带来自动配置？
- Boot 如何创建 `SqlSessionFactory`、`SqlSessionTemplate`？
- `@MapperScan` 如何把 Mapper 接口变成可以注入的代理对象？
- Mapper XML 中的 `namespace`、`id`、参数名和返回类型如何对应？
- MyBatis 如何复用 Boot 创建的 `DataSource` 并加入 Spring 的 `@Transactional`？
- 哪些 SQL 仍然由自己编写，哪些对象和生命周期交给 Boot / MyBatis-Spring 管理？

本轮请求链目标：

```text
HTTP
  -> Controller
  -> Service（业务规则与事务边界）
  -> TaskRepository 接口
  -> MyBatis Mapper 代理
  -> SqlSessionTemplate
  -> SqlSessionFactory
  -> DataSource / JDBC
  -> MySQL
```

第一轮的 `JdbcTaskRepository` 不要立即删除。保留它用于对照，新增 MyBatis 实现或在确认切换后再停用，这样可以比较“手写 JdbcTemplate”与“Mapper 代理”的职责差异。

## 二、依赖配置

### 2.1 在 `05-spring-boot/pom.xml` 中加入

Spring Boot 4 使用 Spring Framework 7，MyBatis Starter 应选择支持 Boot 4 的 4.x 版本。当前引导以 `4.0.0` 为示例；如果 IDEA/Maven 仓库显示的兼容版本不同，以 MyBatis 官方发布说明和实际可解析版本为准，不要把 Boot 3 时代的 Starter 版本直接混用。

```xml
<!-- MyBatis 与 Spring Boot 的自动配置适配层 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>4.0.0</version>
</dependency>
```

第一轮已经存在的依赖继续保留：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

不要在 Module 中另外手动声明 `mybatis`、`mybatis-spring`、Spring JDBC 或连接池版本，除非依赖树或官方兼容矩阵明确要求。Starter 会引入适配版本，Boot 父 POM 负责它能够管理的版本。

加入依赖后由你手动完成：

1. IDEA Maven 面板 Reload。
2. 查看 External Libraries，确认 `mybatis-spring-boot-autoconfigure`、`mybatis-spring` 和 `mybatis` 已进入依赖树。
3. 如果出现 Spring Framework 主版本冲突，先停止编码，检查 Starter 版本兼容性。

## 三、配置 MyBatis

在 `src/main/resources/application.yaml` 的 `spring` 同级增加：

```yaml
mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: cn.siyes.training.boot.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

逐项理解：

- `mapper-locations`：告诉 MyBatis 从 classpath 哪些位置加载 Mapper XML。
- `type-aliases-package`：允许 XML 中用 `Task` 代替完整类名；它只影响别名解析，不会把 model 注册成 Spring Bean。
- `map-underscore-to-camel-case`：把 `created_at` 映射到 `createdAt`，把 `due_date` 映射到 `dueDate`。
- `log-impl`：训练阶段打印 SQL 和参数，便于观察映射；生产环境不建议直接使用 StdOut 日志实现。

配置文件的数据库部分仍然沿用第一轮：

```yaml
spring:
  datasource:
    url: ${BOOT_DB_URL}
    username: ${BOOT_DB_USERNAME}
    password: ${BOOT_DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

本轮不再手动创建 `DataSource`、`SqlSessionFactory` 或 `SqlSessionTemplate`。先理解自动配置的结果，再决定是否需要自定义 Bean。

## 四、Boot 自动配置链路

加入 Starter 后，启动时大致发生：

```text
mybatis-spring-boot-starter
  -> MyBatis 自动配置类进入 classpath
  -> MybatisAutoConfiguration 被候选配置导入
  -> 读取 spring.datasource.* 和 mybatis.*
  -> 注入 Boot 已创建的 DataSource
  -> 创建 SqlSessionFactory
  -> 创建 SqlSessionTemplate
  -> @MapperScan 通过 MapperScanRegistrar 注册 Mapper 扫描器
  -> 每个 Mapper 接口注册 MapperFactoryBean
  -> 注入 Mapper 代理
```

其中，`MybatisAutoConfiguration` 主要负责 `SqlSessionFactory`、`SqlSessionTemplate` 等基础设施；`@MapperScan` 是 MyBatis-Spring 提供的独立注册入口，它通过 `MapperScanRegistrar` 向 Spring 注册扫描器。两者经常同时出现，但职责并不相同。

这里要区分三层：

| 对象 | 负责什么 |
| --- | --- |
| `SqlSessionFactory` | 创建和管理 MyBatis `SqlSession` 所需的配置与执行器 |
| `SqlSessionTemplate` | 让 Mapper 调用使用 Spring 管理的线程绑定会话，并参与 Spring 事务 |
| Mapper 代理 | 把接口方法转换为对应的 `MappedStatement` 执行 |

MyBatis 不会通过 `new TaskMapper()` 创建接口实例。`@MapperScan` 会注册工厂 Bean，运行时返回代理对象；调用接口方法时，代理根据接口全限定名和方法名查找 XML 或注解 SQL。

## 五、注册 Mapper

### 5.1 在启动类上增加扫描

在 `Application` 上加入：

```java
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("cn.siyes.training.boot.mapper")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@MapperScan` 的作用不是执行 SQL，而是扫描指定包中的接口，并为每个接口注册 Mapper 工厂 Bean。也可以在接口上逐个写 `@Mapper`，但本轮使用包扫描，观察集中注册方式。

### 5.2 建议包结构

```text
cn.siyes.training.boot
├─ controller
├─ service
├─ repository
│  ├─ TaskRepository.java
│  └─ MybatisTaskRepository.java   （Repository 适配层，可选）
├─ mapper
│  ├─ TaskMapper.java
│  ├─ TaskCommentMapper.java
│  └─ TaskDetailMapper.java
├─ model
├─ dto
└─ exception

src/main/resources
└─ mapper
   ├─ TaskMapper.xml
   ├─ TaskCommentMapper.xml
   └─ TaskDetailMapper.xml
```

Mapper 是数据访问接口，Repository 是业务层看到的数据访问抽象。为了对照第一轮，可以让 `MybatisTaskRepository` 实现已有的 `TaskRepository`，内部组合 `TaskMapper`，保持 Service 和 Controller 不感知 MyBatis：

```text
Service -> TaskRepository -> MybatisTaskRepository -> TaskMapper
```

这样做比让 Service 直接依赖 Mapper 更适合本轮学习，因为你能清楚观察“框架 Mapper”和“项目 Repository”各自的边界。确认理解后，再讨论是否需要在实际项目中省略这一层。

## 六、手写 `TaskMapper` 和 XML

### 6.1 Mapper 接口

先完成最小查询和新增：

```java
package cn.siyes.training.boot.mapper;

import cn.siyes.training.boot.model.Task;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskMapper {
    Task findById(Long id);

    int insert(Task task);

    int update(Task task);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(Long id);

    List<Task> findPage(@Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("offset") int offset,
                        @Param("size") int size);

    long count(@Param("keyword") String keyword,
               @Param("status") String status);
}
```

`@Param` 的作用是给 XML 参数命名。多个简单参数如果没有明确名称，XML 中的可用名称可能变成 `arg0`、`arg1` 或 `param1`、`param2`，不适合阅读和维护。

### 6.2 XML 的 namespace 和 id

文件：`src/main/resources/mapper/TaskMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="cn.siyes.training.boot.mapper.TaskMapper">
    <resultMap id="taskResultMap" type="Task">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="description" column="description"/>
        <result property="status" column="status"/>
        <result property="dueDate" column="due_date"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="findById" resultMap="taskResultMap">
        SELECT id, title, description, status, due_date, created_at, updated_at
        FROM tasks
        WHERE id = #{id}
    </select>

    <insert id="insert" parameterType="Task"
            useGeneratedKeys="true" keyProperty="id" keyColumn="id">
        INSERT INTO tasks (title, description, status, due_date)
        VALUES (#{title}, #{description}, #{status}, #{dueDate})
    </insert>

    <update id="update" parameterType="Task">
        UPDATE tasks
        <set>
            <if test="title != null">title = #{title},</if>
            <if test="description != null">description = #{description},</if>
            <if test="dueDate != null">due_date = #{dueDate},</if>
        </set>
        WHERE id = #{id}
    </update>

    <update id="updateStatus">
        UPDATE tasks
        SET status = #{status}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM tasks WHERE id = #{id}
    </delete>
</mapper>
```

必须满足：

```text
namespace = Mapper 接口全限定名
<select / <insert / <update / <delete> 的 id = 接口方法名
#{...} = 预编译参数绑定
```

`useGeneratedKeys` 会把数据库自增主键写回传入的 `Task.id`，这样 Repository 可以继续按第一轮方式返回创建后的任务。

### 6.3 动态条件和分页

第二步再加入条件查询，不要一开始就堆完整 SQL：

```xml
<sql id="taskConditions">
    <where>
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%')
                 OR description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
    </where>
</sql>

<select id="findPage" resultMap="taskResultMap">
    SELECT id, title, description, status, due_date, created_at, updated_at
    FROM tasks
    <include refid="taskConditions"/>
    ORDER BY created_at DESC, id DESC
    LIMIT #{size} OFFSET #{offset}
</select>

<select id="count" resultType="long">
    SELECT COUNT(*) FROM tasks
    <include refid="taskConditions"/>
</select>
```

本轮仍然使用 `#{}`。排序字段不能直接使用 `#{sortBy}`，因为预编译参数不能替换列名；如果要支持排序，使用 `<choose>` 白名单映射固定列名，禁止把前端原始字符串直接拼入 `${}`。

## 七、Repository 适配与 Service 事务

### 7.1 Repository 适配

`MybatisTaskRepository` 通过构造器注入 `TaskMapper`：

```java
@Repository
public class MybatisTaskRepository implements TaskRepository {
    private final TaskMapper taskMapper;

    public MybatisTaskRepository(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    // 在这里把 TaskMapper 的返回值转换为 TaskRepository 约定
    // 例如 findById 返回 Optional<Task>，不存在时由 Service 抛出异常
}
```

第一轮的 `JdbcTaskRepository` 暂时不要与新实现同时标记为 `@Repository` 并实现同一个接口，否则 Service 构造器注入 `TaskRepository` 时会出现多个候选 Bean。切换方式二选一：

1. 暂时移除旧实现的 `@Repository`，保留源码作对照；
2. 给两套实现命名，并使用 `@Primary` 或 `@Qualifier` 明确当前 Service 使用哪一个。

学习阶段建议先采用第一种，避免把重点转移到 Bean 候选冲突。

### 7.2 事务如何接入

如果继续保留 `spring-boot-starter-jdbc`，Boot 会基于同一个 `DataSource` 自动配置事务管理器。Service 层原有的：

```java
@Transactional
public int createAndUpdateStatus(CreateTaskRequest request) {
    // insert -> updateStatus
}
```

可以继续使用。MyBatis Mapper 调用通过 `SqlSessionTemplate` 获取当前事务绑定的连接，因此两个 SQL 会参与同一个 Spring 事务。

事务链路是：

```text
外部调用 Service 代理
  -> Spring 事务拦截器开启事务
  -> Mapper 代理执行 insert
  -> Mapper 代理执行 update
  -> 正常返回 -> commit
  -> 运行时异常 -> rollback
```

不要在 Service 中手动 `openSession()`、`commit()` 或 `close()`；Spring 集成阶段这些生命周期由 `SqlSessionTemplate` 和事务管理器协调。只有在原生 MyBatis 练习中才手动管理 `SqlSession`。

## 八、加入任务评论一对多查询

第一轮已有 `task_comments` 表，本轮用它练习 Boot 环境中的结果映射。建议新增：

- `TaskComment` model；
- `TaskDetail`，包含一个 `Task` 字段或任务字段加 `List<TaskComment>`；
- `TaskCommentMapper`；
- `TaskDetailMapper.xml`。

最小一对多映射形态：

```xml
<resultMap id="taskDetailMap" type="TaskDetail">
    <id property="id" column="task_id"/>
    <result property="title" column="task_title"/>
    <collection property="comments" ofType="TaskComment">
        <id property="id" column="comment_id"/>
        <result property="content" column="comment_content"/>
        <result property="createdAt" column="comment_created_at"/>
    </collection>
</resultMap>

<select id="findDetailById" resultMap="taskDetailMap">
    SELECT t.id AS task_id,
           t.title AS task_title,
           c.id AS comment_id,
           c.content AS comment_content,
           c.created_at AS comment_created_at
    FROM tasks t
    LEFT JOIN task_comments c ON c.task_id = t.id
    WHERE t.id = #{id}
    ORDER BY c.created_at ASC, c.id ASC
</select>
```

重点观察：JOIN 结果可能有多行，但 MyBatis 根据父级 `<id>` 把相同任务合并成一个对象，再把每行评论放入 `comments` 集合。`LEFT JOIN` 能让没有评论的任务仍然被查询出来。

## 九、推荐手写顺序与检查点

### 步骤 1：只接入 Starter

- 加入依赖并 Reload Maven。
- 增加 `mybatis` 配置。
- 增加 `@MapperScan`。
- 暂时不写业务 Mapper，先确认应用能启动。

检查点：能解释 Starter、自动配置、`SqlSessionFactory`、`SqlSessionTemplate` 和 Mapper 扫描各自的职责。

### 步骤 2：完成一个 `findById`

- 创建 `TaskMapper`。
- 创建 `TaskMapper.xml`。
- 保证 `namespace` 和 `id` 对应。
- 用 Repository 适配层调用 Mapper。
- 暂时只验证 `GET /tasks/{id}`。

检查点：断点观察注入的 `TaskMapper` 实际是代理对象，而不是接口实例；观察 SQL 日志和结果映射。

### 步骤 3：补齐 CRUD 和分页

- 新增、更新、状态更新、删除。
- 使用 `useGeneratedKeys` 回写主键。
- 使用 `<where>`、`<if>`、`<set>`、`<include>` 完成条件 SQL。
- 排序只允许白名单字段。

检查点：保持 Controller API 基本不变，对照第一轮 JdbcTemplate 实现，说明 SQL 和对象映射分别从哪里负责。

### 步骤 4：加入一对多详情和评论

- 用 Navicat 确认 `tasks` 与 `task_comments` 数据。
- 完成 JOIN、`<resultMap>` 和 `<collection>`。
- 验证没有评论和有多条评论两种情况。

检查点：解释为什么 JOIN 的多行结果最终能合并成一个任务对象。

### 步骤 5：验证 Spring 事务

- 使用已有的 `createAndUpdateStatus` 事务方法。
- 在第二条 Mapper 调用后临时抛出运行时异常。
- 用 Navicat 确认两次更新都回滚。
- 删除临时异常后再次确认正常提交。

检查点：说明事务由 Service 代理开启，Mapper 使用同一个 Spring 事务连接，而不是 Mapper 自己提交。

## 十、最小验收要求

本轮完成前，至少要有以下证据：

- Starter 依赖可以解析，应用能启动。
- 能解释 `MybatisAutoConfiguration` 如何复用 Boot 的 `DataSource`。
- `@MapperScan` 能将 Mapper 接口注册为可注入代理。
- `findById`、创建、更新、状态更新、删除和条件分页可用。
- XML 的 `namespace`、方法 `id`、`#{}`、`<where>`、`<if>`、`<set>` 至少亲手使用并能解释。
- 自增主键能回写到 Java 对象。
- 任务详情能通过 `<collection>` 映射多条评论。
- Service 的 `@Transactional` 能让多个 Mapper 操作一起提交或回滚。
- Navicat 中的数据结果与接口结果一致。
- 能说明第一轮 JdbcTemplate 与本轮 MyBatis 在连接、SQL、映射和事务职责上的差异。

## 十一、常见错误定位顺序

遇到问题时按这条链检查，不要先改一堆配置：

```text
依赖是否解析
  -> Mapper XML 是否复制到 target/classes
  -> mapper-locations 是否匹配路径
  -> namespace 是否等于接口全限定名
  -> id 是否等于方法名
  -> 参数名是否通过 @Param 明确
  -> resultMap / resultType 是否匹配 Java 类型
  -> DataSource 是否仍然连接正确数据库
  -> 旧 JdbcTaskRepository 是否造成多个 TaskRepository Bean
```

典型现象：

- `Invalid bound statement`：通常是 XML 没加载、namespace 错或 id 错。
- `NoSuchBeanDefinitionException`：通常是未扫描 Mapper 或包路径写错。
- `NoUniqueBeanDefinitionException`：通常是旧、新 Repository 同时注册。
- 字段为 `null`：检查列名、别名、`resultMap` 和下划线转驼峰配置。
- 事务未回滚：检查异常是否从 Service 代理边界抛出，是否在同一 DataSource 上执行，是否错误地手动创建了会话。

## 十二、第二轮复盘问题

1. `mybatis-spring-boot-starter` 与 `mybatis-spring`、MyBatis 核心框架分别是什么关系？
2. `MybatisAutoConfiguration` 创建了哪些基础设施？它依赖哪些条件？
3. `@MapperScan` 为什么能让接口被注入？注入的对象是什么？
4. `SqlSessionTemplate` 为什么能让 Mapper 参与 Spring 事务？
5. `namespace`、`id`、`#{}` 和 `resultMap` 分别解决什么问题？
6. 为什么 SQL 仍然需要自己写，Boot 却能自动提供 `SqlSessionFactory`？
7. 为什么 Repository 适配层可以让 Controller 和 Service 不感知底层从 JdbcTemplate 换成 MyBatis？
8. `useGeneratedKeys` 如何把数据库主键写回 Java 对象？
9. `<collection>` 如何把 JOIN 的多行结果合并成一个父对象？
10. MyBatis 的 Mapper 事务和前面原生 MyBatis 手动 `commit()` 的边界有什么不同？

## 十三、范围边界

本轮暂不追求：

- MyBatis-Plus；
- 二级缓存、插件、复杂 `TypeHandler`；
- 多数据源和复杂事务传播；
- 为每条 SQL 编写大量自动化测试。

重点是把“Boot 自动配置的 MyBatis 基础设施”和“自己编写的 Mapper SQL、结果映射、Repository 适配、Service 事务边界”区分清楚，并能独立写出最小可运行版本。
