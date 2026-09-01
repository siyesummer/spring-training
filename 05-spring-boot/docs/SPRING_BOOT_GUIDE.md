# Spring Boot 4 基础实战引导

> 项目：`05-spring-boot`
> 
> 基线：Spring Boot `4.0.8`、Java `21`、Maven、Jar、内嵌 Tomcat
> 
> 本文只覆盖第一轮 `Spring Boot + JdbcTemplate`；第二轮 `Spring Boot + MyBatis` 见 [SPRING_BOOT_MYBATIS_GUIDE.md](SPRING_BOOT_MYBATIS_GUIDE.md)。

## 1. 本阶段要理解什么

前面几个阶段已经分别手动练习了 Servlet、Spring Core、Spring MVC、JdbcTemplate、MyBatis 和事务。本阶段的重点不是再写一套更大的业务，而是观察 Spring Boot 如何把这些基础设施自动装配起来。

本阶段需要能够回答：

- `SpringApplication.run()` 启动时做了哪些事情？
- `@SpringBootApplication` 为什么能同时完成组件扫描和自动配置？
- Starter 依赖与自动配置类是什么关系？
- Boot 自动配置的 `DispatcherServlet`、Jackson、DataSource、JdbcTemplate 和事务管理器，分别替代了前面哪些手动配置？
- `application.yaml`、环境变量和 Profile 的配置优先级如何工作？
- 为什么 Boot 项目可以直接用 `java -jar` 启动，而不需要单独安装并配置外部 Tomcat？
- 后续接入 MyBatis 后，哪些配置由 Boot 接管，哪些 SQL、Mapper 和结果映射仍然由 MyBatis 负责？（第二轮再展开）

本阶段的主线是：

```text
Spring Boot 启动器
  -> 创建 SpringApplication
  -> 加载 Environment 和配置文件
  -> 推断应用类型
  -> 执行自动配置
  -> 组件扫描并创建 Bean
  -> 启动内嵌 Tomcat
  -> DispatcherServlet 接收 HTTP 请求
  -> Controller -> Service -> JdbcTemplate -> MySQL
```

## 2. 当前项目与前面 WAR 项目的区别

你当前创建的是：

```text
Spring Boot 4.0.8
Java 21
Maven Jar
内嵌 Tomcat
```

它和前面 `03-spring-mvc`、`04-mybatis` 的 WAR 运行方式不同：

| 项目 | 传统 WAR | Spring Boot Jar |
| --- | --- | --- |
| Web 容器 | IDEA/Tomcat 外部提供 | Boot 启动内嵌 Tomcat |
| 入口 | `web.xml` / `DispatcherServlet` 配置 | `main()` 调用 `SpringApplication.run()` |
| Servlet API | 通常由 Tomcat 提供，依赖常为 `provided` | 由 Web Starter 带入运行时依赖 |
| 部署 | 复制 WAR 到 Tomcat | 执行 `java -jar xxx.jar` |
| 配置方式 | 手动 Java Config 和 XML 较多 | 条件自动配置 + `application.yaml` |

因此本阶段不要创建 `webapp` 目录，也不要把应用部署到外部 Tomcat 作为主流程。你仍然可以通过此前的 Servlet 和 Spring MVC 知识理解 Boot 内部最终启动的 Web 层。

## 3. 当前生成项目的检查结果

当前生成的 POM 已包含：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.8</version>
</parent>
```

这表示版本管理和 Maven 默认配置由 Spring Boot 父 POM 提供。依赖通常不需要手动写版本号。

当前依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

逐项理解：

- `spring-boot-starter-webmvc`：Web MVC、JSON、内嵌 Servlet 容器及相关基础依赖的组合入口。
- `mysql-connector-j`：Java 运行时连接 MySQL 的 JDBC 驱动。`runtime` 表示编译主代码通常不直接引用驱动类，但运行时必须存在。
- `spring-boot-starter-webmvc-test`：Boot 4 对 Web MVC 测试支持的 Starter，当前测试类中的 `@SpringBootTest` 会使用它提供的测试依赖。
- `spring-boot-maven-plugin`：将项目打包成可执行 Jar，并生成 Boot Loader 所需结构。

本阶段还需要你手动加入第一轮依赖：

```xml
<!-- JdbcTemplate、事务基础设施和 DataSource 自动配置所需的 Spring JDBC 能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- Jakarta Bean Validation 与 Spring MVC 参数校验集成 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- 健康检查和基础运行状态端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

依赖选择原则：版本由 Boot `4.0.8` 的依赖管理统一决定，不要分别给 Spring、Jackson、HikariCP、Tomcat 或 Validation 写版本号。新增依赖后先在 IDEA Maven 面板 Reload，再检查依赖树是否出现冲突。

## 4. 第一轮：Spring Boot + JdbcTemplate

### 4.1 实战范围

继续使用任务管理主题，但第一轮只关注 Boot 自动配置和 JDBC 数据访问：

- `/health`：返回应用运行状态。
- `POST /api/tasks`：创建任务。
- `GET /api/tasks/{id}`：按 ID 查询任务。
- `GET /api/tasks`：条件分页查询任务。
- `PUT /api/tasks/{id}`：修改任务。
- `PATCH /api/tasks/{id}/status`：修改状态。
- `DELETE /api/tasks/{id}`：删除任务。

第一轮暂不加入评论一对多查询和 MyBatis。先确认 Boot 自动配置的 `DataSource`、`JdbcTemplate`、MVC 和配置绑定已经工作。

### 4.2 建议包结构

先把生成的包名从 `_5springboot` 调整为更适合长期维护的：

```text
cn.siyes.training.boot
├─ Application.java
├─ controller
├─ service
├─ repository
├─ model
├─ dto
├─ config
└─ exception
```

`Application` 放在 `cn.siyes.training.boot` 根包，其他类放在其子包。`@SpringBootApplication` 默认从启动类所在包向下扫描；启动类放错位置会导致 Controller 或 Service 未被发现。

职责保持清晰：

- `Application`：启动入口，不堆业务代码。
- `controller`：HTTP 路由、参数接收、响应状态和 DTO。
- `service`：业务规则、事务边界和跨 Repository 操作。
- `repository`：调用 `JdbcTemplate` 执行 SQL，负责数据库读写。
- `model`：数据库或领域对象。
- `dto`：请求和响应数据模型。
- `config`：只放确有必要的手动配置，用来观察和补充自动配置，不要重新复制前面 WAR 项目的全部配置。

### 4.3 数据库操作

数据库操作默认使用 Navicat。你需要在 Navicat 查询窗口中：

1. 创建本阶段独立数据库，例如 `spring_training_boot`。
2. 创建 `tasks` 表，字段沿用 `04-mybatis` 的任务模型。
3. 插入少量初始数据，方便查询、更新和删除验证。
4. 将最终确认过的 DDL 同步保存到：

```text
05-spring-boot/src/main/resources/schema.sql
```

不要把真实密码写入 `application.yaml` 或提交到 Git。Java 应用的连接参数使用运行配置环境变量：

```text
BOOT_DB_URL=jdbc:mysql://localhost:3306/spring_training_boot?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
BOOT_DB_USERNAME=你的数据库用户名
BOOT_DB_PASSWORD=你的数据库密码
```

Navicat 保存的连接信息不会自动传给 IDEA 启动的 Java 进程，这两套连接配置需要分别维护。

### 4.4 `application.yaml` 配置

你需要手动补充类似配置：

```yaml
spring:
  application:
    name: 05-spring-boot
  datasource:
    url: ${BOOT_DB_URL}
    username: ${BOOT_DB_USERNAME}
    password: ${BOOT_DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

server:
  port: 8085

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

配置链路是：

```text
环境变量 BOOT_DB_*
  -> Spring Environment
  -> application.yaml 中的 ${...}
  -> DataSourceProperties
  -> 自动创建 DataSource
  -> 自动创建 JdbcTemplate
```

这里不需要自己 `new HikariConfig` 或声明 `DataSource`、`JdbcTemplate` Bean。`spring-boot-starter-jdbc` 加入后，Boot 会根据 `spring.datasource.*` 创建这些基础设施；你要做的是提供正确的连接属性并观察结果。

### 4.5 启动类和自动配置

生成的启动类：

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@SpringBootApplication` 可以拆成三个核心能力：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

- `@SpringBootConfiguration`：表明这是 Boot 应用的主要配置类。
- `@EnableAutoConfiguration`：根据 classpath 中的依赖、配置属性和条件注解选择自动配置。
- `@ComponentScan`：扫描启动类所在包及其子包中的 `@Controller`、`@Service`、`@Repository`、`@Component` 等 Bean。

自动配置不是无条件地创建所有 Bean。典型判断条件包括：

- classpath 中是否存在某个类。
- 容器中是否还没有用户自己的同类型 Bean。
- 是否存在对应配置属性。
- 当前应用是否是 Web 应用。

因此，“自己声明一个 Bean”往往会使 Boot 的 `@ConditionalOnMissingBean` 退让给你的实现。这是 Boot 可覆盖设计的关键。

### 4.6 第一轮手写顺序

按下面顺序逐步完成，每一步先启动确认：

1. 在现有 POM 中加入 `spring-boot-starter-jdbc`、Validation 和 Actuator。
2. 创建 `application.yaml` 的数据源、端口和 Actuator 配置。
3. 配置 IDEA 的 Application Run Configuration 环境变量。
4. 启动应用，访问 `/actuator/health`，确认返回 `UP`。
5. 创建 `HealthController`，访问自定义 `/health`，区分 Actuator 健康检查与业务 Controller。
6. 手写 `Task`、请求 DTO、响应 DTO 和 `TaskRepository`。
7. 构造器注入 `JdbcTemplate`，完成最小 `findById` 查询。
8. 再补创建、更新、状态修改、删除和分页。
9. 在 Service 层标记明确的 `@Transactional` 方法，完成至少一个多 SQL 写操作。
10. 使用 Apifox 发请求，用 Navicat 对照数据，观察 Boot 自动配置后的完整链路。

### 4.7 第一轮重点观察

不要只观察接口是否返回成功，还要回答：

- 没有手动声明 `JdbcTemplate`，它是从哪里来的？
- 没有手动注册 `DispatcherServlet`，它是谁创建的？
- 没有手动配置 Jackson，为什么 DTO 可以转 JSON？
- 没有外部 Tomcat，8085 端口由谁监听？
- 数据库连接失败时，失败发生在启动阶段还是第一次访问数据库时？为什么？
- `spring.datasource.*` 配置改变后，自动创建的 DataSource 如何受到影响？

可以使用启动日志辅助观察，也可以在一个临时 Controller 中注入并打印 Bean 类型：

```java
private final JdbcTemplate jdbcTemplate;

public ProbeController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
}
```

重点是理解“注入成功说明自动配置已生效”，不是长期保留调试 Controller。

## 5. 配置来源与 Profile 练习

至少准备两个配置文件：

```text
application.yaml
application-dev.yaml
```

在 `application.yaml` 中指定默认 Profile：

```yaml
spring:
  profiles:
    active: dev
```

在 `application-dev.yaml` 中配置本地端口或日志级别。然后用 IDEA 环境变量覆盖一个配置项，观察优先级变化。

建议记录这个结论：

```text
命令行参数 / 运行参数
  > 环境变量
  > 激活 Profile 的配置文件
  > application.yaml
  > 代码中的默认值
```

具体优先级还会受到配置来源类型和 Boot 版本影响，复盘时以实际启动结果为准。不要把密码写进任何配置文件；即使是本地训练，也使用环境变量。

## 6. Actuator 与自定义健康接口

Actuator 的：


```http
GET /actuator/health
```

是运行状态端点，由 Boot/Actuator 提供；你自己写的：

```http
GET /health
```

是普通 Controller 接口。两者不要混为一谈：

- Actuator 用于应用和基础设施运行状态。
- Controller 用于业务 API。
- 生产环境通常只暴露必要的 Actuator 端点，并限制访问范围。

## 7. 打包与运行

本阶段重点是理解 Boot Jar 的结构和启动方式：

```powershell
mvn clean package
java -jar target\05-spring-boot-0.0.1-SNAPSHOT.jar
```

`spring-boot-maven-plugin` 会把应用类、依赖 Jar 和 Boot Loader 组织到可执行 Jar 中。它与普通 Maven Jar 的区别是：普通 Jar 只包含自己的 class，Boot Jar 还包含运行依赖并指定启动入口。

验证：

1. 删除或清理 `target` 后重新执行 `mvn package`。
2. 使用 `java -jar` 启动，而不是 IDEA 专属的运行按钮。
3. 访问 `/actuator/health` 和 `/health`。
4. 观察启动日志中的端口、Profile、DataSource 和 Web 容器信息。

测试命令只作为辅助：

```powershell
mvn test
```

当前阶段重点是理解启动、自动配置和请求链，不需要为了形式编写大量测试矩阵。

## 8. 第一轮最小验收

完成以下核心验证后，第一轮才算结束；不要在未完成这些检查前进入第二轮：

- 应用能通过 `SpringApplication.run()` 启动。
- 不配置外部 Tomcat，内嵌 Tomcat 能监听指定端口。
- `/actuator/health` 返回 `UP`。
- 自定义 `/health` 返回预期 JSON。
- `JdbcTemplate` 能够通过构造器注入并查询 MySQL。
- 任务 CRUD 至少完成创建、按 ID 查询、更新和删除。
- DTO 校验失败返回 `400`，业务不存在返回 `404`。
- Navicat 中的数据与 HTTP 请求结果一致。
- 能解释 `spring-boot-starter-jdbc` 如何间接带来 DataSource、JdbcTemplate 和事务基础设施。
- 能解释 `@SpringBootApplication`、组件扫描、自动配置和内嵌 Tomcat 的关系。
- `mvn package` 成功生成可执行 Jar，并能用 `java -jar` 启动。

## 9. 与 `linux-server` 的对照

完成第一轮后再阅读 `linux-server`：

- 启动类上的 `@SpringBootApplication` 与当前项目有什么相同点？
- `application.yml` 如何绑定到配置对象？
- DataSource、JdbcTemplate、事务和 Controller 分别由哪个 Starter 或自动配置提供？
- `linux-server` 的异常处理、校验和统一响应与你在 Spring MVC 阶段的实现有什么变化？
- 哪些代码属于 Boot 自动配置，哪些仍是业务代码？

对照目标是把“自动配置后的结果”映射回你已经手动写过的 Spring MVC、JdbcTemplate、MyBatis 和事务代码，不把 AI 生成的 `linux-server` 当作独立实现证据。

## 10. 复盘问题

1. Spring Boot 与 Spring Framework 的关系是什么？
2. `@SpringBootApplication` 由哪几个核心注解组成？
3. Starter、自动配置类和条件注解之间是什么关系？
4. 为什么没有手动注册 `DispatcherServlet`，应用仍能接收请求？
5. 为什么没有手动声明 `JdbcTemplate`，仍然可以构造器注入？
6. `application.yaml`、环境变量和 Profile 如何共同决定最终配置？
7. 为什么本项目使用 Jar 和内嵌 Tomcat，而前面使用 WAR 和外部 Tomcat？
8. `java -jar` 启动的 Jar 与普通 Maven Jar 有什么区别？
9. 如果自己声明一个 DataSource 或 JdbcTemplate Bean，自动配置会发生什么变化？
10. `@Transactional` 的代理边界与前面 Spring Core 练习中的自调用问题有什么关系？
11. 读取 `linux-server` 时，怎样判断一段代码是 Boot 自动配置结果，还是项目自己的业务配置？

## 11. 当前边界

本阶段暂不追求：

- 直接研究 Boot 自动配置源码的所有细节。
- 一开始就引入 Security、Redis、消息队列或微服务组件。
- 为每个接口编写完整自动化测试矩阵。
- 直接复制 Boot 2.4 视频中的 `javax.*` 依赖或旧版配置。

重点是用 Boot `4.0.8` 跑通第一轮最小服务，并把自动配置结果与前面手动配置过的 Spring MVC、JdbcTemplate 和事务机制对应起来。第一轮验收后，再单独编写第二轮 MyBatis 引导，并在整个阶段结束后整理 Boot 2.4 与 Boot 4 的迁移差异。
