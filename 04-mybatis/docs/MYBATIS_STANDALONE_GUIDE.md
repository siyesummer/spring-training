# 第一段：原生 MyBatis 详细引导

> 前置条件：`04-mybatis` Module 已创建。
> 本段运行方式：普通 Java `main()`，不启动 Tomcat。
> 本段验收目标：不用 Spring 也能解释并运行 MyBatis 的配置、会话、Mapper 代理、XML SQL、动态 SQL、结果映射和手动事务。

## 1. 这一段真正要掌握什么

完成本段后，你需要能解释这条链路：

```text
mybatis-config.xml
  -> SqlSessionFactoryBuilder 读取配置
  -> 创建 SqlSessionFactory
  -> SqlSessionFactory.openSession()
  -> 创建 SqlSession
  -> sqlSession.getMapper(TaskMapper.class)
  -> 创建 TaskMapper 动态代理对象
  -> 调用 Mapper 方法
  -> namespace + 方法名定位 MappedStatement
  -> 绑定参数并生成 PreparedStatement
  -> JDBC 执行 SQL
  -> ResultSet 映射为 Java 对象
```

前端类比只能帮助理解入口，不能替代后端机制：

```text
Mapper 接口方法       类似类型化的数据请求函数声明
Mapper XML           类似独立维护的查询定义，但它实际保存 SQL 和映射规则
Mapper 动态代理       类似运行时生成的实现对象
SqlSession           类似一次数据库工作上下文，同时持有事务边界和一级缓存
```

关键区别是：Mapper 最终会通过 JDBC 访问数据库，`SqlSession` 不能被当作普通无状态工具长期共享。

## 2. 第一段功能边界

本段实现：

- 新增任务并取得数据库生成的 ID。
- 按 ID 查询任务。
- 动态条件查询和分页。
- 修改任务。
- 删除任务。
- 批量新增任务评论。
- 查询任务及其评论的一对多详情。
- 手动提交与回滚。
- 用一个简单注解 SQL 和 XML 做对照。

暂不实现：

- Spring Bean 管理。
- `@MapperScan`。
- `@Transactional`。
- Controller 和 HTTP API。
- MyBatis-Plus。
- 分页插件。

## 3. 分步顺序

严格按下面顺序推进：

```text
检查点 A：依赖可解析
  -> 检查点 B：数据库和表创建完成
  -> 检查点 C：SqlSessionFactory 可以创建
  -> 检查点 D：第一个 XML 查询成功
  -> 检查点 E：CRUD 和生成主键成功
  -> 检查点 F：动态 SQL 与安全排序成功
  -> 检查点 G：一对多和批量操作成功
  -> 检查点 H：手动事务提交/回滚成功
  -> 第一段复盘
```

不要先写完所有 Mapper 再统一运行。每过一个检查点，先观察结果和调用链。

## 4. Maven 依赖

### 4.1 版本放在哪里

`mysql-connector-j` 已由根 POM 的 `dependencyManagement` 管理。MyBatis 会继续用于后面的 Spring Boot Module，因此 MyBatis 版本也适合由根 POM 统一管理，而不是散落在 `04-mybatis/pom.xml`。

建议你在根 `pom.xml` 的 `<properties>` 中手动加入：

```xml
<mybatis.version>3.5.19</mybatis.version>
<mybatis.spring.version>3.0.4</mybatis.spring.version>
<hikaricp.version>5.1.0</hikaricp.version>
<slf4j.version>2.0.16</slf4j.version>
```

然后在根 POM 的 `<dependencyManagement><dependencies>` 中加入：

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>${mybatis.version}</version>
</dependency>

<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>${mybatis.spring.version}</version>
</dependency>

<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>${hikaricp.version}</version>
</dependency>

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>${slf4j.version}</version>
</dependency>
```

说明：

- `mybatis` 是核心框架，本段立即使用。
- `mybatis-spring` 是第二段的适配层，本段只管理版本，暂不声明为 Module 依赖。
- `HikariCP` 第二段使用，本段先用 MyBatis 自带的学习型连接池。
- `slf4j-simple` 让控制台能看到 MyBatis SQL 日志。
- 根 POM 的 `dependencyManagement` 只管理版本，不会自动把 JAR 加入任何 Module。

### 4.2 第一段 Module 依赖

在 `04-mybatis/pom.xml` 中先手动加入：

```xml
<dependencies>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
    </dependency>

    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
    </dependency>

    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

`mysql-connector-j` 使用 `runtime` 是因为你的源码不直接引用 MySQL Driver 类型；运行时 MyBatis/JDBC 根据驱动类名加载它。

检查点 A：在 IDEA Maven 面板 Reload 后，确认 External Libraries 中存在 MyBatis、MySQL Driver 和 SLF4J。此时不要求写测试。

## 5. 数据库设计与 Navicat 操作

### 5.1 为什么使用独立数据库

新建：

```text
spring_training_mybatis
```

不要继续改 `spring_training_mvc`。上一阶段已经验收完成，独立数据库可以避免练习动态 SQL、外键和事务时破坏原有证据。

数据库账号是 MySQL Server 级账号，只要它拥有新库权限，就可以继续使用；账号不是只属于某一个数据库。若现有账号没有权限，再在 Navicat 中授权。

### 5.2 在 Navicat 查询窗口执行

把以下 SQL 同步保存到：

```text
src/main/resources/db/schema.sql
```

然后在 Navicat 新建查询并手动执行：

```sql
CREATE DATABASE IF NOT EXISTS spring_training_mybatis
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE spring_training_mybatis;

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    due_date DATE NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_mybatis_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    INDEX idx_mybatis_tasks_status_created_at (status, created_at),
    INDEX idx_mybatis_tasks_due_date (due_date)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS task_comments (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE,
    INDEX idx_task_comments_task_id_created_at (task_id, created_at)
) ENGINE = InnoDB;
```

关系：

```text
tasks.id
   1
   |
   n
task_comments.task_id
```

`ON DELETE CASCADE` 表示删除任务时数据库自动删除其评论。它适合当前练习，但生产项目是否使用级联删除要结合审计和误删风险决定。

检查点 B：在 Navicat 中确认两张表、外键和索引存在，并确认字符集为 `utf8mb4`。

## 6. 包结构和类职责

第一段先创建：

```text
cn.siyes.training.mybatis
├─ mapper/
│  ├─ TaskMapper.java
│  └─ TaskCommentMapper.java
├─ model/
│  ├─ Task.java
│  ├─ TaskComment.java
│  ├─ TaskDetail.java
│  ├─ TaskQuery.java
│  └─ TaskStatus.java
└─ standalone/
   ├─ MyBatisFactory.java
   └─ StandaloneApplication.java
```

职责：

| 类型 | 职责 |
| --- | --- |
| `Task` | 对应任务表的一行数据 |
| `TaskComment` | 对应评论表的一行数据 |
| `TaskDetail` | 承载任务和评论集合的一对多查询结果 |
| `TaskQuery` | 承载筛选、分页和排序条件 |
| `TaskMapper` | 声明任务数据访问方法，不写实现类 |
| `TaskCommentMapper` | 声明评论数据访问方法 |
| `MyBatisFactory` | 读取配置并创建唯一的 `SqlSessionFactory` |
| `StandaloneApplication` | 第一段手动调用与观察入口，不承载业务层设计 |

Model 保持普通 JavaBean：私有字段、无参构造器、getter/setter。字段建议：

```text
Task
  id: Long
  title: String
  description: String
  status: TaskStatus
  dueDate: LocalDate
  createdAt: LocalDateTime
  updatedAt: LocalDateTime

TaskComment
  id: Long
  taskId: Long
  content: String
  createdAt: LocalDateTime

TaskDetail
  Task 的全部字段
  comments: List<TaskComment>
```

数据库主键使用 `BIGINT UNSIGNED`，Java 仍使用 `Long`。Java 没有直接对应的无符号 `long`，当前数据量不会触及差异边界。

## 7. MyBatis 主配置

创建：

```text
src/main/resources/mybatis-config.xml
```

完整骨架：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <setting name="logImpl" value="SLF4J"/>
    </settings>

    <typeAliases>
        <package name="cn.siyes.training.mybatis.model"/>
    </typeAliases>

    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="${db.url}"/>
                <property name="username" value="${db.username}"/>
                <property name="password" value="${db.password}"/>
            </dataSource>
        </environment>
    </environments>

    <mappers>
        <mapper resource="cn/siyes/training/mybatis/mapper/TaskMapper.xml"/>
        <mapper resource="cn/siyes/training/mybatis/mapper/TaskCommentMapper.xml"/>
    </mappers>
</configuration>
```

注意 XML 元素有固定顺序。常用顺序是：

```text
properties
-> settings
-> typeAliases
-> typeHandlers
-> environments
-> databaseIdProvider
-> mappers
```

即使标签名称正确，顺序错误也可能导致配置解析失败。

这里不把密码直接写进 XML。`${db.url}` 等占位符由 Java 创建 `SqlSessionFactory` 时传入。注意：配置文件中的 `${}` 是配置占位符替换；Mapper SQL 中的 `${}` 是 SQL 文本拼接。写法相同，但上下文和风险不同。

JDBC URL 示例：

```text
jdbc:mysql://localhost:3306/spring_training_mybatis?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
```

在 XML 属性中，`&` 必须写成 `&amp;`；如果 URL 从环境变量传入，就直接使用普通 `&`，不要写 `\=`。

## 8. 创建 SqlSessionFactory

IDEA 的 `StandaloneApplication` 运行配置中添加：

```text
MYBATIS_DB_URL=jdbc:mysql://localhost:3306/spring_training_mybatis?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
MYBATIS_DB_USERNAME=你的数据库用户名
MYBATIS_DB_PASSWORD=你的数据库密码
```

`MyBatisFactory` 的核心结构：

```java
public final class MyBatisFactory {

    private static final SqlSessionFactory SQL_SESSION_FACTORY = build();

    private MyBatisFactory() {
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return SQL_SESSION_FACTORY;
    }

    private static SqlSessionFactory build() {
        Properties properties = new Properties();
        properties.setProperty("db.url", requireEnv("MYBATIS_DB_URL"));
        properties.setProperty("db.username", requireEnv("MYBATIS_DB_USERNAME"));
        properties.setProperty("db.password", requireEnv("MYBATIS_DB_PASSWORD"));

        try (InputStream input = Resources.getResourceAsStream("mybatis-config.xml")) {
            return new SqlSessionFactoryBuilder()
                    .build(input, "development", properties);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 MyBatis 配置失败", exception);
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量: " + name);
        }
        return value;
    }
}
```

对象生命周期：

| 对象 | 建议生命周期 | 原因 |
| --- | --- | --- |
| `SqlSessionFactoryBuilder` | 创建工厂时临时使用 | 完成配置解析后不再需要 |
| `SqlSessionFactory` | 应用级单例 | 创建成本较高、线程安全，用于持续创建会话 |
| `SqlSession` | 一次工作或事务 | 非线程安全，持有连接、事务状态和一级缓存 |
| Mapper 代理 | 跟随当前 `SqlSession` | 代理内部最终委托给对应会话 |

不要把一个 `SqlSession` 存为 `static` 并被多线程共享。

检查点 C：仅调用 `MyBatisFactory.getSqlSessionFactory()`，确认配置可加载、数据库连接信息正确。此时如果 Mapper XML 尚未创建，先把 `<mappers>` 暂时保留到对应文件建好后再运行。

## 9. 第一个 Mapper XML 查询

### 9.1 Mapper 接口

先在 `TaskMapper` 中声明：

```java
Task findById(Long id);
```

接口不能实例化，但 MyBatis 会使用 JDK 动态代理创建实现对象：

```java
TaskMapper mapper = sqlSession.getMapper(TaskMapper.class);
```

### 9.2 Mapper XML

创建：

```text
src/main/resources/cn/siyes/training/mybatis/mapper/TaskMapper.xml
```

骨架：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="cn.siyes.training.mybatis.mapper.TaskMapper">

    <resultMap id="taskResultMap" type="Task">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="description" column="description"/>
        <result property="status" column="status"/>
        <result property="dueDate" column="due_date"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="findById" parameterType="long" resultMap="taskResultMap">
        SELECT id,
               title,
               description,
               status,
               due_date,
               created_at,
               updated_at
        FROM tasks
        WHERE id = #{id}
    </select>
</mapper>
```

定位规则：

```text
namespace = Mapper 接口全限定名
statement id = Mapper 方法名

cn.siyes.training.mybatis.mapper.TaskMapper.findById
```

这组成 MyBatis 内部 `MappedStatement` 的唯一标识。方法名、参数和返回类型必须与 XML 的语义对应。

### 9.3 调用

```java
try (SqlSession session = MyBatisFactory.getSqlSessionFactory().openSession()) {
    TaskMapper mapper = session.getMapper(TaskMapper.class);
    Task task = mapper.findById(1L);
    System.out.println(task);
}
```

查询不需要 `commit()`。`try-with-resources` 会关闭 `SqlSession`，继而归还连接；它不会替你提交未提交的写操作。

检查点 D：先用 Navicat 插入一条任务，再确认 Java 查询出的字段、枚举和时间正确。

## 10. CRUD 与生成主键

`TaskMapper` 逐步加入：

```java
int insert(Task task);

int update(Task task);

int deleteById(Long id);
```

插入 XML：

```xml
<insert id="insert"
        parameterType="Task"
        useGeneratedKeys="true"
        keyProperty="id"
        keyColumn="id">
    INSERT INTO tasks (title, description, status, due_date)
    VALUES (#{title}, #{description}, #{status}, #{dueDate})
</insert>
```

执行成功后，数据库生成的 ID 会写回同一个 `Task` 对象：

```java
int rows = mapper.insert(task);
System.out.println(rows);        // 1
System.out.println(task.getId()); // 数据库生成的 ID
```

更新建议使用动态 `<set>`：

```xml
<update id="update" parameterType="Task">
    UPDATE tasks
    <set>
        <if test="title != null">title = #{title},</if>
        <if test="description != null">description = #{description},</if>
        <if test="status != null">status = #{status},</if>
        <if test="dueDate != null">due_date = #{dueDate},</if>
    </set>
    WHERE id = #{id}
</update>
```

注意当前写法无法表达“把 description 主动更新成 `NULL`”，因为 `null` 被解释成“不更新”。这是局部更新 DTO 常见的三态问题：未传、传 `null`、传具体值。第一段先记录边界，不需要为它设计复杂包装类型。

写操作需要明确事务：

```java
try (SqlSession session = factory.openSession()) {
    TaskMapper mapper = session.getMapper(TaskMapper.class);
    mapper.insert(task);
    session.commit();
} catch (RuntimeException exception) {
    // try-with-resources 会关闭会话；显式 rollback 在多步事务中更清晰
    throw exception;
}
```

更适合多步事务的形式见第 14 节。

## 11. 参数命名与 `@Param`

单个 JavaBean 参数可以直接写属性名：

```java
int insert(Task task);
```

```xml
VALUES (#{title}, #{status})
```

多个参数建议显式使用 MyBatis 的 `@Param`：

```java
List<Task> findPage(
        @Param("query") TaskQuery query,
        @Param("offset") int offset,
        @Param("size") int size
);
```

XML 中使用：

```xml
#{query.status}
#{offset}
#{size}
```

这里的 `@Param` 只是给 SQL 参数命名，不是用注解写 SQL，因此不会破坏“XML 为主”的方案。

## 12. 动态 SQL、分页和安全排序

`TaskQuery` 建议包含：

```text
keyword
status
page
size
sortBy
direction
```

分页换算：

```java
int offset = (page - 1) * size;
```

分页列表和总数必须复用相同筛选条件。可以定义 SQL 片段：

```xml
<sql id="taskConditions">
    <where>
        <if test="query.status != null">
            AND status = #{query.status}
        </if>
        <if test="query.keyword != null and query.keyword != ''">
            AND (
                title LIKE CONCAT('%', #{query.keyword}, '%')
                OR description LIKE CONCAT('%', #{query.keyword}, '%')
            )
        </if>
    </where>
</sql>
```

列表：

```xml
<select id="findPage" resultMap="taskResultMap">
    SELECT id, title, description, status, due_date, created_at, updated_at
    FROM tasks
    <include refid="taskConditions"/>
    ORDER BY
    <choose>
        <when test="query.sortBy == 'dueDate'">due_date</when>
        <when test="query.sortBy == 'title'">title</when>
        <otherwise>created_at</otherwise>
    </choose>
    <choose>
        <when test="query.direction == 'asc'">ASC</when>
        <otherwise>DESC</otherwise>
    </choose>
    LIMIT #{size} OFFSET #{offset}
</select>
```

总数：

```xml
<select id="count" resultType="long">
    SELECT COUNT(*)
    FROM tasks
    <include refid="taskConditions"/>
</select>
```

### `#{}` 与 `${}`

```text
#{value}
  -> 使用 JDBC 占位符 ?
  -> 值作为 PreparedStatement 参数绑定
  -> 常规用户输入应使用它

${value}
  -> 直接把文本拼进 SQL
  -> 不会产生 ? 占位符
  -> 用户可控时可能造成 SQL 注入
```

列名和 `ASC` / `DESC` 不能作为普通 `?` 参数绑定，所以排序必须使用固定白名单。当前 `<choose>` 让 SQL 结构来自代码中的固定选项，客户端传入恶意字符串时会进入默认分支。

不要这样写：

```xml
ORDER BY ${query.sortBy} ${query.direction}
```

检查点 F：分别请求或调用合法排序与 `sortBy=id desc; DROP TABLE tasks` 之类的恶意文本，确认后者只能进入默认排序，不能改变 SQL 结构。

## 13. 一对多结果映射与批量操作

### 13.1 评论 Mapper

```java
int insertBatch(@Param("comments") List<TaskComment> comments);

List<TaskComment> findByTaskId(Long taskId);
```

批量插入：

```xml
<insert id="insertBatch">
    INSERT INTO task_comments (task_id, content)
    VALUES
    <foreach collection="comments" item="comment" separator=",">
        (#{comment.taskId}, #{comment.content})
    </foreach>
</insert>
```

调用前先拒绝空集合，否则可能生成不完整的 `INSERT ... VALUES`。

### 13.2 任务详情的一对多映射

任务与评论联表时，一条任务会对应多行 ResultSet：

```text
task 1 + comment 1
task 1 + comment 2
task 1 + comment 3
```

MyBatis 的 `<collection>` 把重复的任务列合并为一个 `TaskDetail`，再组装评论集合。

```xml
<resultMap id="taskDetailResultMap" type="TaskDetail">
    <id property="id" column="task_id"/>
    <result property="title" column="task_title"/>
    <result property="description" column="task_description"/>
    <result property="status" column="task_status"/>
    <result property="dueDate" column="task_due_date"/>
    <result property="createdAt" column="task_created_at"/>
    <result property="updatedAt" column="task_updated_at"/>
    <collection property="comments" ofType="TaskComment">
        <id property="id" column="comment_id"/>
        <result property="taskId" column="comment_task_id"/>
        <result property="content" column="comment_content"/>
        <result property="createdAt" column="comment_created_at"/>
    </collection>
</resultMap>

<select id="findDetailById" resultMap="taskDetailResultMap">
    SELECT t.id          AS task_id,
           t.title       AS task_title,
           t.description AS task_description,
           t.status      AS task_status,
           t.due_date    AS task_due_date,
           t.created_at  AS task_created_at,
           t.updated_at  AS task_updated_at,
           c.id          AS comment_id,
           c.task_id     AS comment_task_id,
           c.content     AS comment_content,
           c.created_at  AS comment_created_at
    FROM tasks t
    LEFT JOIN task_comments c ON c.task_id = t.id
    WHERE t.id = #{id}
    ORDER BY c.created_at ASC, c.id ASC
</select>
```

`<id>` 非常重要，它帮助 MyBatis 识别哪些行属于同一个父对象。列别名也避免任务和评论都叫 `id`、`created_at` 时发生覆盖。

不要直接用这种一对多 JOIN 做任务分页：JOIN 后分页针对的是结果行，不一定是任务数量。当前方案先分页查任务列表，按 ID 查单个详情。

检查点 G：为一个任务插入两条评论，确认详情只返回一个任务对象，`comments` 中有两个元素。

## 14. 原生 MyBatis 事务

`factory.openSession()` 默认创建 `autoCommit=false` 的会话。SQL 已经在数据库连接的当前事务中执行，但最终是否提交由 `commit()` / `rollback()` 决定。

练习“新增任务 + 批量新增初始评论”：

```java
try (SqlSession session = factory.openSession()) {
    try {
        TaskMapper taskMapper = session.getMapper(TaskMapper.class);
        TaskCommentMapper commentMapper = session.getMapper(TaskCommentMapper.class);

        taskMapper.insert(task);

        for (TaskComment comment : comments) {
            comment.setTaskId(task.getId());
        }
        commentMapper.insertBatch(comments);

        session.commit();
    } catch (RuntimeException exception) {
        session.rollback();
        throw exception;
    }
}
```

两个 Mapper 由同一个 `SqlSession` 创建，因此共享同一数据库连接和事务。若分别打开两个 `SqlSession`，就不再天然属于同一个事务。

主动制造一次异常：任务插入成功后、评论插入前抛出异常。随后用 Navicat 确认任务也没有留下。恢复临时代码后再继续，不提交故障代码。

检查点 H：

- 正常路径：任务和评论同时存在。
- 异常路径：任务和评论都不存在。
- 能解释为什么同一个 `SqlSession` 是事务成立的重要条件。

## 15. 注解 SQL 对照

只选择一个简单方法，例如：

```java
@Select("SELECT COUNT(*) FROM task_comments WHERE task_id = #{taskId}")
long countByTaskId(@Param("taskId") Long taskId);
```

这个练习用于确认 SQL 来源既可以是 XML，也可以是注解。不要把动态查询和一对多映射全部改成 `<script>` 注解字符串；复杂 SQL 继续放 XML。

同一个 Mapper 方法不能同时由 XML 和注解重复定义，否则会出现 MappedStatement 重复注册问题。

## 16. 一级缓存最小观察

在同一个 `SqlSession` 中连续调用两次相同查询，观察 SQL 日志：

```java
mapper.findById(id);
mapper.findById(id);
```

默认情况下，第二次可能直接使用该会话的一级缓存。随后执行一次更新或调用：

```java
session.clearCache();
```

再查询并观察日志。

要点：

- 一级缓存作用域是 `SqlSession`。
- 不同 `SqlSession` 不共享一级缓存。
- 写操作通常会清空可能失效的本地缓存。
- 第二段 Spring 管理会话后，不能把一级缓存想象成全局业务缓存。

## 17. 常见错误定位

### `Invalid bound statement (not found)`

检查：

```text
Mapper XML 是否进入 src/main/resources
-> mybatis-config.xml 是否注册 XML
-> namespace 是否等于接口全限定名
-> statement id 是否等于方法名
```

### `Parameter 'xxx' not found`

检查 Mapper 是否存在多个参数但没有 `@Param`，以及 XML 使用的名称是否与 `@Param` 一致。

### 返回对象字段为空

检查：

- SQL 是否查询了该列。
- 列名与属性名能否通过驼峰规则匹配。
- `resultMap` 的 `property` 和 `column` 是否写反。
- JOIN 是否存在重名列且没有别名。
- JavaBean 是否有无参构造器和 setter。

### 写操作执行后 Navicat 看不到

先检查是否漏掉 `session.commit()`。关闭 `SqlSession` 不等于提交事务。

### 枚举写库报二进制内容或乱码

确保 `TaskStatus` 作为枚举直接绑定，由 MyBatis 默认枚举类型处理器写入名称；不要把枚举作为 `Object` 或序列化对象传入 JDBC。

## 18. 第一段验收清单

- [ ] 根 POM 管理 MyBatis 相关版本，Module POM 只声明依赖。
- [ ] Navicat 创建 `spring_training_mybatis`、`tasks`、`task_comments`，DDL 保存到 `schema.sql`。
- [ ] 能从环境变量构造 `SqlSessionFactory`。
- [ ] 能说明四类核心对象的生命周期。
- [ ] Mapper XML 的 namespace、id、参数和返回值对应正确。
- [ ] XML CRUD 可用，插入后生成主键能写回对象。
- [ ] 动态筛选、总数、分页和排序白名单可用。
- [ ] 能解释并演示 `#{}` 与 `${}` 的差异。
- [ ] 批量评论插入可用，空集合在 Java 层拦截。
- [ ] 一对多详情能组装一个任务和多个评论。
- [ ] 正常事务提交，主动异常时任务和评论同时回滚。
- [ ] 能观察并解释 `SqlSession` 一级缓存。
- [ ] 用自己的话画出从 Mapper 方法到 JDBC 的完整链路。

完成并验收这些内容后，再进入第二段。不要因为第一条查询成功就直接接入 Spring。
