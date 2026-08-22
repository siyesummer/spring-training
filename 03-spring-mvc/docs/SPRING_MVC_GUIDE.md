# Module 03：Spring MVC 详细实战引导

> 当前状态：`已完成`（2026-08-22）
> 业务主题：任务管理 REST API
> 运行方式：Maven WAR + Tomcat 10.1
> 配置方式：`web.xml` 显式注册 `DispatcherServlet`，Java 配置类启用 Spring MVC
> 编码原则：你手动完成依赖、配置和源码；每完成一个检查点先启动或发送请求确认，再进入下一步。

## 1. 这次真正要练什么

你已经分别练过原生 Java Web 和 Spring Core：

```text
Java Web：Tomcat -> Filter -> Servlet -> Service -> DAO
Spring Core：容器 -> BeanDefinition -> Bean -> 依赖注入 -> 代理
```

Spring MVC 阶段要把两条线接起来：

```text
Tomcat
  -> DispatcherServlet（Servlet 规范中的 Servlet）
  -> 找到 Spring 容器中的 Controller Bean
  -> 把 HTTP 数据转换成 Controller 方法参数
  -> 调用 Controller、Service、Repository Bean
  -> 把 Java 返回值转换成 HTTP 响应
```

本阶段不要只记 `@GetMapping`。每写一个功能，都要知道下面三个问题：

1. 这段信息来自 HTTP 请求的哪个位置？
2. Spring MVC 的哪个组件把它交给 Controller？
3. Controller 返回后，哪个组件把结果写进 HTTP 响应？

## 2. 总体实施顺序

严格按下面顺序推进：

| 阶段 | 你要完成的内容 | 主要观察对象 |
| --- | --- | --- |
| 0 | Maven 依赖、WAR、MySQL 与 Tomcat Artifact | 依赖作用域、连接池和运行进程环境变量 |
| 1 | `web.xml` 和 `WebMvcConfig` | `DispatcherServlet`、WebApplicationContext |
| 2 | `/api/health` | HandlerMapping、HandlerAdapter、Controller |
| 3 | 路由与四种参数来源 | `@PathVariable`、`@RequestParam`、`@RequestBody`、`@ModelAttribute` |
| 4 | Task DTO、Service、JDBC Repository | Spring Bean 分层和真实数据库数据流 |
| 5 | MySQL CRUD、筛选和分页 | REST 路由、状态码、返回值处理、参数化 SQL |
| 6 | Bean Validation | 数据绑定、类型转换、校验边界 |
| 7 | 全局异常处理 | 异常到 HTTP 响应的转换 |
| 8 | Interceptor 和 CORS | MVC 扩展点与 Servlet Filter 的边界 |
| 9 | 机制观察和最终验收 | 一次请求的完整调用链 |

本轮直接使用 MySQL，但继续使用已经练习过的 `JdbcTemplate` 和事务，不在 Spring MVC 阶段提前学习 MyBatis。数据库是为了形成真实请求链，不作为本阶段重复考核 JDBC 语法的重点。

## 3. 第 0 步：手动配置 Maven

### 3.1 版本放在哪一层

`spring-training` 已有多个 Spring Module。Spring Framework 版本会被 `02-spring-core`、`03-spring-mvc` 以及后续 Module 共同使用，因此更合理的长期方案是把 Spring 版本放到根 POM，并在根 POM 的 `dependencyManagement` 中导入 Spring Framework BOM，而不是在每个 Module 重复指定版本。

先在根 `pom.xml` 的 `<properties>` 中补充：

```xml
<spring.version>6.2.8</spring.version>
```

然后在根 POM 已有的 `<dependencyManagement><dependencies>` 中补充 BOM；不要另外创建第二个 `<dependencyManagement>`：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-framework-bom</artifactId>
            <version>${spring.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 根 POM 已有的 mysql-connector-j、junit-jupiter 等继续保留 -->
    </dependencies>
</dependencyManagement>
```

BOM 是一组彼此兼容的依赖版本清单，不会因为导入它就自动给 Module 添加 `spring-webmvc`。子 Module 仍然必须声明自己真正使用的依赖，只是可以省略版本。

`02-spring-core` 当前也声明了同名且同值的属性，并在每个 Spring 依赖上显式写了版本，不会立即导致冲突。更整洁的做法是在本 Module 跑通后，手动删除 `02-spring-core/pom.xml` 中重复的 `<spring.version>` 和 Spring 依赖上的 `<version>`，让它们统一受根 BOM 管理；这不是启动 `03-spring-mvc` 的阻塞项。

本阶段独有的 Jackson、Validation 实现版本先保留在 Module POM。等后续 Module 确实复用时，再提升到根 POM 或改用统一 BOM，避免根 POM 提前堆入尚未使用的版本。

### 3.2 Module POM 完整配置

手动把 `03-spring-mvc/pom.xml` 补充成下面的结构。不要直接覆盖前先核对 `parent`、`artifactId` 和 `packaging`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>cn.siyes.training</groupId>
        <artifactId>spring-training</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>03-spring-mvc</artifactId>
    <packaging>war</packaging>

    <properties>
        <jackson.version>2.17.2</jackson.version>
        <jakarta.validation.version>3.0.2</jakarta.validation.version>
        <hibernate.validator.version>8.0.1.Final</hibernate.validator.version>
        <jakarta.el.version>4.0.2</jakarta.el.version>
        <hikaricp.version>5.1.0</hikaricp.version>
        <slf4j.version>1.7.36</slf4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>

        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>${jackson.version}</version>
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

        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>${hikaricp.version}</version>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>${slf4j.version}</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>03-spring-mvc</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖用途：

| 依赖 | 用途 | 作用域说明 |
| --- | --- | --- |
| `spring-webmvc` | `DispatcherServlet`、Controller、HandlerMapping、参数和返回值处理 | 默认 `compile`，版本由根 POM 的 Spring BOM 管理，进入 WAR |
| `spring-jdbc` | `JdbcTemplate`、JDBC 异常转换和事务管理器 | 版本由根 POM 的 Spring BOM 管理，进入 WAR |
| `jakarta.servlet-api` | 编译 Servlet、Request、Response 等接口 | `provided`，运行时由 Tomcat 10.1 提供 |
| `jackson-databind` | JSON 与 Java 对象互转 | 进入 WAR，由 Spring MVC 的消息转换器调用 |
| `jackson-datatype-jsr310` | `LocalDate`、`LocalDateTime` 等 Java 时间类型 | 与 Jackson 保持相同版本 |
| `jakarta.validation-api` | `@NotBlank`、`@Size`、`@Valid` 等规范 API | 只有接口和注解 |
| `hibernate-validator` | 实际执行 Bean Validation 约束 | Validation 规范的实现 |
| `jakarta.el` | Hibernate Validator 默认消息插值使用的表达式语言实现 | 传统 Spring MVC 没有 Boot Starter 自动补齐，需要显式声明 |
| `HikariCP` | 复用数据库物理连接，向并发请求提供连接池 | Web 应用使用，不继续把 `DriverManagerDataSource` 当连接池 |
| `mysql-connector-j` | MySQL JDBC 驱动实现 | 版本由根 POM 管理，`runtime` 并进入 WAR |
| `slf4j-simple` | 显示 HikariCP 的基础连接池日志 | 仅用于当前训练的简单日志后端，后续统一日志阶段替换 |
| `junit-jupiter` | 后续必要的最小测试 | `test`，不进入 WAR |

关键关系：

```text
@RequestBody
  -> Spring MVC 选择 MappingJackson2HttpMessageConverter
  -> 转交 Jackson 反序列化

@Valid
  -> Spring MVC 在参数解析后触发校验
  -> Hibernate Validator 执行 Jakarta Validation 约束
```

`spring-webmvc` 会传递引入 `spring-context`、`spring-web`、`spring-core` 等依赖，所以不需要把它们全部重复声明。Maven Reload 后，在 IDEA External Libraries 中确认这些依赖已经出现。

### 3.3 第一处检查点

此时只检查：

- 根 POM 包含 `<module>03-spring-mvc</module>`。
- Module 的 `<packaging>` 是 `war`。
- `jakarta.servlet-api` 是 `provided`。
- `mysql-connector-j` 是 `runtime`，会进入 WAR；HikariCP 是连接池，不是 JDBC 驱动。
- Spring Framework 使用 `6.2.8`，由根 POM 的 BOM 统一管理并与上一 Module 一致。
- Servlet、Validation 等 Jakarta EE API 使用 `jakarta.*`，不能混入视频中旧的 `javax.servlet.*`、`javax.validation.*`。

注意 `javax.sql.DataSource` 是 Java SE/JDBC 中仍然保留的标准接口，Spring 6 项目继续使用这个包名是正确的；Jakarta 迁移不等于所有 `javax.*` 包都改名。

不需要现在运行测试。Maven Reload 成功后，继续在 Navicat 创建数据库。

### 3.4 在 Navicat 创建数据库和表

项目已经保存完整 DDL：

```text
src/main/resources/db/schema.sql
```

由你在 Navicat 中手动执行：

1. 使用能够创建数据库的 MySQL 连接打开 Navicat。
2. 新建查询。
3. 打开或复制项目中的 `schema.sql`。
4. 执行全部 SQL。
5. 刷新数据库列表，确认 `spring_training_mvc.tasks` 存在。
6. 打开表设计，确认字符集、字段、约束和索引。

表的关键设计：

| 字段 | 设计原因 |
| --- | --- |
| `id BIGINT UNSIGNED AUTO_INCREMENT` | 数据库生成主键，Java 使用 `Long` |
| `title VARCHAR(100) NOT NULL` | 数据库兜底保证标题存在；更具体的空白校验由 Bean Validation 完成 |
| `status VARCHAR(20)` | 与 Java `TaskStatus` 枚举名称对应 |
| `due_date DATE` | 只表达日期，对应 `LocalDate` |
| `created_at/updated_at DATETIME(3)` | 保存毫秒级时间，对应 `LocalDateTime` |
| `CHECK status ...` | 防止绕过 Java 代码写入未知状态 |
| 状态和日期索引 | 为筛选、排序建立最小索引意识；不在本阶段做复杂调优 |

你之前创建的 MySQL 账号可以继续使用，前提是它对新数据库有权限。不要仅凭 Navicat 能连接就判断应用账号有权限；在使用该账号的查询窗口执行：

```sql
SHOW GRANTS FOR CURRENT_USER;
```

如果结果不包含 `spring_training_mvc` 的权限，使用管理员连接执行授权。把下面的账号名和来源主机替换成你的实际账号，不要把密码写入项目：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE
ON spring_training_mvc.*
TO '你的应用账号'@'localhost';

FLUSH PRIVILEGES;
```

创建数据库和表需要管理员或 DDL 权限；Java 应用运行时只需要任务表的增删改查权限。训练环境可以临时复用已有账号，但要理解最小权限原则。

### 3.5 配置 Tomcat 运行时环境变量

数据库参数属于运行 Tomcat 的 JVM，不属于 Navicat。打开：

```text
Run -> Edit Configurations
  -> Tomcat Server -> Local
  -> Environment variables
```

添加：

```text
MVC_DB_URL=jdbc:mysql://localhost:3306/spring_training_mvc?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MVC_DB_USERNAME=你的应用账号
MVC_DB_PASSWORD=你的数据库密码
```

注意：

- 环境变量输入框中不要把 JDBC URL 的 `=` 写成 `\=`；那是之前连接字符串解析失败的原因。
- Navicat 保存的密码不会自动传给 Tomcat。
- 配置后必须完全停止并重启 Tomcat，新 JVM 才能读取变量。
- 不要在日志、POM、源码或 `schema.sql` 中打印和提交密码。

## 4. 第 1 步：手动启动 Spring MVC 容器

### 4.1 创建 `WEB-INF/web.xml`

手动创建：

```text
src/main/webapp/WEB-INF/web.xml
```

内容使用 Jakarta Servlet 6 描述符：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
         https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <display-name>03-spring-mvc</display-name>

    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>

        <init-param>
            <param-name>contextClass</param-name>
            <param-value>org.springframework.web.context.support.AnnotationConfigWebApplicationContext</param-value>
        </init-param>

        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>cn.siyes.training.mvc.config.WebMvcConfig</param-value>
        </init-param>

        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

逐项理解：

- `DispatcherServlet` 本身实现了 Servlet API，由 Tomcat 创建并调用。
- `load-on-startup=1` 表示应用部署时初始化，不等第一次请求才创建。
- `contextClass` 指定使用注解配置型 WebApplicationContext。
- `contextConfigLocation` 指向下一步要写的 Java 配置类。
- `/` 表示请求先交给 `DispatcherServlet`；它随后再根据 Controller 映射做二次路由。

这里不是把 Controller 转换成 Servlet。Tomcat 只直接调用 `DispatcherServlet`，后者再通过 Spring MVC 组件调用 Controller Bean。

### 4.2 创建 MVC 配置类

创建：

```text
src/main/java/cn/siyes/training/mvc/config/WebMvcConfig.java
```

先写最小版本：

```java
package cn.siyes.training.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan("cn.siyes.training.mvc")
public class WebMvcConfig {
}
```

三个注解职责不同：

| 注解 | 职责 |
| --- | --- |
| `@Configuration` | 该类是 Spring Bean 定义来源 |
| `@ComponentScan` | 扫描 Controller、Service、Repository 等业务 Bean |
| `@EnableWebMvc` | 注册注解路由、参数解析、返回值处理、消息转换等 MVC 基础设施 |

`@ComponentScan` 本身不会提供 MVC 路由能力；`@EnableWebMvc` 也不会替你创建业务 Controller。两者不能互相代替。

### 4.3 创建数据库配置类

不要把 DataSource 配置塞进 Controller，也不要把数据库密码写进 Java。创建：

```text
src/main/java/cn/siyes/training/mvc/config/DatabaseConfig.java
```

```java
package cn.siyes.training.mvc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environment.getRequiredProperty("MVC_DB_URL"));
        config.setUsername(environment.getRequiredProperty("MVC_DB_USERNAME"));
        config.setPassword(environment.getRequiredProperty("MVC_DB_PASSWORD"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("spring-mvc-training-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5_000);
        config.setInitializationFailTimeout(5_000);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

这里形成的 Bean 关系是：

```text
Spring Environment
  -> HikariDataSource Bean
  -> JdbcTemplate Bean
  -> JdbcTaskRepository Bean

HikariDataSource Bean
  -> DataSourceTransactionManager Bean
  -> @Transactional 事务拦截器
```

重点说明：

- `Environment` 能读取操作系统环境变量；`getRequiredProperty` 缺少配置时会让应用启动失败，避免带着 `null` 配置继续运行。
- `HikariDataSource` 实现 `DataSource`，负责连接池；`JdbcTemplate` 仍然只依赖标准 `DataSource` 接口。
- `maximumPoolSize=10` 只是本地训练的固定值，不是生产环境通用答案。
- Spring 容器销毁时会调用 `close()` 关闭连接池，不是关闭某一个业务请求借出的 Connection。
- `DataSourceTransactionManager` 与上一阶段相同；本轮写操作需要多条 SQL 时可以继续通过 `@Transactional` 保持边界。

为什么不继续使用 `DriverManagerDataSource`：

```text
DriverManagerDataSource.getConnection()
  -> 每次建立新的物理数据库连接

HikariDataSource.getConnection()
  -> 从连接池借出连接代理
  -> 使用后 close() 实际归还连接池
```

Web 应用会反复处理请求，建立物理连接的成本不应在每次 Repository 调用中重复支付。

### 4.4 为什么这次先只有一个容器

当前结构是：

```text
DispatcherServlet
  -> 创建一个 WebApplicationContext
  -> 扫描 Controller、Service、Repository
```

传统大型 Spring MVC 项目还可能采用：

```text
ContextLoaderListener
  -> Root WebApplicationContext（Service、Repository）

DispatcherServlet
  -> Child WebApplicationContext（Controller、MVC 配置）
```

子容器可以读取父容器 Bean，父容器不能读取子容器 Bean。这种层级有利于多个 Servlet 或复杂 Web 应用分隔职责，但对当前单个 REST API 会增加扫描范围和 Bean 重复注册问题。因此本练习先用一个容器，等请求链跑通后再考虑是否进行父子容器实验。

父子容器的创建者、Bean 查找方向、扫描隔离原则，以及基于当前项目的 `RootConfig + WebMvcConfig + web.xml` 完整配置见：[SPRING_MVC_CONTEXT_HIERARCHY.md](SPRING_MVC_CONTEXT_HIERARCHY.md)。

## 5. 第 2 步：最小 Controller 与第一次请求

创建：

```text
src/main/java/cn/siyes/training/mvc/controller/HealthController.java
```

先手写：

```java
package cn.siyes.training.mvc.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
```

理解：

- `@RestController` 让类成为 Spring Bean，并让方法返回值默认写入响应体。
- 类上的 `/api` 与方法上的 `/health` 合并为 `/api/health`。
- `Map` 不是 JSON。它是 Java 对象，最后由 Jackson 转换为 JSON。

### 5.1 在 IDEA 中部署

沿用上一阶段的 Tomcat 10.1：

1. `Run -> Edit Configurations -> Tomcat Server -> Local`。
2. 在 `Deployment` 中添加 `03-spring-mvc:war exploded`。
3. 把 Application context 明确设为 `/03_spring_mvc`。
4. 不要让另一个 Artifact 使用相同 Context Path。
5. 启动 Tomcat，先观察 `dispatcher` 初始化是否成功。

访问：

```text
GET http://localhost:8080/03_spring_mvc/api/health
```

预期：

```http
HTTP/1.1 200
Content-Type: application/json

{"status":"UP"}
```

如果失败，先按状态分类：

- Tomcat 启动失败：检查 Artifact、端口和部署日志。
- 应用上下文路径 404：检查 Deployment 中的 Application context。
- `/api/health` 404：检查 `DispatcherServlet` 映射、组件扫描和 Controller 路由。
- 500：查看 Spring 容器启动异常，常见原因是依赖缺失或配置类名不一致。

### 5.2 检查点：你应该能解释

```text
Tomcat 为什么认识 DispatcherServlet？
  -> web.xml 注册了一个 Servlet。

DispatcherServlet 为什么认识 HealthController？
  -> WebMvcConfig 的组件扫描把它注册为 Spring Bean，HandlerMapping 读取路由注解。

Map 为什么变成 JSON？
  -> 返回值处理器把它识别为响应体，HttpMessageConverter 调用 Jackson 序列化。
```

没有跑通这一步，不进入任务 CRUD。

## 6. 第 3 步：先练四种参数来源

在正式 CRUD 前，建议先用临时方法分别验证四种来源。理解后可以删除这些临时路由。

### 6.1 `@PathVariable`：路径变量

```java
@GetMapping("/tasks/{id}")
public Object getById(@PathVariable Long id) {
    // TODO：先返回 id，之后改为调用 Service
}
```

对应：

```http
GET /api/tasks/10
```

`10` 是资源标识，属于 URL 路径。Spring MVC 先匹配 `{id}`，再把字符串转换为 `Long`。

### 6.2 `@RequestParam`：独立查询参数

```java
@GetMapping("/tasks/search-demo")
public Object search(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String keyword
) {
    // TODO：先返回解析结果
}
```

对应：

```http
GET /api/tasks/search-demo?page=2&size=5&keyword=spring
```

适合少量、相互独立的查询参数。

### 6.3 `@ModelAttribute`：查询参数绑定对象

当筛选参数增加时，创建 `TaskQuery`：

```java
public class TaskQuery {
    private int page = 1;
    private int size = 10;
    private String keyword;
    private TaskStatus status;

    // getter / setter
}
```

Controller 写法：

```java
@GetMapping("/tasks")
public Object list(@ModelAttribute TaskQuery query) {
    // TODO：Spring MVC 已把 query string 绑定到 query 对象
}
```

对应请求仍然是：

```http
GET /api/tasks?page=1&size=10&status=TODO&keyword=spring
```

`@ModelAttribute` 默认处理查询参数或表单字段，不读取 JSON 请求体。它要求普通 JavaBean 具备可写属性，通常就是无参构造器和 setter。

### 6.4 `@RequestBody`：请求体

```java
@PostMapping("/tasks")
public Object create(@RequestBody CreateTaskRequest request) {
    // TODO：request 已经是 Jackson 反序列化后的 Java 对象
}
```

对应：

```http
POST /api/tasks
Content-Type: application/json

{
  "title": "学习 Spring MVC",
  "description": "理解 DispatcherServlet",
  "dueDate": "2026-08-31"
}
```

前端类比：`@PathVariable` 类似路由参数，`@RequestParam` 类似 `location.search`，`@RequestBody` 类似前端请求库的 `data/body`。`@ModelAttribute` 则是后端把一组查询或表单字段批量组装成对象。

不要在同一个参数上同时写 `@RequestBody` 和 `@ModelAttribute`，它们的数据来源和解析器不同。

## 7. 第 4 步：设计任务业务和分层

### 7.1 建议包结构

手动创建：

```text
cn.siyes.training.mvc
├─ config/
│  ├─ WebMvcConfig.java
│  └─ DatabaseConfig.java
├─ controller/
│  ├─ HealthController.java
│  └─ TaskController.java
├─ dto/
│  ├─ CreateTaskRequest.java
│  ├─ UpdateTaskRequest.java
│  ├─ UpdateTaskStatusRequest.java
│  ├─ TaskQuery.java
│  ├─ TaskResponse.java
│  ├─ PageResponse.java
│  └─ ErrorResponse.java
├─ exception/
│  ├─ TaskNotFoundException.java
│  └─ GlobalExceptionHandler.java
├─ interceptor/
│  └─ RequestTraceInterceptor.java
├─ model/
│  ├─ Task.java
│  └─ TaskStatus.java
├─ repository/
│  ├─ TaskRepository.java
│  └─ JdbcTaskRepository.java
└─ service/
   └─ TaskService.java
```

### 7.2 每层职责

| 层 | 本阶段职责 | 不应该承担 |
| --- | --- | --- |
| Controller | HTTP 路由、接收参数、选择状态码、调用 Service | 保存集合、实现业务规则 |
| DTO | 表达请求和响应的数据契约 | 直接充当持久化实现 |
| Service | 业务流程、查找不存在等业务判断、模型与 DTO 组织 | 操作 `HttpServletResponse` |
| Repository | 保存、查询、删除 Task | 决定 HTTP 状态码 |
| ExceptionHandler | 把异常转换为 HTTP 错误响应 | 实现任务业务 |

与你的前端经验对应：Controller 类似服务端路由处理器，但它不应变成包含全部状态和业务逻辑的“大组件”；Service 类似可复用业务层；Repository 是数据访问边界。本轮 Repository 使用 `JdbcTemplate`，下一阶段在尽量不改变 Controller 和 Service 的前提下替换为 MyBatis Mapper。

### 7.3 领域模型

`TaskStatus` 建议先定义三个状态：

```java
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
```

`Task` 至少包含：

```text
id          Long
title       String
description String
status      TaskStatus
dueDate     LocalDate
createdAt   LocalDateTime
updatedAt   LocalDateTime
```

先使用普通 Java 类并自己写构造器、getter、setter，不引入 Lombok。这样可以明确 Jackson 数据绑定和 JavaBean 属性从哪里来。

设计边界：

- `id`、`createdAt`、`updatedAt` 由数据库生成，`status` 由 Service 设为默认值；创建请求不允许直接指定这些字段。
- 创建时默认状态为 `TODO`。
- 修改时 `updatedAt` 由 MySQL 列定义自动更新。
- 是否允许 `DONE -> TODO` 属于业务规则；本轮可以允许，重点是 MVC 链路。

### 7.4 请求 DTO 与响应 DTO

不要让 Controller 直接接收和返回同一个 `Task`。建议：

```text
CreateTaskRequest      创建时允许前端提交的字段
UpdateTaskRequest      修改基本信息的字段
UpdateTaskStatusRequest 单独修改状态
TaskQuery              查询字符串绑定对象
TaskResponse           返回给前端的数据
```

这样能看出 HTTP 数据契约与内部模型的边界。它和 TypeScript 中分别声明 `CreateTaskPayload`、`TaskQuery`、`TaskVO` 的思路一致。

普通 DTO 至少要有：

- 无参构造器。
- getter 和 setter。
- 字段类型与 JSON 格式对应。

`LocalDate` 的 JSON 建议使用 ISO 格式：

```json
"2026-08-31"
```

如果 Java 时间类型不能转换，先确认 `jackson-datatype-jsr310` 已加入，不要把日期退回成没有类型约束的任意字符串。

### 7.5 Repository 接口

先定义行为，不在 Controller 直接操作 `Map`：

```java
public interface TaskRepository {
    Task insert(Task task);
    Optional<Task> findById(Long id);
    List<Task> findPage(String keyword, TaskStatus status, int offset, int limit);
    long count(String keyword, TaskStatus status);
    int update(Task task);
    int updateStatus(Long id, TaskStatus status);
    int deleteById(Long id);
}
```

返回类型的含义：

- `Optional<Task>` 明确表达“可能查不到”，避免用 `null` 隐藏情况。
- `insert` 返回带数据库主键和时间字段的任务。
- 更新和删除返回受影响行数，Service 可以据此判断资源是否存在或写入是否成功。
- `findPage` 返回当前页数据，`count` 返回筛选后的总记录数；两者共同组成分页响应。

### 7.6 编写 `JdbcTaskRepository`

创建实现类并通过构造器注入 `JdbcTemplate`：

```java
@Repository
public class JdbcTaskRepository implements TaskRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // TODO：实现接口方法
}
```

`@Repository` 同时表达两件事：

- 组件扫描把该类注册为 Spring Bean。
- Spring 可以对数据访问异常做统一转换，Repository 对外主要表现为 `DataAccessException` 体系，而不是让上层依赖各数据库驱动异常。

先定义一个复用的行映射器：

```java
private final RowMapper<Task> taskRowMapper = (resultSet, rowNum) -> {
    Task task = new Task();
    task.setId(resultSet.getLong("id"));
    task.setTitle(resultSet.getString("title"));
    task.setDescription(resultSet.getString("description"));
    task.setStatus(TaskStatus.valueOf(resultSet.getString("status")));
    task.setDueDate(resultSet.getObject("due_date", LocalDate.class));
    task.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
    task.setUpdatedAt(resultSet.getObject("updated_at", LocalDateTime.class));
    return task;
};
```

这一步对应：

```text
MySQL 一行 ResultSet
  -> RowMapper
  -> Task Java 对象
```

下一阶段 MyBatis 会接管类似的结果映射工作，因此这里值得亲手写一次。

#### 插入并取得数据库生成主键

普通 `jdbcTemplate.update(sql, args...)` 能得到受影响行数，但不能直接得到自增 ID。使用 `GeneratedKeyHolder`：

```java
@Override
public Task insert(Task task) {
    String sql = """
            INSERT INTO tasks (title, description, status, due_date)
            VALUES (?, ?, ?, ?)
            """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    int affectedRows = jdbcTemplate.update(connection -> {
        PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        );
        statement.setString(1, task.getTitle());
        statement.setString(2, task.getDescription());
        statement.setString(3, task.getStatus().name());
        statement.setObject(4, task.getDueDate());
        return statement;
    }, keyHolder);

    if (affectedRows != 1 || keyHolder.getKey() == null) {
        throw new IllegalStateException("任务创建失败");
    }

    long id = keyHolder.getKey().longValue();
    return findById(id)
            .orElseThrow(() -> new IllegalStateException("任务创建后无法查询"));
}
```

需要的主要 import：

```java
java.sql.PreparedStatement
java.sql.Statement
org.springframework.jdbc.support.GeneratedKeyHolder
org.springframework.jdbc.support.KeyHolder
```

继续使用 `?` 占位符，不能用字符串拼接 title 或 description。`PreparedStatement` 既负责参数类型，也避免用户输入改变 SQL 结构。

#### 根据 ID 查询

`JdbcTemplate.query` 返回 List，适合把 0 行映射为 `Optional.empty()`：

```java
@Override
public Optional<Task> findById(Long id) {
    String sql = """
            SELECT id, title, description, status,
                   due_date, created_at, updated_at
            FROM tasks
            WHERE id = ?
            """;

    return jdbcTemplate.query(sql, taskRowMapper, id)
            .stream()
            .findFirst();
}
```

如果直接使用 `queryForObject`，查不到时会抛 `EmptyResultDataAccessException`。两种写法都可以，但这里使用 List -> Optional，让“查不到”作为正常业务分支，而不是数据库故障。

#### SQL 中完成筛选和分页

不要先 `SELECT *` 再在 Java 中筛选。使用固定 SQL 片段和参数列表动态组装条件：

```java
private QueryParts buildWhere(String keyword, TaskStatus status) {
    StringBuilder where = new StringBuilder(" WHERE 1 = 1");
    List<Object> parameters = new ArrayList<>();

    if (keyword != null && !keyword.isBlank()) {
        where.append(" AND title LIKE ?");
        parameters.add("%" + keyword.trim() + "%");
    }

    if (status != null) {
        where.append(" AND status = ?");
        parameters.add(status.name());
    }

    return new QueryParts(where.toString(), parameters);
}

private record QueryParts(String whereSql, List<Object> parameters) {
}
```

这里只动态拼接程序内部固定的 SQL 片段，用户值仍然通过 `?` 传入。不要把 `keyword` 或 `status` 本身拼接到 SQL 字符串。

分页查询：

```java
@Override
public List<Task> findPage(
        String keyword,
        TaskStatus status,
        int offset,
        int limit) {
    QueryParts parts = buildWhere(keyword, status);
    String sql = """
            SELECT id, title, description, status,
                   due_date, created_at, updated_at
            FROM tasks
            """ + parts.whereSql()
            + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";

    List<Object> parameters = new ArrayList<>(parts.parameters());
    parameters.add(limit);
    parameters.add(offset);

    return jdbcTemplate.query(sql, taskRowMapper, parameters.toArray());
}
```

总数查询使用完全相同的 WHERE 条件：

```java
@Override
public long count(String keyword, TaskStatus status) {
    QueryParts parts = buildWhere(keyword, status);
    String sql = "SELECT COUNT(*) FROM tasks" + parts.whereSql();
    Long total = jdbcTemplate.queryForObject(
            sql,
            Long.class,
            parts.parameters().toArray()
    );
    return total == null ? 0 : total;
}
```

如果 `findPage` 和 `count` 使用不同筛选条件，前端会看到“总数与当前列表不一致”，所以必须复用同一条件构建逻辑。

#### 更新、修改状态和删除

实现时使用以下 SQL，并检查返回行数：

```sql
UPDATE tasks
SET title = ?, description = ?, due_date = ?
WHERE id = ?;

UPDATE tasks
SET status = ?
WHERE id = ?;

DELETE FROM tasks
WHERE id = ?;
```

`updated_at` 由表定义的 `ON UPDATE CURRENT_TIMESTAMP(3)` 维护，所以 Java 不需要同时维护第二套更新时间规则。更新成功后再调用 `findById` 返回最新数据。

不要把 MySQL 的表名、列名或排序方向直接交给普通 `?` 参数；占位符只代表值，不能代表 SQL 标识符。当前排序固定为 `created_at DESC, id DESC`，动态排序白名单留到 MyBatis 阶段重点练习。

### 7.7 Service

`TaskService` 使用构造器注入 `TaskRepository`：

```java
@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // create / get / list / update / updateStatus / delete
}
```

Service 中需要完成：

```text
创建：请求 DTO -> Task -> Repository.insert -> Task
查询：Repository.findById -> 不存在则抛 TaskNotFoundException -> Task
列表：计算 offset -> Repository.findPage + count -> 分页业务结果
修改：先查找 -> 组合允许字段 -> Repository.update -> 再查询 -> Task
删除：先确认存在 -> 删除
```

这里的 `TaskResponse`、`PageResponse` 属于 HTTP 响应 DTO，不应该作为 Service 的返回类型。Service 返回业务对象或分页业务结果，Controller 再将其转换、包装成接口响应。当前练习可以继续把请求 DTO 传给 Service，由 Service 完成 `CreateTaskRequest -> Task` 的转换；重点是不要让 Service 依赖 HTTP 状态码和统一响应结构。

页码对外从 `1` 开始，MySQL offset 从 `0` 开始：

```text
offset = (page - 1) * size
limit = size
```

不要在参数校验完成前计算 offset，否则 `page=0` 或负数会产生错误分页。为了避免极大页码造成 `int` 乘法溢出，可以先使用 `long` 计算并判断范围，再转换为 Repository 需要的整数。

写操作建议把事务边界放在 Service：

```java
@Transactional
public Task create(CreateTaskRequest request) {
    Task task = new Task();
    task.setTitle(request.getTitle());
    task.setDescription(request.getDescription());
    task.setDueDate(request.getDueDate());
    return taskRepository.insert(task);
}
```

当前单条 UPDATE 本身具有原子性，但 insert 后再查询、update 后再查询已经涉及多个数据库操作。事务放在 Service 能明确表达这一完整业务方法的连接边界。不要在 Controller 上添加 `@Transactional`，HTTP 参数和响应处理不是数据库事务职责。

Controller 再负责将 Service 的业务结果组装成 HTTP 响应：

```java
Task task = taskService.create(request);
return new TaskResponse<Task>(TaskCode.SUCCESS, task, "创建成功");
```

因此，“事务边界放在 Service”和“响应 DTO 在 Controller 组装”是两条不同的职责规则。

## 8. 第 5 步：设计 REST Controller

`TaskController` 使用构造器注入 `TaskService`，类级路径为 `/api/tasks`。

建议接口：

| 方法 | 路径 | 参数来源 | 成功状态码 |
| --- | --- | --- | --- |
| POST | `/api/tasks` | `@RequestBody CreateTaskRequest` | `201 Created` |
| GET | `/api/tasks/{id}` | `@PathVariable Long id` | `200 OK` |
| GET | `/api/tasks` | `@ModelAttribute TaskQuery` | `200 OK` |
| PUT | `/api/tasks/{id}` | 路径 + `@RequestBody UpdateTaskRequest` | `200 OK` |
| PATCH | `/api/tasks/{id}/status` | 路径 + `@RequestBody UpdateTaskStatusRequest` | `200 OK` |
| DELETE | `/api/tasks/{id}` | `@PathVariable Long id` | `200 OK` + JSON |

Controller 方法骨架：

```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody CreateTaskRequest request) {
        // TODO：调用 Service，返回 201
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        // TODO
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            @Valid @ModelAttribute TaskQuery query) {
        // TODO
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        // TODO
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        // TODO
    }

    @DeleteMapping("/{id}")
    public TaskResponse<Integer> delete(@PathVariable Long id) {
        // TODO：调用 Service，返回删除行数和成功消息
    }
}
```

为什么部分方法使用 `ResponseEntity`：

- 普通 `200` 可以直接返回 Java 对象。
- 创建需要明确表达 `201`。
- 删除在本项目中返回 `200 + JSON`，便于前端统一读取 `code/message/data`；如果接口约定不需要响应体，也可以改为 `204`。
- `ResponseEntity` 是 HTTP 响应描述，不是业务实体。

不要返回字符串形式的 JSON，也不要直接调用 `response.getWriter()`。否则会绕开本阶段要练习的返回值处理器和消息转换器。

## 9. 第 6 步：Bean Validation

### 9.1 在请求 DTO 上声明约束

示例约束：

```java
public class CreateTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题不能超过100个字符")
    private String title;

    @Size(max = 500, message = "任务描述不能超过500个字符")
    private String description;

    @FutureOrPresent(message = "截止日期不能早于今天")
    private LocalDate dueDate;

    // constructor / getter / setter
}
```

查询 DTO：

```java
public class TaskQuery {

    @Min(value = 1, message = "page最小为1")
    private int page = 1;

    @Min(value = 1, message = "size最小为1")
    @Max(value = 50, message = "size最大为50")
    private int size = 10;

    private String keyword;
    private TaskStatus status;

    // getter / setter
}
```

状态 DTO：

```java
public class UpdateTaskStatusRequest {
    @NotNull(message = "任务状态不能为空")
    private TaskStatus status;

    // getter / setter
}
```

### 9.2 `@Valid` 的真实作用

只有在 Controller 参数前添加 `@Valid`，Spring MVC 才会在参数解析后触发对象校验：

```java
@Valid @RequestBody CreateTaskRequest request
```

链路是：

```text
JSON
  -> Jackson 创建 CreateTaskRequest
  -> Spring MVC 触发 Validator
  -> Hibernate Validator 检查字段注解
  -> 通过：调用 Controller
  -> 失败：Controller 不执行，抛出校验异常
```

职责边界：

- “标题不能为空”是输入格式校验，放 DTO 注解。
- “任务不存在”需要查询 Repository，是业务判断，放 Service 并抛业务异常。
- “当前用户无权修改任务”也是业务或安全判断，不能只靠字段注解。

### 9.3 绑定失败不都属于同一种异常

要能区分：

| 输入问题 | 大致发生阶段 |
| --- | --- |
| JSON 语法错误 | Jackson 反序列化阶段 |
| `status: "UNKNOWN"` | JSON 转枚举失败 |
| 路径 `id=abc` | 字符串转 `Long` 失败 |
| `title=""` | DTO 已创建，Bean Validation 失败 |
| 查询参数 `page=abc` | `@ModelAttribute` 类型绑定失败 |

它们最后都可能返回 `400`，但原因和抛出的异常并不相同。当前阶段至少亲手验证其中两类，不要求背完异常类列表。

## 10. 第 7 步：统一异常处理

### 10.1 业务异常

创建 `TaskNotFoundException`，继承 `RuntimeException`：

```java
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("任务不存在：" + id);
    }
}
```

Service 查询不到任务时抛出它，不要返回 `null` 后让 Controller 自己判断。

### 10.2 最小错误响应

`ErrorResponse` 建议包含：

```text
timestamp  LocalDateTime
status     int
error      String
message    String
path       String
fieldErrors Map<String, String>（没有字段错误时可为空）
```

本阶段统一的是“错误响应结构”，不强制所有成功响应再包一层 `code/message/data`。HTTP 状态码已经表达成功或失败；额外包装属于团队 API 规范，不是 Spring MVC 核心机制。

### 10.3 全局处理器

创建：

```text
exception/GlobalExceptionHandler.java
```

骨架：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            TaskNotFoundException exception,
            HttpServletRequest request) {
        // TODO：组装 ErrorResponse，返回 404
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        // TODO：从 exception.getBindingResult().getFieldErrors()
        // 收集字段名和默认提示，返回 400
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request) {
        // TODO：返回 400，不把内部堆栈直接暴露给前端
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception exception,
            HttpServletRequest request) {
        // TODO：服务端记录完整异常，响应只返回通用 500 信息
    }
}
```

需要的类型来自：

```java
jakarta.servlet.http.HttpServletRequest
org.springframework.http.ResponseEntity
org.springframework.web.bind.MethodArgumentNotValidException
org.springframework.web.bind.annotation.ExceptionHandler
org.springframework.web.bind.annotation.RestControllerAdvice
org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
org.springframework.http.converter.HttpMessageNotReadableException
```

不要在 `finally` 中写响应，也不要在每个 Controller 方法重复 `try/catch`。异常处理器本身仍然会经过返回值处理和 Jackson 转换：

```text
Service 抛异常
  -> Controller 调用失败
  -> HandlerExceptionResolver 找到 @ExceptionHandler
  -> 异常处理方法返回 ErrorResponse
  -> HttpMessageConverter 转成 JSON
```

### 10.4 关于 `@ModelAttribute` 校验异常

`@RequestBody` 校验失败通常表现为 `MethodArgumentNotValidException`。`@ModelAttribute` 的绑定或校验失败可能表现为 `BindException` 等异常，具体会受 Spring 版本和方法签名影响。

不要一开始复制庞大的异常矩阵。先实际发送 `page=0`、`page=abc`，查看你当前版本抛出的异常类型，再为真实出现的类型补一个 `400` 处理器。这也是本阶段最有代表性的排查练习。

## 11. 第 8 步：MVC Interceptor 与 CORS

### 11.1 编写最小 Interceptor

创建 `RequestTraceInterceptor`，先只观察执行链：

```java
@Component
public class RequestTraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        System.out.println("MVC preHandle: " + request.getRequestURI());
        System.out.println("handler type: " + handler.getClass().getName());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        System.out.println("MVC afterCompletion: " + response.getStatus());
    }
}
```

注册时让 `WebMvcConfig` 实现 `WebMvcConfigurer`，通过构造器注入 Interceptor：

```java
@Configuration
@EnableWebMvc
@ComponentScan("cn.siyes.training.mvc")
public class WebMvcConfig implements WebMvcConfigurer {
    private final RequestTraceInterceptor requestTraceInterceptor;

    public WebMvcConfig(RequestTraceInterceptor requestTraceInterceptor) {
        this.requestTraceInterceptor = requestTraceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor)
                .addPathPatterns("/api/**");
    }
}
```

`preHandle` 返回 `false` 会终止后续 Controller 调用。当前先固定返回 `true`，不要在这里实现登录系统。

### 11.2 与 Filter 对照

```text
Tomcat
  -> Servlet Filter
  -> DispatcherServlet
  -> HandlerMapping 已找到 Handler
  -> HandlerInterceptor.preHandle
  -> Controller
```

区别：

| Filter | HandlerInterceptor |
| --- | --- |
| Servlet 规范能力 | Spring MVC 能力 |
| 可以包住 DispatcherServlet 和其他 Servlet | 只处理进入 MVC Handler 链的请求 |
| 不天然知道将调用哪个 Controller 方法 | `handler` 通常可以看到 `HandlerMethod` |
| 常用于编码、通用安全链、底层请求包装 | 常用于 MVC 日志、权限检查、Controller 上下文 |

如果你想确认具体 Controller 方法，可以判断：

```java
if (handler instanceof HandlerMethod handlerMethod) {
    System.out.println(handlerMethod.getBeanType().getSimpleName());
    System.out.println(handlerMethod.getMethod().getName());
}
```

这正是 Interceptor 比原生 Filter 更接近 Spring MVC 路由层的地方。

### 11.3 配置 CORS

继续在 `WebMvcConfig` 中添加：

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
}
```

理解边界：

- CORS 是浏览器的跨源访问约束，不是服务端身份认证。
- Postman、IDEA HTTP Client 通常不执行浏览器同源策略，因此不能证明浏览器跨域一定成功。
- `allowCredentials(true)` 时不能把允许来源简单配置成 `*`。
- 当前只允许本地前端开发地址；真实域名以后应通过环境配置管理，不能把所有来源永久放开。

## 12. 第 9 步：手动 HTTP 验收

可以使用 IDEA HTTP Client、Postman、Apifox 或浏览器前端请求。当前 IDEA 部署的实际应用上下文为 `/03_spring_mvc`。

### 12.1 健康检查

```http
GET http://localhost:8080/03_spring_mvc/api/health
Accept: application/json
```

预期 `200`。

### 12.2 新建任务

```http
POST http://localhost:8080/03_spring_mvc/api/tasks
Content-Type: application/json
Accept: application/json

{
  "title": "学习 Spring MVC",
  "description": "串起完整请求链",
  "dueDate": "2026-08-31"
}
```

预期 `201`，响应包含服务端生成的 `id`、`status`、`createdAt`。

随后在 Navicat 查询：

```sql
SELECT *
FROM spring_training_mvc.tasks
ORDER BY id DESC;
```

确认响应中的 ID、标题和数据库记录一致。完全停止并重启 Tomcat 后再次查询该 ID，数据仍应存在；这与原内存方案的生命周期不同。

### 12.3 查询和筛选

```http
GET http://localhost:8080/03_spring_mvc/api/tasks/1
Accept: application/json
```

```http
GET http://localhost:8080/03_spring_mvc/api/tasks?page=1&size=10&status=TODO&keyword=Spring
Accept: application/json
```

预期 `200`。第二个请求用于证明 `@ModelAttribute`、枚举转换、筛选和分页同时工作。

### 12.4 修改状态

```http
PATCH http://localhost:8080/03_spring_mvc/api/tasks/1/status
Content-Type: application/json

{
  "status": "DONE"
}
```

预期 `200`，状态变为 `DONE`。

### 12.5 删除

```http
DELETE http://localhost:8080/03_spring_mvc/api/tasks/1
```

本项目约定返回 `200 + JSON`，响应体包含删除成功消息和受影响行数。再次查询 ID `1` 应返回 `404` 和统一错误 JSON。

### 12.6 两条代表性错误链路

空标题：

```http
POST http://localhost:8080/03_spring_mvc/api/tasks
Content-Type: application/json

{
  "title": "",
  "description": "invalid"
}
```

预期 `400`，错误响应指出 `title` 校验失败，Controller 的创建方法不应执行。

错误类型：

```http
GET http://localhost:8080/03_spring_mvc/api/tasks/abc
```

预期 `400`。这个请求在 Controller 调用前就因 `String -> Long` 转换失败。

本阶段不要求堆积大量错误案例。上述两类足以证明你能区分“DTO 校验失败”和“方法参数类型转换失败”。

## 13. 如何观察 Spring MVC 核心组件

### 13.1 推荐断点顺序

如果 IDEA 已下载 Spring 源码，可以依次观察：

```text
DispatcherServlet.doDispatch
RequestMappingHandlerMapping.getHandlerInternal
RequestMappingHandlerAdapter.handleInternal
RequestResponseBodyMethodProcessor.resolveArgument
TaskController.create
RequestResponseBodyMethodProcessor.handleReturnValue
```

不要求记源码实现。断点只回答：

- 当前请求在哪一层？
- 此时是否已经找到 Controller 方法？
- JSON 是在 Controller 之前还是之后转换的？
- Controller 返回时还是 Java 对象吗？

### 13.2 一次 POST 请求的对象变化

```text
HTTP 字节和请求头
  -> Tomcat 创建 HttpServletRequest / HttpServletResponse
  -> DispatcherServlet 接收 Servlet 对象
  -> HandlerMapping 返回 HandlerMethod
  -> HandlerAdapter 准备调用
  -> ArgumentResolver 识别 @RequestBody
  -> HttpMessageConverter + Jackson 创建 CreateTaskRequest
  -> Validator 校验 DTO
  -> Controller 调用 Service
  -> 返回 TaskResponse
  -> ReturnValueHandler 识别响应体
  -> HttpMessageConverter + Jackson 生成 JSON 字节
  -> Tomcat 写回 HTTP 响应
```

### 13.3 一个很有价值的可逆实验

在 `/api/health` 和普通路由已经稳定后，可以临时注释 `jackson-databind` 依赖、Reload Maven 并重新部署，然后观察返回对象或 `application/json` 请求发生什么变化。完成观察后立即恢复依赖。

这个实验用于证明：

```text
Spring MVC 负责选择和调用消息转换器
Jackson 负责具体 JSON 序列化/反序列化
```

不要把这个临时故障提交到最终版本。

## 14. 常见错误与定位顺序

### 14.1 404

依次检查：

```text
Tomcat 是否部署应用
  -> Context Path 是否为 /03_spring_mvc
  -> DispatcherServlet 是否初始化
  -> web.xml 的 url-pattern 是否为 /
  -> @ComponentScan 是否覆盖 Controller 包
  -> 类级和方法级路径拼接是否正确
```

### 14.2 415 Unsupported Media Type

检查：

- 请求是否带 `Content-Type: application/json`。
- Controller 是否使用 `@RequestBody`。
- Jackson 是否在 WAR 运行时依赖中。

### 14.3 406 Not Acceptable

检查客户端 `Accept` 是否要求了服务端无法生成的媒体类型，以及 Controller 的 `produces` 是否限制过严。

### 14.4 返回中文乱码

JSON 响应通常使用 UTF-8，不需要在每个 Controller 手动设置编码。先确认源码和 Maven 编码是 UTF-8，并观察实际 `Content-Type`。不要再回到每个 Servlet 手写 `response.setCharacterEncoding()` 的方式。

### 14.5 Controller 创建失败或注入失败

检查：

- Service 是否有 `@Service`。
- Repository 实现是否有 `@Repository`。
- 是否存在两个相同接口实现而没有消除歧义。
- 构造器参数类型是否与已注册 Bean 匹配。
- 组件包是否在扫描范围内。

### 14.6 Validation 没有执行

检查：

- DTO 字段是否使用 `jakarta.validation.constraints.*`。
- Controller 参数是否添加 `@Valid`。
- 是否同时存在 `jakarta.validation-api` 和实现 `hibernate-validator`。
- 是否误把业务规则当成字段注解校验。

### 14.7 数据库连接失败

按下面顺序定位：

```text
MySQL 服务是否启动
  -> Navicat 是否能用同一账号访问 spring_training_mvc
  -> Tomcat Run Configuration 是否存在三个 MVC_DB_* 环境变量
  -> JDBC URL 中是否误写 \=
  -> mysql-connector-j 是否进入 WEB-INF/lib
  -> HikariCP 启动日志中的根异常
```

如果应用在部署阶段就失败，这是 `initializationFailTimeout` 的预期作用：连接池尽早暴露错误，而不是等第一个 API 请求才返回模糊的 `500`。

## 15. 与 `linux-server` 的最小对照

本阶段实现稳定后再读 `linux-server`，只做结构映射，不复制代码：

```text
本 Module TaskController
  <-> linux-server Controller

CreateTaskRequest / TaskQuery / ErrorResponse
  <-> linux-server DTO

TaskService
  <-> linux-server Service

WebMvcConfig / GlobalExceptionHandler
  <-> linux-server Web 配置和异常处理
```

重点找出：

- 哪些配置是 Spring MVC 本身提供的。
- 哪些配置是 Spring Boot 自动配置的，因此项目里看不到手动注册代码。
- Controller 如何接收 DTO、调用 Service、返回响应。
- 全局异常是怎样映射成状态码的。

`linux-server` 能运行不代表本阶段已经掌握；能力证据仍然来自你手写的 `03-spring-mvc`。

## 16. 最终验收问题

完成代码后，需要用自己的话回答：

1. Tomcat 为什么能调用 `DispatcherServlet`？
2. `DispatcherServlet` 如何找到某个 Controller 方法？
3. HandlerMapping 和 HandlerAdapter 为什么要分成两个组件？
4. `@PathVariable`、`@RequestParam`、`@ModelAttribute`、`@RequestBody` 的数据分别来自哪里？
5. JSON 转 DTO 和 DTO 转 JSON 分别发生在什么时机？
6. `@Valid` 为什么能阻止不合法请求进入 Controller？
7. Service 抛出的 `TaskNotFoundException` 如何最终变成 `404` JSON？
8. Filter 和 Interceptor 在请求链中的位置及能力有什么差异？
9. `HikariDataSource`、`JdbcTemplate` 和 `JdbcTaskRepository` 分别承担什么职责？换成 MyBatis 时哪些层应变化、哪些层尽量不变？
10. Spring Boot 在未来会替你自动完成本 Module 中的哪些步骤？

## 17. 最终验收清单

- [x] Module 能以 WAR 形式部署到 Tomcat 10.1。
- [x] `/api/health` 返回 JSON，能解释 `Map -> JSON` 的转换者。
- [x] 任务新增、查询、筛选分页、修改、状态修改和删除可用。
- [x] Navicat 能看到 HTTP 请求写入的任务，Tomcat 重启后数据仍然存在。
- [x] 四种主要参数来源均完成代码练习。
- [x] DTO 校验失败返回 `400`，且 Controller 不执行。
- [x] 不存在的任务返回 `404`，错误结构稳定。
- [x] 未知异常不会把完整堆栈或原始内部错误返回给前端。
- [x] Interceptor 能打印匹配到的 Controller 类型和方法。
- [x] 能解释 Filter、DispatcherServlet、Interceptor、Controller 的顺序。
- [x] 完成至少一条成功链路和两类代表性错误链路的真实 HTTP 验证。
- [x] 能完整口述第 16 节问题，并形成简短复盘。

2026-08-22 已结合源码、Apifox、Navicat、Tomcat/Interceptor 日志、复盘和 Maven WAR 构建完成阶段验收，本 Module 状态为 `已完成`。
