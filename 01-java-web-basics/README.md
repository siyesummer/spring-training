# Module 01：Java Web 基础实战引导

> 当前状态：`已完成`
> 开始日期：2026-08-14
> 完成日期：2026-08-18
> 完成情况：已独立完成 Servlet、Filter、Listener、Session、JDBC、事务、WAR 部署和核心功能验收。JUnit、Maven 命令和异常记录作为辅助工程能力保留。

## 1. 先回答项目创建问题

### 应该创建哪一种项目？

本阶段建议创建一个 **传统 Maven Web 应用**，最终配置为：

```xml
<packaging>war</packaging>
```

它部署到 Tomcat，由 Tomcat 接收 HTTP 请求并调用 Servlet。这个阶段暂时不使用 Spring、Spring MVC 或 Spring Boot，目的是先看清楚后续框架替你封装了哪些底层工作。

截图中的 `maven-archetype-webapp` 可以作为初始模板，但它生成的模板比较旧，创建后仍要检查和补充 `pom.xml`、Java 源码目录及测试目录。更推荐在 IntelliJ IDEA 中选择 `Java -> Maven` 创建普通 Maven 项目，然后手动设置 `packaging` 为 `war`，这样目录和依赖更容易理解。

### 创建位置

不要把 Module 建在 `E:\本地项目\java-project` 下与 `spring-training` 并列。Module 最终应位于：

```text
E:\本地项目\java-project\spring-training\01-java-web-basics
```

在 IDEA 的“创建 Maven Module”窗口中，`Location` 填父目录 `E:\本地项目\java-project\spring-training`，再由 `Name` 自动创建最后一级目录。不要把完整的 `...\spring-training\01-java-web-basics` 同时填入 Location，否则会生成重复目录。

本目录是一个可以独立执行 Maven 命令的 Module。当前先保持 Module 独立，等第一个 Module 验收稳定后，再决定是否把根 `pom.xml` 改成统一的 Maven 聚合项目。

### IntelliJ IDEA 创建参数

| 配置项 | 建议值 |
| --- | --- |
| Name | `01-java-web-basics` |
| Location | `E:\本地项目\java-project\spring-training`（IDEA 会自动追加 Name） |
| JDK | 21（与当前环境一致） |
| Build system | Maven |
| Archetype | 可选 `org.apache.maven.archetypes:maven-archetype-webapp`；也可以不选模板手动创建 |
| Packaging | `war` |
| GroupId | `cn.siyes.training` |
| ArtifactId | `01-java-web-basics` |

如果 IDEA 的 Archetype 页面只显示旧模板，不要因此改用 Spring Boot。模板版本不是本阶段的核心，`war + Servlet + Tomcat` 才是核心。

兼容性注意：本引导按 Tomcat 10.1 和 Jakarta Servlet 6 编写，Java 代码中的导入应使用 `jakarta.servlet.*`。如果旧 Archetype 或旧视频示例出现 `javax.servlet.*`，不要混用两套包名；要么统一改成 Jakarta 版本，要么改用 Tomcat 9 及对应的旧 Servlet API。本项目统一选择前者。

## 2. 实战目标

实现一个“用户注册、登录和留言板”小服务，使用静态 HTML 作为前端页面、原生 Servlet 作为后端接口、MySQL 作为数据库。

本机已经安装 MySQL，因此本阶段直接使用 MySQL + 原生 JDBC。这样除了练习 `Connection`、`PreparedStatement`、`ResultSet` 和事务，还能实际处理数据库服务、专用账号、字符集和连接失败。当前安装的 MySQL 5.7.19 可以用于本地训练，但该版本已经停止官方支持；Module 01 使用兼容它的 Connector/J 8.0.33，综合项目之前应升级到仍受支持的 MySQL 版本。

第一轮只追求看懂完整请求链路，不追求页面美观或复杂业务。完成后应能解释：

```text
Tomcat 启动 Web 应用 -> ServletContextListener.contextInitialized

浏览器
  -> HTTP 请求
  -> Tomcat
  -> Filter
  -> Servlet
  -> JDBC DAO
  -> MySQL 数据库
  -> Servlet 写回 HTTP 响应

Session 创建/销毁 -> HttpSessionListener
Tomcat 停止 Web 应用 -> ServletContextListener.contextDestroyed
```

这里要区分三类容器扩展点：Servlet 处理具体请求，Filter 包住请求并决定是否继续，Listener 不处理请求本身，而是接收 Web 应用、Session 或请求生命周期事件的回调。

## 3. Maven 配置与依赖规划

先区分根 POM 和 Module POM：所有 Module 共用的 Java 版本、编码配置和依赖版本放在根目录的 `pom.xml`；本阶段实际使用哪些依赖仍由 `01-java-web-basics/pom.xml` 声明。这样后续 Module 不需要重复维护版本，但仍能明确知道自己使用了哪些依赖。

### 3.1 根目录 `pom.xml`

在根 POM 的 `<properties>` 中配置：

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
    <mysql.connector.version>8.0.33</mysql.connector.version>
    <junit.jupiter.version>5.10.3</junit.jupiter.version>
</properties>
```

在根 POM 中使用 `dependencyManagement` 统一管理后续 Module 可能复用的依赖版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.connector.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.jupiter.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

`dependencyManagement` 只管理版本，不会自动把依赖加入每个 Module；Module 仍需在自己的 `<dependencies>` 中声明实际需要的依赖。

然后删除 Module POM 中 Archetype 自动生成的 Java 8 配置：

```xml
<maven.compiler.source>8</maven.compiler.source>
<maven.compiler.target>8</maven.compiler.target>
```

### 3.2 Module 的 `pom.xml`

在 `01-java-web-basics/pom.xml` 的 `<dependencies>` 中填写以下完整内容：

```xml
<dependencies>
    <!-- 编译时需要，运行时由 Tomcat 10.1 提供 -->
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <version>6.0.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- MySQL JDBC 驱动，版本由根 POM 统一管理 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 密码慢哈希，本 Module 独有，因此在此声明版本 -->
    <dependency>
        <groupId>at.favre.lib</groupId>
        <artifactId>bcrypt</artifactId>
        <version>0.10.2</version>
    </dependency>

    <!-- 测试普通 Java 类和参数校验逻辑，版本由根 POM 统一管理 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

`jakarta.servlet-api` 的 `provided` 表示编译时需要、运行时由 Tomcat 提供，因此不会重复打入 WAR。MySQL 驱动使用 `runtime`，因为业务代码只依赖标准的 `java.sql` 接口，驱动实现只需在运行和测试时出现，并会被打入 WAR。BCrypt 使用默认 `compile`，因为业务代码会直接调用它的 API。JUnit 使用 `test`，只在测试编译和运行时使用。

Module 的 `packaging` 必须保持为 `war`。Archetype 生成的 `pluginManagement` 可以先保留，它用于锁定 Maven 插件版本。编码前先理解每一项的作用，不要只复制配置。

| 依赖或插件 | 作用 | 说明 |
| --- | --- | --- |
| `jakarta.servlet:jakarta.servlet-api:6.0.0` | Servlet 接口、请求响应对象、Filter、Session 等 API | `provided`，运行时由 Tomcat 提供 |
| `com.mysql:mysql-connector-j:8.0.33` | MySQL JDBC 驱动 | `runtime`，兼容当前 MySQL 5.7.19；版本由根 POM 管理 |
| `at.favre.lib:bcrypt:0.10.2` | 密码慢哈希 | 默认 `compile`，只在本 Module 使用，版本留在 Module POM |
| `org.junit.jupiter:junit-jupiter:5.10.3` | 单元测试 | `test`，用于测试参数校验、密码规则等可独立测试的代码 |
| `org.apache.maven.plugins:maven-compiler-plugin` | 按 Java 21 编译 | 配置 `release=21` |
| `org.apache.maven.plugins:maven-war-plugin` | 打包 WAR | 用于部署到 Tomcat |

当前不引入：

- `spring-webmvc`、`spring-boot-starter-web`：会隐藏本阶段要观察的 Servlet 细节。
- MyBatis、JPA：数据访问先直接使用 JDBC。
- Lombok：先熟悉构造器、Getter/Setter 和普通 Java 类。

## 4. 推荐目录结构

```text
01-java-web-basics/
├─ pom.xml
├─ README.md
├─ src/
│  ├─ main/
│  │  ├─ java/cn/siyes/training/web/
│  │  │  ├─ filter/
│  │  │  ├─ listener/
│  │  │  ├─ servlet/
│  │  │  ├─ dao/
│  │  │  ├─ model/
│  │  │  ├─ service/
│  │  │  └─ util/
│  │  ├─ resources/
│  │  │  └─ schema.sql
│  │  └─ webapp/
│  │     ├─ index.html
│  │     ├─ login.html
│  │     └─ message.html
│  └─ test/java/cn/siyes/training/web/
└─ docs/
   └─ ACCEPTANCE.md
```

说明：`src/main/webapp` 是 WAR 中的 Web 根目录；Java 类仍然放在 `src/main/java`，不要把 Servlet 代码放进 `webapp`。

## 5. 分步实施顺序

### 第 0 步：确认最小工程能编译

先只创建项目和 `pom.xml`，执行：

```bash
mvn clean test
```

验收点：能下载依赖并成功编译；如果失败，先记录 JDK、Maven 和网络错误，不要继续堆代码。

### 第 0.1 步：准备 MySQL 训练环境

后续数据库相关的人工操作统一在 Navicat Premium 中完成，包括：创建数据库和用户、授权、执行建表或初始化 SQL、查看表结构和数据、验证事务结果以及主动制造数据库错误。Java 应用仍然通过 JDBC 访问 MySQL；Navicat 只是数据库管理客户端，不是 Java 项目的运行依赖。

建议优先使用 Navicat 的“新建查询”执行 SQL，而不是只依赖可视化表设计器。这样可以理解实际执行的 SQL，并把最终建表和初始化语句同步保存到 `src/main/resources/schema.sql`，确保数据库能够重新创建。

当前 Windows 服务名为 `MySQL`，已经启动且 Navicat 已成功连接。以后需要排查服务状态时可以使用 Windows 服务管理器；开始 JDBC 编码前应确认 Navicat 连接可用。

```powershell
Get-Service MySQL
```

如果状态是 `Stopped`，使用 Windows 服务管理器启动，或在具有相应权限的 PowerShell 中执行：

```powershell
Start-Service MySQL
```

服务确认正常后，在 Navicat 的本机 MySQL 连接中新建查询，执行以下 SQL。不要复用已有的 `chat_db` 或 `shop_db`，也不要让 Java 应用使用 `root` 账号：

```sql
CREATE DATABASE spring_training_web
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER 'spring_training'@'localhost'
    IDENTIFIED BY 'replace-with-your-local-password';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
    ON spring_training_web.*
    TO 'spring_training'@'localhost';

FLUSH PRIVILEGES;

SHOW GRANTS FOR 'spring_training'@'localhost';
```

执行完成后：

1. 在 Navicat 中刷新数据库列表，确认出现 `spring_training_web`。
2. 新建一个独立的 Navicat 连接，用户名使用 `spring_training`，默认数据库选择 `spring_training_web`。
3. 使用该账号测试连接并执行 `SELECT DATABASE();`，确认应用账号和数据库都正确。
4. 后续建表、查询数据和验证 SQL 都使用这个专用连接；只有创建用户或调整权限时才使用管理员连接。

Navicat 中保存的是数据库连接；Java 程序还需要自己的 JDBC 连接参数。应用通过环境变量读取以下信息，这些变量在 IDE 运行配置或启动应用的终端中设置，不在 Navicat 中设置。以下 PowerShell 示例只对当前终端会话生效，密码不写入 `pom.xml`、Java 源码或 Git：

```powershell
$env:TRAINING_DB_URL = "jdbc:mysql://localhost:3306/spring_training_web?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false"
$env:TRAINING_DB_USER = "spring_training"
$env:TRAINING_DB_PASSWORD = "replace-with-your-local-password"
```

验收点：Navicat 能使用专用账号连接 `spring_training_web`，能够通过 `SHOW GRANTS` 解释账号权限；停止 MySQL 服务或填错密码时，能够识别并解释连接失败。

### 第 0.2 步：准备 Tomcat 运行环境

本 Module 使用传统 `WAR` 部署方式，因此需要安装外部 Tomcat 才能真正启动 Servlet。Maven 编译阶段只需要 `jakarta.servlet-api`，不要求本机已经安装 Tomcat；但访问 `/health`、部署 WAR 和观察 Servlet 生命周期时必须有 Tomcat。

版本必须匹配：

| 项目 | 本项目选择 |
| --- | --- |
| Tomcat | 10.1.x |
| Servlet API | Jakarta Servlet 6.0 |
| Java | JDK 21 |
| Java 包名 | `jakarta.servlet.*` |

不要把 Tomcat 9 与当前代码混用。Tomcat 9 使用旧的 `javax.servlet.*` 命名空间；Tomcat 10.1 才与本项目的 `jakarta.servlet-api:6.0.0` 对应。

建议先下载 Tomcat 10.1 的 ZIP 压缩包并手动解压到一个固定目录，例如：

```text
D:\apache-tomcat-10.1.x
```

第一轮训练不需要把 Tomcat 安装成 Windows 服务。使用命令窗口启动更容易观察日志：

```bat
cd /d D:\apache-tomcat-10.1.x\bin
catalina.bat run
```

停止时在同一个窗口按 `Ctrl+C`。如果需要后台启动，可以使用 `startup.bat`，停止使用 `shutdown.bat`。默认端口是 `8080`，如果端口被占用，应先查看 Tomcat 日志和 `conf/server.xml`，不要直接修改代码绕过问题。

WAR 部署方式：

1. 执行 `mvn package`。
2. 将 `target/01-java-web-basics.war` 复制到 Tomcat 的 `webapps` 目录。
3. 等待 Tomcat 自动解压和部署。
4. 后续实现 `HealthServlet` 后访问：

```text
http://localhost:8080/01-java-web-basics/health
```

验收点：能说明 Maven 的 `provided` 依赖与 Tomcat 运行时提供 Servlet API 的关系；能从 Tomcat 日志判断 WAR 是否部署成功；能解释项目名为什么成为默认上下文路径。

### 第 1 步：写一个最小 Servlet

实现 `HealthServlet`，访问 `/health` 时返回：

```json
{"status":"ok"}
```

需要理解：

- `HttpServlet`、`doGet`、`HttpServletRequest`、`HttpServletResponse`。
- `@WebServlet("/health")` 如何把 URL 映射到类。
- `response.setContentType("application/json;charset=UTF-8")` 为什么要明确字符集。

先用 Tomcat 运行并用浏览器或 HTTP 客户端访问，不急着做注册功能。

### 第 2 步：注册用户、写入 MySQL 和重复用户校验

本步骤完成第一条包含 Servlet、Service、DAO 和 MySQL 的完整写入链路：

```text
POST /api/register
Content-Type: application/x-www-form-urlencoded
请求参数：username、password
成功：201
参数错误：400
用户名已存在：409
数据库异常：500
```

第一轮使用表单编码 `application/x-www-form-urlencoded`，后续再增加 JSON 请求体。实现顺序如下，不要先把全部代码堆进 Servlet。

#### 2.1 先修正当前代码中的问题

1. `web.xml` 的路径映射必须以 `/` 开头：

   ```xml
   <url-pattern>/api/register</url-pattern>
   ```

2. 参数错误响应后必须 `return`，否则设置 `400` 后仍会继续执行注册逻辑。
3. `null` 只能判断参数是否缺失，还需要用 `isBlank()` 拦截空字符串和纯空格。
4. 读取参数前先执行 `request.setCharacterEncoding("UTF-8")`。
5. 建议把 `Register` 重命名为 `RegisterServlet`，让类名直接表达职责。
6. 当前 `schema.sql` 中不应保存真实数据库密码。管理员建库、创建用户和授权的 SQL 在 Navicat 中手动执行；如果需要记录，可放到 `docs/database/bootstrap.sql` 并使用密码占位符。`src/main/resources/schema.sql` 只保存表、索引等应用结构。

如果已经把真实使用的密码写入项目文件，应手动删除并在 Navicat 中修改该账号密码；不要只删除文件而继续使用已暴露的密码。

#### 2.2 在 Navicat 中创建用户表

在 `spring_training_web` 专用连接的查询窗口执行，并把最终建表 SQL 手动保存到 `src/main/resources/schema.sql`：

```sql
USE spring_training_web;

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
```

`UNIQUE KEY uk_users_username` 是重复注册的最终保障。当前 `utf8mb4_unicode_ci` 排序规则默认不区分大小写，因此 `Alice` 和 `alice` 会发生唯一键冲突。

#### 2.3 使用 BCrypt 保存密码哈希

禁止把明文密码写入数据库。手动在 Module POM 中加入：

```xml
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```

该依赖目前只有本 Module 使用，因此直接在 Module POM 声明版本；以后多个 Module 共用时，再把版本提升到根 POM 的 `dependencyManagement`。

生成哈希：

```java
import at.favre.lib.crypto.bcrypt.BCrypt;

String passwordHash = BCrypt.withDefaults()
    .hashToString(12, password.toCharArray());
```

数据库只保存 `passwordHash`。Servlet 响应、日志和异常信息都不能输出原密码或密码哈希。

#### 2.4 建立注册分层

推荐手动整理为：

```text
cn.siyes.training.web/
├─ dao/
│  ├─ ConnectionFactory.java
│  └─ UserDao.java
├─ model/
│  └─ User.java
├─ exception/
│  └─ UsernameAlreadyExistsException.java
├─ service/
│  └─ RegisterService.java
└─ servlet/
   └─ RegisterServlet.java
```

职责边界：

| 类 | 职责 |
| --- | --- |
| `ConnectionFactory` | 从环境变量读取 JDBC 参数并创建 `Connection` |
| `UserDao` | 执行用户查询和插入 SQL，不处理 HTTP 状态码 |
| `RegisterService` | 参数规则、密码哈希、重复注册判断和流程编排；成功返回用户 ID，业务冲突抛出业务异常 |
| `UsernameAlreadyExistsException` | 表示用户名已存在的业务异常，不依赖 Servlet 或 HTTP |
| `RegisterServlet` | 读取 HTTP 参数，调用 Service，将结果转换为状态码和 JSON |

#### 2.5 建立 JDBC 连接

Tomcat 进程必须能读取 `TRAINING_DB_URL`、`TRAINING_DB_USER`、`TRAINING_DB_PASSWORD`。使用 IDEA 启动 Tomcat 时，应把它们手动配置到 Tomcat Run Configuration 的环境变量中，而不是写进 Java、POM 或 Git。

IDEA 中的手动配置步骤：

1. 打开 `Run -> Edit Configurations`。
2. 选择当前的 `Tomcat Server -> Local` 配置。
3. 在 `Server` 页点击 `Modify options`，启用 `Environment variables`；部分 IDEA 版本把入口放在 `Startup/Connection -> Run` 中。
4. 打开环境变量编辑框，逐项添加：

   | Name | Value |
   | --- | --- |
   | `TRAINING_DB_URL` | `jdbc:mysql://localhost:3306/spring_training_web?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false` |
   | `TRAINING_DB_USER` | `spring_training` |
   | `TRAINING_DB_PASSWORD` | 本地训练账号的真实密码，只填在 IDEA 本地配置中 |

5. Value 不要额外添加引号，点击 `Apply` 和 `OK`。
6. 完整停止并重新启动 Tomcat，使新的 JVM 进程读取环境变量。

这些变量属于 Tomcat 进程，与 Navicat 中保存的连接互不共享。Navicat 连接成功不代表 Tomcat 已获得相同参数。不要在日志中输出 `TRAINING_DB_PASSWORD`；需要确认配置时，可以用调试器查看 `requireEnv` 是否正常返回，或直接执行一次数据库连接测试。

`ConnectionFactory` 的核心结构：

```java
public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(
        System.getenv("TRAINING_DB_URL"),
        System.getenv("TRAINING_DB_USER"),
        System.getenv("TRAINING_DB_PASSWORD")
    );
}
```

Connector/J 8 支持 JDBC 4 自动驱动注册。在独立 Java 程序中通常不需要显式调用 `Class.forName`；但在当前 Tomcat Web 应用中，`DriverManager` 与 Web 应用类加载器可能存在隔离，实测删除显式加载后会出现 `No suitable driver` 并返回 500。因此本 Module 可以保留：

```java
static {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        throw new ExceptionInInitializerError(e);
    }
}
```

这段初始化应放在 `ConnectionFactory` 中，只在类第一次加载时执行，不要在每次注册请求的 `RegisterService` 方法中重复调用。后续使用 Spring Boot 数据源自动配置时，这个细节通常会被框架隐藏。仍然要检查环境变量是否为 `null` 或空字符串，并抛出含变量名但不含密码值的明确异常。

#### 2.6 实现 UserDao

第一轮需要两个方法：

```java
boolean existsByUsername(Connection connection, String username)
long insert(Connection connection, String username, String passwordHash)
```

检查用户名：

```sql
SELECT 1
FROM users
WHERE username = ?
LIMIT 1;
```

`existsByUsername` 需要导入：

```java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
```

完整实现：

```java
public boolean existsByUsername(
        Connection connection,
        String username
) throws SQLException {
    String sql = """
        SELECT 1
        FROM users
        WHERE username = ?
        LIMIT 1
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, username);

        try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }
}
```

关键语义：

- `?` 是预编译参数占位符，不能用字符串拼接用户名。
- `statement.setString(1, username)` 中的参数编号从 `1` 开始，不是从 `0` 开始。
- `executeQuery()` 用于执行 `SELECT` 并返回 `ResultSet`。
- `resultSet.next()` 尝试移动到第一行：返回 `true` 表示查询到用户，返回 `false` 表示没有用户。
- SQL 只查询常量 `1`，这里只关心记录是否存在，因此不需要再调用 `getInt()`。
- 方法必须声明 `throws SQLException`。不能捕获数据库异常后返回 `false`，因为“查询失败”和“查询成功但用户不存在”是两种不同结果。

插入用户：

```sql
INSERT INTO users (username, password_hash)
VALUES (?, ?);
```

两个方法都必须使用 `PreparedStatement` 和 `try-with-resources`。插入时使用 `Statement.RETURN_GENERATED_KEYS`，读取数据库生成的用户 ID。禁止把用户名拼接进 SQL。

资源所有权必须清楚：`UserDao` 负责关闭自己创建的 `PreparedStatement` 和 `ResultSet`，但不能关闭调用者传入的 `Connection`。Connection 由 Service 创建和关闭，这样同一个业务操作中的多个 DAO 调用才能共用连接和事务。

`RegisterService` 不能把 Connection 保存在静态字段中。Servlet 会并发处理请求，静态 Connection 会被所有请求共享，可能产生线程安全、连接超时、事务互相影响和连接永不关闭等问题。每次注册应获取一个连接，并在业务操作结束后关闭：

```java
public long register(String username, String password)
        throws SQLException {
    UserDao userDao = new UserDao();

    try (Connection connection = ConnectionFactory.getConnection()) {
        boolean exists = userDao.existsByUsername(connection, username);

        if (exists) {
            throw new UsernameAlreadyExistsException(username);
        }

        // 生成 BCrypt 哈希并调用 userDao.insert(...)
        // 成功后返回数据库生成的用户 ID
    }
}
```

这里的 `try (Connection connection = ...)` 是 `try-with-resources`。它等价于“执行完业务后，在 `finally` 中关闭连接”，但由 Java 编译器保证关闭动作执行：

```java
Connection connection = null;
try {
    connection = ConnectionFactory.getConnection();
    // 使用 connection 执行业务
} finally {
    if (connection != null) {
        connection.close();
    }
}
```

训练时优先使用 `try-with-resources`，原因是：

- `ConnectionFactory.getConnection()` 失败时，连接变量不会被错误地关闭，也不会因为 `connection == null` 再触发空指针。
- DAO 查询或插入抛出 `SQLException` 时，Java 仍会先关闭连接，再把原异常继续向上抛出。
- 连接的创建和关闭范围清晰，方法结束、`return` 或异常退出时都会执行关闭。
- 关闭由 Service 创建的 `Connection`；DAO 只关闭自己创建的 `PreparedStatement`、`ResultSet` 和生成键结果集，不关闭传入的连接。

`try-with-resources` 中声明的资源只在 `try` 代码块内有效。后续如果一个注册业务包含多条 SQL，应把它们放在同一个 `try` 中，让多个 DAO 共用同一条连接，并在 Service 中统一控制事务提交和回滚。

这里的 `UsernameAlreadyExistsException` 是业务异常，不是 HTTP 状态码。建议创建：

```text
src/main/java/cn/siyes/training/web/exception/UsernameAlreadyExistsException.java
```

```java
package cn.siyes.training.web.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("用户名已存在: " + username);
    }

    public UsernameAlreadyExistsException(
            String username,
            Throwable cause
    ) {
        super("用户名已存在: " + username, cause);
    }
}
```

`throw` 是实际抛出异常，`throws` 是方法声明可能抛出的异常。由于这个类继承 `RuntimeException`，方法签名中可以不写它；Servlet 仍然可以主动捕获：

```java
try {
    long userId = registerService.register(username, password);
    resp.setStatus(HttpServletResponse.SC_CREATED);
} catch (UsernameAlreadyExistsException e) {
    resp.setStatus(HttpServletResponse.SC_CONFLICT);
} catch (SQLException e) {
    // 记录服务端日志，不把 SQL 细节返回给前端
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
}
```

不要在 `RegisterService` 中导入 `HttpServletResponse` 或返回 `201`、`409`、`500`；状态码只在 `RegisterServlet` 这一层设置。

当前只有一条查询或单条插入时可以使用 JDBC 默认的自动提交。后续一个注册业务包含多条相关写操作时，仍应在 Service 中使用同一个 Connection 管理提交和回滚。

#### 2.7 重复用户校验必须有两层

注册流程：

```text
校验并规范化参数
  -> UserDao.existsByUsername
  -> 已存在：返回用户名冲突
  -> 不存在：生成 BCrypt 哈希并尝试 INSERT
  -> INSERT 成功：返回新用户 ID
  -> INSERT 仍触发 MySQL 1062：仍按用户名冲突处理
```

不能只做“先查询再插入”。两个并发请求可能同时查询到不存在，然后同时插入；只有数据库唯一索引能够最终阻止重复数据。提前查询用于给出友好提示，唯一索引用于保证数据正确性。

Connector/J 通常把重复键映射为 `SQLIntegrityConstraintViolationException`，MySQL 错误码为 `1062`。Service 应把它转换为业务层的“用户名已存在”，Servlet 再返回 `409`；其他 `SQLException` 返回 `500`，详细异常只记录在服务端日志中。

查询重复用户名时直接抛出：

```java
import cn.siyes.training.web.exception.UsernameAlreadyExistsException;

if (userDao.existsByUsername(connection, username)) {
    throw new UsernameAlreadyExistsException(username);
}
```

插入阶段还要捕获数据库唯一索引异常，避免并发请求绕过前置查询：

```java
import java.sql.SQLIntegrityConstraintViolationException;

try {
    return userDao.insert(connection, username, passwordHash);
} catch (SQLIntegrityConstraintViolationException e) {
    throw new UsernameAlreadyExistsException(username, e);
}
```

不要把所有 `SQLException` 都转换成“用户名已存在”；只有明确的唯一键冲突才返回 `409`，连接失败、超时和其他 SQL 错误应继续交给 Servlet 转换为 `500`。

#### 2.8 Servlet 参数与响应

Servlet 的参数入口至少应做到：

```java
req.setCharacterEncoding("UTF-8");

String username = req.getParameter("username");
String password = req.getParameter("password");

if (username == null || username.isBlank()
        || password == null || password.isBlank()) {
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    resp.setContentType("application/json;charset=UTF-8");
    resp.getWriter().write("{\"message\":\"用户名和密码不能为空\"}");
    return;
}

username = username.trim();
```

成功、业务冲突和系统异常都需要返回 JSON 时，不要让某个分支提前依赖可能尚未初始化的 `PrintWriter`。将写响应集中到一个辅助方法中，每个 `try/catch` 分支直接调用它：

```java
private void writeResponse(
        HttpServletResponse response,
        int status,
        String message
) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().print(createResponse(status, message));
}
```

```java
try {
    registerService.register(username, password);
    writeResponse(response, HttpServletResponse.SC_CREATED, "注册成功");
} catch (UsernameAlreadyExistsException e) {
    writeResponse(response, HttpServletResponse.SC_CONFLICT, "用户名已存在");
} catch (SQLException e) {
    // 记录服务端日志，不向前端暴露 SQL 细节
    writeResponse(
            response,
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "未预期数据库错误"
    );
}
```

`HttpServletResponse.getWriter()` 正常情况下不会返回 `null`；如果业务异常发生在调用它之前，保存 Writer 的局部变量可能仍为 `null`。`finally` 应主要用于关闭资源，不应依赖未完成赋值的 Writer 来生成响应。`sendError` 会交给容器生成默认错误页面，不适合需要统一 JSON 格式的 API。

状态码使用常量而不是直接写数字：

| 场景 | 状态码 |
| --- | --- |
| 用户名或密码不符合规则 | `SC_BAD_REQUEST`（400） |
| 注册成功 | `SC_CREATED`（201） |
| 用户名已存在 | `SC_CONFLICT`（409） |
| 未预期数据库错误 | `SC_INTERNAL_SERVER_ERROR`（500） |

注册成功可以返回用户 ID 和用户名，但绝不能返回密码或 `password_hash`。

#### 2.9 主动制造数据库连接失败

为了验证 `500` 异常路径，优先只修改 IDEA Tomcat Run Configuration 中的本地环境变量，不要修改源码、POM 或 Navicat 中的数据库结构。

1. 记录当前正确的 `TRAINING_DB_URL`，确认端口是 `3306`。
2. 打开 `Run -> Edit Configurations -> Tomcat Server -> Local -> Environment variables`。
3. 只把 URL 端口临时改为一个没有 MySQL 服务监听的端口，例如 `3307`：

   ```text
   jdbc:mysql://localhost:3307/spring_training_web?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
   ```

4. 保存配置，完全停止 Tomcat，再重新启动，让新环境变量进入新的 JVM 进程。
5. 使用一个新的测试用户名提交注册，例如 `db_error_test_20260817`，不要使用已经存在的用户名，否则无法确认请求是否真的走到了数据库连接阶段。
6. 在浏览器 Network 中确认：

   ```text
   Request Method: POST
   Status Code: 500 Internal Server Error
   Response: {"code":500,"message":"未预期数据库错误"}
   ```

   响应不能包含 JDBC URL、账号密码、SQL 语句或异常堆栈；详细异常只能出现在服务端控制台日志中。
7. 在 Navicat 中查询 `users`，确认没有新增 `db_error_test_20260817` 记录。
8. 测试完成后务必把端口改回 `3306`，完全停止并重新启动 Tomcat，再进行后续正常注册测试。

也可以通过 Windows 服务管理器临时停止 MySQL 来制造连接失败，但这会同时影响 Navicat；本训练优先使用错误端口方式，故障范围更小、恢复更明确。

#### 2.10 本步骤验收

按当前 IDEA 上下文路径测试；如果已改成连字符路径，应相应替换 URL：

```powershell
curl.exe -i -X POST "http://localhost:8080/01_java_web_basics/api/register" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "username=alice&password=change-this-test-password"
```

必须验证：

- 第一次注册返回 `201`。
- 相同用户名再次注册返回 `409`。
- 在当前不区分大小写的排序规则下，`Alice` 与 `alice` 不能重复注册。
- 缺少参数、空字符串和纯空格返回 `400`。
- Navicat 中能看到用户记录，`password_hash` 与输入密码明显不同。
- 停止 MySQL 或使用错误连接信息时返回 `500`，浏览器响应不暴露 SQL 和异常堆栈。
- 单条 `INSERT` 暂时使用自动提交即可；增加第二条相关写操作后再进入显式事务练习。

需要掌握：

- `request.getParameter` 的返回值和空值判断。
- `response.setStatus`、`sendError` 和 `sendRedirect` 的区别。
- `Connection`、`PreparedStatement`、`ResultSet`、生成键和 `try-with-resources`。
- 业务层提前查询与数据库唯一约束各自解决什么问题。
- 为什么密码必须使用带随机盐的慢哈希，而不能明文保存或使用普通 SHA-256。
- 为什么不能把异常堆栈直接返回给前端。

### 第 3 步：Cookie、Session 和登录

注册接口验收通过后，下一阶段先实现登录会话，不要同时开始留言板。登录阶段的目标是看懂“用户名密码校验成功后，服务端如何记住当前用户”。

#### 3.1 功能边界和接口

本阶段不引入 JWT，不把用户密码或密码哈希放进 Cookie，只使用 Servlet 容器提供的 `HttpSession`：

| 方法 | 路径 | 请求体 | 未登录/失败 | 成功 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/login` | `username`、`password` 表单字段 | 参数错误 `400`、凭据错误 `401` | `200`，创建 Session |
| `GET` | `/api/me` | 无 | `401` | `200`，返回当前用户基本信息 |
| `POST` | `/api/logout` | 无 | 可按幂等处理 | `204`，销毁 Session |

成功登录后，浏览器会自动保存容器返回的 `JSESSIONID` Cookie；后续请求携带这个 Cookie，Tomcat 才能找到服务端对应的 Session。Cookie 只保存会话标识，不保存用户密码。

#### 3.2 依赖和目录

本阶段不需要新增 Maven 依赖：Servlet API、MySQL Connector/J 和 BCrypt 已经在 Module POM 中声明。新增类建议放置为：

```text
cn.siyes.training.web/
├─ dao/
│  └─ UserDao.java                 # 增加按用户名查询密码哈希的方法
├─ exception/
│  └─ InvalidCredentialsException.java
├─ service/
│  └─ LoginService.java
└─ servlet/
   ├─ LoginServlet.java
   ├─ CurrentUserServlet.java
   └─ LogoutServlet.java
```

可以把三个 HTTP 入口合并到一个 Servlet，但本阶段建议拆开，让 URL 映射、职责和 Session 生命周期更清楚。不要让 DAO 或 Service 导入 `HttpServletResponse`。

#### 3.3 增加 DAO 查询

登录时不查询明文密码，只查询哈希：

```sql
SELECT id, username, password_hash
FROM users
WHERE username = ?
LIMIT 1;
```

不要把登录 SQL 写成同时比较 `username` 和 `password_hash`：

```sql
-- 不推荐
WHERE username = ?
  AND password_hash = ?
```

原因是用户提交的是原始密码，而数据库保存的是 BCrypt 哈希。BCrypt 每次生成哈希都会使用随机盐，同一个密码多次哈希得到的字符串也不同；应用无法把本次新生成的哈希直接用 `=` 与数据库旧哈希比较。BCrypt 的正确验证方式是先取出数据库中的完整哈希，再让 BCrypt 从哈希中读取盐和成本参数完成验证。

DAO 可以返回 `User`，也可以返回专门的登录查询对象。当前练习优先复用 `User`，但不要把 `password_hash` 放进 HTTP 响应。`PreparedStatement` 的顺序仍然是：创建、设置参数、执行、读取结果：

```java
try (PreparedStatement statement = connection.prepareStatement(sql)) {
    statement.setString(1, username);

    try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
            return null;
        }

        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash")
        );
    }
}
```

如果当前源码中还没有 `User.java`，先手动创建 `cn.siyes.training.web.model.User`。登录阶段只需要不可变的查询结果对象：

```java
package cn.siyes.training.web.model;

public class User {
    private final long id;
    private final String username;
    private final String passwordHash;

    public User(long id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
```

如果已经存在 `User` 模型，只需补齐等价的构造器和 Getter，不要把 SQL 结果临时塞进 Servlet 局部变量中。

#### 3.4 Service 校验密码

登录 Service 的输入输出边界：

```text
输入：username、password
  -> 查询用户哈希
-> 用户不存在或 BCrypt 校验失败：同样返回凭据错误
  -> 校验成功：返回用户 ID 和用户名
```

建议先确定 Service 方法签名：

```java
public User login(String username, String password)
        throws SQLException, InvalidCredentialsException {
    // 查询用户；不存在或密码校验失败时抛出 InvalidCredentialsException
    // 校验成功返回 User
}
```

`InvalidCredentialsException` 继承 `RuntimeException`，`SQLException` 继续向 Servlet 传播。Service 不创建或销毁 Session，也不设置 HTTP 状态码。

不要分别提示“用户名不存在”和“密码错误”，否则会泄露账号是否存在。可以创建：

```java
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("用户名或密码错误");
    }
}
```

BCrypt 校验示例：

```java
BCrypt.Result result = BCrypt.verifyer()
        .verify(password.toCharArray(), user.getPasswordHash());

if (!result.verified) {
    throw new InvalidCredentialsException();
}
```

数据库查询只负责根据用户名找到账号和哈希，密码校验由 Service 完成：

```text
username -> SQL 查询 password_hash
password + password_hash -> BCrypt.verifyer()
```

这样也能避免把密码验证逻辑和数据库 SQL 耦合。无论用户名不存在还是密码错误，都返回同一个 `InvalidCredentialsException`，不要向前端暴露具体失败原因。

登录 Service 只返回经过验证的用户信息，不创建 Session。Session 属于 Servlet Web 层，因为只有 Servlet 能访问 `HttpServletRequest`。

#### 3.5 登录 Servlet 和 Session

`LoginServlet` 的关键顺序：

```text
读取并校验参数
  -> 调用 LoginService
  -> 防止 Session 固定攻击：request.changeSessionId()
  -> request.getSession(true)
  -> 保存 USER_ID、USERNAME
  -> 返回 200 JSON
```

登录成功后保存最少的信息：

```java
HttpSession session = req.getSession(true);
session.setAttribute("USER_ID", user.getId());
session.setAttribute("USERNAME", user.getUsername());
```

不要保存密码、密码哈希或完整数据库对象。`changeSessionId()` 在认证成功后调用，用于避免登录前后的 Session ID 被固定复用：

```java
req.changeSessionId();
HttpSession session = req.getSession(true);
```

登录失败返回：

```json
{"code":401,"message":"用户名或密码错误"}
```

响应必须设置：

```java
resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
resp.setContentType("application/json;charset=UTF-8");
```

#### 3.6 当前用户和登出

当前用户接口必须使用 `getSession(false)`，不能因为访问 `/api/me` 就自动创建空 Session：

```java
HttpSession session = req.getSession(false);
if (session == null || session.getAttribute("USER_ID") == null) {
    // 返回 401
    return;
}
```

只把用户 ID 从 Session 取出，再通过 Service/DAO 查询当前用户基本信息。不要直接把 Session 中未经验证的对象完整返回。

登出接口也使用 `getSession(false)`，有 Session 就销毁，没有 Session 也可以返回 `204`，保持幂等：

```java
HttpSession session = req.getSession(false);
if (session != null) {
    session.invalidate();
}
resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
```

#### 3.7 `web.xml` 映射

继续沿用当前项目的 `web.xml` 显式映射方式。新增三组映射，类名以你实际创建的类为准：

```xml
<servlet>
    <servlet-name>login</servlet-name>
    <servlet-class>cn.siyes.training.web.servlet.LoginServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>login</servlet-name>
    <url-pattern>/api/login</url-pattern>
</servlet-mapping>

<servlet>
    <servlet-name>currentUser</servlet-name>
    <servlet-class>cn.siyes.training.web.servlet.CurrentUserServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>currentUser</servlet-name>
    <url-pattern>/api/me</url-pattern>
</servlet-mapping>

<servlet>
    <servlet-name>logout</servlet-name>
    <servlet-class>cn.siyes.training.web.servlet.LogoutServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>logout</servlet-name>
    <url-pattern>/api/logout</url-pattern>
</servlet-mapping>
```

不要同时用注解和 XML 映射同一个路径。现有 `ApiFilter` 会继续对这些 `/api/*` 请求设置请求编码和 JSON 响应类型。

#### 3.8 手动验收顺序

浏览器表单可以验证登录，但要观察 Cookie 和不同状态码，推荐使用同一个浏览器 DevTools Network 或 curl。先注册一个确定存在的测试用户，再按顺序执行：

```powershell
# 不存在的用户名，应返回 401，消息统一为“用户名或密码错误”
curl.exe -i -X POST "http://localhost:8080/01_java_web_basics/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "username=user_not_exists_20260818&password=wrong-test-password"

# 已存在的用户名 + 错误密码，应返回相同的 401 消息
# 将 alice 替换为已经在 users 表中存在的测试用户名
curl.exe -i -X POST "http://localhost:8080/01_java_web_basics/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "username=alice&password=wrong-test-password"

# 缺少 password，应返回 400
curl.exe -i -X POST "http://localhost:8080/01_java_web_basics/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "username=alice"

# 缺少 username，也应返回 400
curl.exe -i -X POST "http://localhost:8080/01_java_web_basics/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "password=change-this-test-password"

# 登录成功，保存 Cookie 到本地临时文件
curl.exe -i -c session.txt -X POST "http://localhost:8080/01_java_web_basics/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "username=alice&password=change-this-test-password"

# 携带 JSESSIONID 获取当前用户
curl.exe -i -b session.txt "http://localhost:8080/01_java_web_basics/api/me"

# 登出并销毁 Session
curl.exe -i -b session.txt -X POST "http://localhost:8080/01_java_web_basics/api/logout"

# 登出后再次访问，应为 401
curl.exe -i -b session.txt "http://localhost:8080/01_java_web_basics/api/me"
```

`session.txt` 只用于本地练习，里面包含会话 Cookie。请把它放在项目目录之外，或测试完成后立即删除，不要提交到 Git。

验收必须观察：

- 正确凭据登录返回 `200`，响应头包含 `Set-Cookie: JSESSIONID=...`，响应不包含密码哈希。
- 错误用户名和错误密码都返回 `401`，对外消息保持一致。
- 缺少参数返回 `400`。
- 登录后 `/api/me` 返回用户 ID 和用户名，不返回密码字段。
- 登出返回 `204`；登出后 `/api/me` 返回 `401`。
- 浏览器 Application/Storage 面板能看到 `JSESSIONID`，并能说明 Cookie 只保存标识、Session 数据在服务端。
- 删除或篡改 Cookie 后访问 `/api/me` 返回 `401`。

本阶段主动制造并记录三类错误：不存在的用户名、已存在用户名的错误密码、缺少必填参数。前两种都必须返回相同的 `401` 消息，缺参返回 `400`；不要把“用户名不存在”和“密码错误”区分到前端。

#### 3.9 当前验收记录（2026-08-18）

已通过 curl 手动验证：

- 不存在的用户名返回 `401`，消息为“用户名或密码错误”。
- 已存在用户名加错误密码返回相同的 `401` 和相同消息，没有泄露用户名是否存在。
- 分别缺少 `username`、`password` 都返回 `400`。
- 正确凭据登录返回 `200`，并通过 `Set-Cookie` 设置带 `HttpOnly` 的 `JSESSIONID`。
- 携带 Cookie 访问 `/api/me` 返回 `200`，并返回用户 ID 和用户名，不包含密码或密码哈希。
- 登出返回 `204` 且无响应体；携带原 Cookie 再访问 `/api/me` 返回 `401`。

本轮登录、Session、当前用户和登出功能已通过手动验收。浏览器显示的用户 ID 与用户名都是 `1`，与 Navicat 中 `id=1`、`username="1"` 的数据一致，不是字段取值错误。本 Module 的重点是 Servlet 请求链路和 Session 生命周期，因此不要求为 `/api/me` 额外抽象通用响应 DTO 或引入 JSON 序列化工具；当前响应已足够作为练习验收证据。`204` 响应上的 JSON `Content-Type` 是 `/api/*` Filter 统一设置造成的冗余，不影响本轮功能结果。

### 第 4 步：Filter

本步骤专门练习 Servlet Filter。可以把它类比成前端路由守卫或 Axios 拦截器，但 Filter 运行在 Java Web 容器中，能够包住一次 Servlet 请求；它不是 Servlet，也不负责某一个具体业务接口。

#### 4.1 Filter 的三个核心 API

Filter 使用的是已经引入的 `jakarta.servlet-api`，不需要增加 Maven 依赖。先熟悉这几个类型：

| API | 作用 |
| --- | --- |
| `Filter` | Filter 的生命周期接口，主要实现 `doFilter` |
| `ServletRequest` / `ServletResponse` | 通用请求和响应类型；需要 HTTP 能力时转成 `HttpServletRequest` / `HttpServletResponse` |
| `FilterChain` | 代表后续 Filter 和目标 Servlet；调用 `chain.doFilter` 才会继续请求 |
| `FilterConfig` | 读取 XML 中配置的 Filter 名称和初始化参数；本步骤暂时不需要自定义参数 |

最小结构如下，先理解参数，再填写业务代码：

```java
public class ExampleFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        // chain.doFilter 前：进入目标 Servlet 之前
        chain.doFilter(request, response);
        // chain.doFilter 后：目标 Servlet 返回之后
    }
}
```

`init(FilterConfig)` 和 `destroy()` 可以在需要初始化资源或释放资源时覆盖；本阶段不要在 Filter 中打开数据库连接，也不要把 Session、DAO 和业务规则都塞进同一个 Filter。

#### 4.2 `chain.doFilter` 的执行模型

假设 XML 中的顺序是 `ApiFilter -> RequestLogFilter -> LoginRequiredFilter -> Servlet`，一次请求的调用关系是：

```text
ApiFilter 前置代码
  -> RequestLogFilter 前置代码
    -> LoginRequiredFilter 检查
      -> Servlet
    <- LoginRequiredFilter 后置代码
  <- RequestLogFilter 后置代码
<- ApiFilter 后置代码
```

每个 Filter 都必须明确选择以下两种路径之一：

1. **放行**：调用 `chain.doFilter(request, response)`，后续 Filter 或 Servlet 才会执行。
2. **拦截**：直接设置响应状态并写回响应，然后 `return`，不能再调用 `chain.doFilter`。

因此，`chain.doFilter` 前适合做编码设置、权限检查前的准备和开始计时；后面适合记录耗时、统一补充日志或观察 Servlet 是否抛出异常。需要确保无论 Servlet 正常返回还是抛异常，耗时日志都能输出时，应使用 `try/finally` 包住 `chain.doFilter`：

```java
long start = System.nanoTime();
try {
    chain.doFilter(request, response);
} finally {
    long elapsedNanos = System.nanoTime() - start;
    // 记录耗时；不要在这里覆盖 Servlet 已经写好的业务响应
}
```

#### 4.3 第一个 Filter：`RequestLogFilter`

目标是记录请求方法、路径和耗时。先把通用参数转换为 HTTP 类型：

```java
HttpServletRequest httpRequest = (HttpServletRequest) request;
HttpServletResponse httpResponse = (HttpServletResponse) response;

String method = httpRequest.getMethod();
String path = httpRequest.getRequestURI();
String query = httpRequest.getQueryString();
long start = System.nanoTime();
try {
    chain.doFilter(request, response);
} finally {
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
    System.out.printf("%s %s query=%s status=%d cost=%dms%n",
            method, path, query, httpResponse.getStatus(), elapsedMillis);
}
```

逐项理解：

- `getMethod()` 得到 `GET`、`POST` 等 HTTP 方法。
- `getRequestURI()` 得到上下文路径之后的 URI，例如 `/01_java_web_basics/api/login`。
- `getQueryString()` 只得到 `?` 后的查询参数，没有查询参数时可能是 `null`。
- `getStatus()` 能观察后续 Servlet 最终设置的状态码。
- 不要记录密码、`password_hash`、Session ID 或完整 `Cookie`，日志内容必须是可用于排查但不会泄露凭据的信息。

先访问 `/health`、`/api/login` 和一个不存在的路径，确认每次请求都有一条日志，并比较正常返回、参数错误和 404 的状态码与耗时。

#### 4.4 第二个 Filter：`LoginRequiredFilter`

目标是保护 `/api/messages/*`。它只判断“是否已经登录”，不查询数据库，也不负责校验密码。登录状态来自登录 Servlet 写入的 Session 属性：`USER_ID`。

核心判断顺序：

```text
ServletRequest/Response 转成 Http 类型
  -> request.getSession(false)
  -> Session 不存在，或 USER_ID 不存在：返回 401 并 return
  -> 否则 chain.doFilter 放行
```

参考骨架：

```java
HttpServletRequest httpRequest = (HttpServletRequest) request;
HttpServletResponse httpResponse = (HttpServletResponse) response;
HttpSession session = httpRequest.getSession(false);

if (session == null || session.getAttribute("USER_ID") == null) {
    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpResponse.getWriter().print(createResponse(401, "无登录用户"));
    return;
}

chain.doFilter(request, response);
```

注意：`getSession(false)` 在没有 Session 时返回 `null`，不会创建新 Session；这里绝对不能使用 `getSession(true)`，否则未登录访问也会被你自己创建出一个空 Session，失去判断意义。拦截响应可以复用当前项目的 JSON 响应方法，但不要把登录密码写入响应或日志。

#### 4.5 Listener：练习容器生命周期回调

Filter 是请求经过时执行的“包裹逻辑”，Listener 则是 Tomcat 在生命周期事件发生时主动调用的回调接口。它没有 `chain.doFilter`，也不应该被当作另一个 Servlet 使用。

本步骤先练习两个最容易观察的 Listener：

| Listener | 触发时机 | 适合练习的内容 |
| --- | --- | --- |
| `ServletContextListener` | Web 应用启动、停止 | 应用级初始化和释放；整个应用通常只有一个 Context |
| `HttpSessionListener` | Session 创建、销毁 | 观察 Session 生命周期和在线 Session 数量变化 |
| `ServletRequestListener` | 请求对象创建、销毁 | 观察请求生命周期；不负责拦截或修改请求 |

它们都已经包含在 `jakarta.servlet-api` 中，不需要新增依赖。建议新建目录：

```text
src/main/java/cn/siyes/training/web/listener/
```

先手写一个只打印生命周期事件的 `TrainingContextListener`：

```java
package cn.siyes.training.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class TrainingContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute("APP_START_TIME", System.currentTimeMillis());
        System.out.println("Web 应用启动完成");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        System.out.println("Web 应用即将停止");
    }
}
```

再手写一个 `TrainingSessionListener`，用实例字段记录当前由容器创建且尚未销毁的 Session 数量：

```java
package cn.siyes.training.web.listener;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class TrainingSessionListener implements HttpSessionListener {
    private int activeSessionCount;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        activeSessionCount++;
        System.out.println("Session 创建，当前数量=" + activeSessionCount);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        activeSessionCount--;
        System.out.println("Session 销毁，当前数量=" + activeSessionCount);
    }
}
```

作为扩展练习，再补一个 `ServletRequestListener`，专门观察一次 HTTP 请求对象的生命周期。它和 `RequestLogFilter` 的职责不同：Filter 能拿到 request、response 并决定是否调用后续链路；`ServletRequestListener` 只会收到请求创建和销毁事件，没有 `HttpServletResponse`，不能拦截请求，也不能调用 `chain.doFilter`。这个扩展不改变本 Module 已完成的验收结论。

手写 `TrainingRequestListener`：

```java
package cn.siyes.training.web.listener;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

public class TrainingRequestListener implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        System.out.printf("request创建 method=%s uri=%s%n",
                request.getMethod(), request.getRequestURI());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        System.out.printf("request销毁 method=%s uri=%s%n",
                request.getMethod(), request.getRequestURI());
    }
}
```

在 `web.xml` 中和其他 Listener 同级注册：

```xml
<listener>
    <listener-class>cn.siyes.training.web.listener.TrainingRequestListener</listener-class>
</listener>
```

手动验证：

1. 重启 Tomcat 后访问 `/health`，观察一组 `request创建` 和 `request销毁` 日志。
2. 再访问 `/api/login`、`/api/me` 或一个不存在的路径，比较不同请求的 method、URI 和回调次数。
3. 在 `RequestLogFilter` 中打印一条前置日志和一条后置日志，结合控制台观察大致顺序：请求 Listener 创建 -> Filter 前置 -> Servlet 或后续路由 -> Filter 后置 -> 请求 Listener 销毁。具体容器日志之间可能夹杂 Tomcat 自身日志，不要把日志先后顺序当成业务返回值。
4. 暂时删除该 Listener 的 XML 注册并重启，确认只有完成容器注册后才会收到回调。

需要特别注意：`requestDestroyed` 表示容器完成了这个请求对象的生命周期，不等于“业务一定返回了 200”；请求可能是 `401`、`404` 或 `500`。如果要读取最终 HTTP 状态码、统一拦截或修改响应，应使用 Filter，而不是 `ServletRequestListener`。不要把请求对象保存到静态字段或跨请求集合中，请求结束后它就不再属于当前业务上下文。

这里重点理解：Listener 对象由 Tomcat 创建和管理；回调方法不是你主动调用，而是容器在事件发生时调用。当前练习只做日志和简单计数，不要在 Listener 中保存数据库连接、执行业务 SQL 或堆积全局业务状态。真实项目中如果需要并发安全的统计，应使用合适的并发类型；本练习先关注生命周期，不把它扩展成监控系统。

使用本项目已有的 `web.xml` 显式注册。把下面两个 `<listener>` 放在 `<web-app>` 内，和现有的 Servlet、Filter 声明同级；不要把它们写进某个 `<servlet>` 或 `<filter>` 节点，也不要写到 `</web-app>` 外。为了便于阅读，建议集中放在 Filter 映射附近，并在修改后重启 Tomcat：

```xml
<listener>
    <listener-class>cn.siyes.training.web.listener.TrainingContextListener</listener-class>
</listener>
<listener>
    <listener-class>cn.siyes.training.web.listener.TrainingSessionListener</listener-class>
</listener>
```

手动验证顺序：

1. 重启 Tomcat，观察 `contextInitialized` 日志；停止 Tomcat，观察 `contextDestroyed` 日志。
2. 第一次登录或其他会创建 Session 的请求，观察 `sessionCreated`；登出、Session 超时或重启应用后，观察 `sessionDestroyed` 的触发情况。
3. 在 `contextInitialized` 中临时设置一个 Context 属性，再在 Servlet 中使用 `getServletContext().getAttribute("APP_START_TIME")` 读取，理解应用级共享数据和 Session 数据的作用域差异。
4. 暂时注释 Listener 的 XML 注册并重启，确认类存在不等于容器会回调，必须完成注册。

验收时能说明以下区别即可：Listener 监听什么事件、谁负责创建 Listener、`ServletContext`、`HttpSession` 和单次 `ServletRequest` 的作用域有什么不同、为什么 Listener 不能替代 Filter 或 Servlet。这个练习的目标是熟悉 API 和生命周期，不要求额外编写测试类或记录复杂异常矩阵。

#### 4.6 XML 映射和执行顺序

本项目继续使用 `src/main/webapp/WEB-INF/web.xml` 显式配置，不要同时为同一个 Filter 使用 `@WebFilter` 和 XML 映射。Filter 映射至少包括：

```xml
<filter>
    <filter-name>requestLogFilter</filter-name>
    <filter-class>cn.siyes.training.web.filter.RequestLogFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>requestLogFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>

<filter>
    <filter-name>loginRequiredFilter</filter-name>
    <filter-class>cn.siyes.training.web.filter.LoginRequiredFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>loginRequiredFilter</filter-name>
    <url-pattern>/api/messages/*</url-pattern>
</filter-mapping>
```

`RequestLogFilter` 使用 `/*` 是为了观察整个 Web 应用；`LoginRequiredFilter` 只保护留言板路径，不能误伤 `/api/login`、`/api/register` 和 `/api/me`。当前 `web.xml` 与接口约定已经统一为 `/api/messages/*`（复数）；后续新增 Servlet 映射时也必须保持一致，否则访问的 URL 与 Filter 实际保护范围会不一致。

通常同一 URL 的 Filter 执行顺序按 `web.xml` 中的映射声明顺序确定。建议排列为：`ApiFilter -> RequestLogFilter -> LoginRequiredFilter`。这样编码和 JSON 响应设置先完成，日志能观察最终状态，登录 Filter 最后决定放行还是拦截。不要依赖类名的字母顺序推断执行顺序。

#### 4.7 手动验收与主动制造错误

1. 未登录访问 `/api/messages/test`：应返回 `401`，并且目标 Servlet 不应打印“已进入”日志。
2. 登录后携带同一个 `JSESSIONID` 访问：Filter 应调用 `chain.doFilter`；如果留言 Servlet 还未创建，可以暂时观察结果变为目标路径的 `404`，这反而证明请求已经放行。
3. 登出后再次访问：应恢复为 `401`。
4. 访问 `/api/login`、`/api/register`、`/api/me`：不应被 `LoginRequiredFilter` 拦截。
5. 删除 XML 中 `RequestLogFilter` 的映射或故意把路径写成 `/api/message/*`，重新部署并比较日志/状态码，记录“配置范围错误”这个主动制造的故障，然后恢复正确配置。
6. 在 `RequestLogFilter` 中临时让 `chain.doFilter` 抛出异常，确认 `finally` 仍然输出耗时；测试后删除临时代码。

验收时必须能用自己的话说明：Filter 为什么能在 Servlet 前后执行、`chain.doFilter` 不调用会发生什么、`getSession(false)` 与 `getSession(true)` 的区别、以及为什么登录保护不能放在 DAO 或 `LoginService` 中。

#### 4.8 当前验收记录（2026-08-18）

- `ApiFilter`、`RequestLogFilter`、`LoginRequiredFilter` 已按 XML 映射顺序进入 Filter 链。
- 未登录访问 `/api/messages/6` 返回 `401`，说明 `LoginRequiredFilter` 在目标 Servlet 之前终止了请求。
- 登录后访问同一路径返回 Tomcat `404`，说明 Session 中存在 `USER_ID`，`LoginRequiredFilter` 已调用 `chain.doFilter` 放行；此时尚未配置留言 Servlet，因此容器在后续路由阶段返回 `404`。
- `RequestLogFilter` 在 `finally` 中正确记录请求方法、URI、查询参数、最终状态码和耗时，能够同时观察 Filter 拦截产生的 `401` 和后续路由产生的 `404`。
- `web.xml` 中登录保护路径已统一为 `/api/messages/*`。
- Listener 验收已通过：Tomcat 启动时观察到 `contextInitialized`，Session 创建时观察到数量从 `1`、`2` 增加，Session 销毁时观察到数量回到 `1`、`0`；Tomcat 停止时观察到 `contextDestroyed`。这证明 Listener 已完成注册，并能接收 Web 应用和 Session 生命周期回调。

以上结果完成了 Filter 和 Listener 的核心机制验收。后续留言板、事务、WAR 部署和核心功能均已完成，本 Module 正式验收通过。

### 第 5 步：扩展 JDBC、留言板和事务

本步骤先完成一个最小留言板，不加入分页、评论、点赞等扩展。复用已经完成的 `ConnectionFactory`、DAO、Session 和 Filter。

#### 5.1 在 Navicat 创建 `messages` 表

在 Navicat 中打开 `spring_training_web` 数据库的查询窗口，执行：

```sql
CREATE TABLE messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
```

执行后在 Navicat 刷新表，确认字段和外键存在，再把最终确认过的 SQL 手动追加到 `01-java-web-basics/src/main/resources/schema.sql`。`user_id` 表示发帖人，不能从请求参数信任，必须从 Session 的 `USER_ID` 读取。

#### 5.2 两个最小接口

```text
POST /api/messages
请求：表单字段 content
登录：必须携带 JSESSIONID
成功：201

GET /api/messages
登录：必须携带 JSESSIONID
成功：200，返回留言列表
```

建议先在 `web.xml` 增加一个 `MessageServlet`，映射 `/api/messages`，再由 `doPost` 和 `doGet` 分别处理两种方法。`LoginRequiredFilter` 已保护 `/api/messages/*`，所以未登录请求会在 Servlet 之前得到 `401`。

#### 5.3 推荐的分层和实现顺序

1. `Message`：保存 `id`、`userId`、`content`、`createdAt`，只作为查询结果对象。
2. `MessageDao`：只负责 SQL 和 JDBC 资源，不读取 `HttpSession`，建议提供 `insert(Connection, long userId, String content)` 和 `findAll(Connection)`。
3. `MessageService`：校验内容不能为空、长度不超过 500，并调用 DAO；不要在 DAO 中设置 HTTP 状态码。
4. `MessageServlet`：读取请求参数和 Session 用户 ID，调用 Service，设置 `201`、`200` 或错误状态并写响应。

单条新增的 JDBC 顺序保持一致：

```text
获取 Connection
  -> PreparedStatement 绑定 content 和 user_id
  -> executeUpdate
  -> 读取生成的 id（需要时）
  -> try-with-resources 自动关闭资源
```

SQL 必须使用占位符，不能拼接用户输入：

```sql
INSERT INTO messages (user_id, content) VALUES (?, ?)
```

`PreparedStatement` 会把参数当作数据而不是 SQL 语句的一部分，既能避免注入，也能避免手动拼接引号和特殊字符。查询列表时使用：

```sql
SELECT id, user_id, content, created_at
FROM messages
ORDER BY created_at DESC, id DESC;
```

#### 5.4 最小事务练习

##### 5.4.1 事务要解决什么问题

不要为了形式上使用事务而包裹单条 `INSERT`。本阶段把“发布留言”定义为两个必须同时成功的动作：

```sql
INSERT INTO messages (user_id, content) VALUES (?, ?);
INSERT INTO message_logs (message_id, action) VALUES (?, 'CREATE');
```

如果第一条成功、第二条失败，数据库会只留下一条没有日志的留言，业务就只完成了一半。事务保证结果只能是：

```text
两条 SQL 都成功 -> commit，两张表都有本次记录
任意一条失败   -> rollback，两张表都没有本次记录
```

JDBC Connection 默认 `autoCommit=true`，每条 SQL 执行后会自动提交。要把两条 SQL 组成一个事务，必须先执行 `connection.setAutoCommit(false)`，等两条都成功后再 `commit()`。

##### 5.4.2 在 Navicat 创建日志表

在 `spring_training_web` 的查询窗口执行：

```sql
CREATE TABLE message_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_message_logs_message
        FOREIGN KEY (message_id) REFERENCES messages(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
```

刷新表并确认外键存在后，把最终 SQL 手动追加到 `src/main/resources/schema.sql`。不要先在 Navicat 手动插入日志；后面要用 Java 请求验证事务。

##### 5.4.3 事务由 Service 控制

事务边界放在 `MessageService`，因为 Service 负责把两条写操作组成一个完整业务动作：

```text
MessageServlet
  -> MessageService：获取同一个 Connection、开始事务、提交或回滚
    -> MessageDao：插入 messages
    -> MessageLogDao：插入 message_logs
```

你当前 `MessageDao` 已经接收 `Connection` 参数，这一点是正确的。新增 `MessageLogDao` 时也必须接收同一个 Connection，不能在两个 DAO 内部分别调用 `ConnectionFactory.getConnection()`，否则它们属于两个事务，无法一起回滚。

`MessageLogDao` 可以先只写一个方法：

```java
public int insertCreateLog(Connection connection, long messageId) throws SQLException {
    String sql = "INSERT INTO message_logs (message_id, action) VALUES (?, ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, messageId);
        statement.setString(2, "CREATE");
        return statement.executeUpdate();
    }
}
```

此处 try-with-resources 关闭的是 `PreparedStatement`，不会关闭外部传入的 Connection。Connection 最后由 Service 关闭。

##### 5.4.4 取得第一条 SQL 生成的留言 ID

你当前 `MessageDao.insert()` 返回 `executeUpdate()` 的结果。正常插入时它是受影响行数 `1`，不是新留言的 `id`。第二条日志 SQL 需要真实的 `message_id`，所以要让 DAO 读取自动生成主键：

```java
public long insertAndReturnId(Connection connection, long userId, String content)
        throws SQLException {
    String sql = "INSERT INTO messages (user_id, content) VALUES (?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(
            sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setLong(1, userId);
        statement.setString(2, content);

        int affectedRows = statement.executeUpdate();
        if (affectedRows != 1) {
            throw new SQLException("新增留言失败，受影响行数不为 1");
        }

        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
    }

    throw new SQLException("新增留言成功但未取得生成的主键");
}
```

这里需要 `java.sql.Statement`。`Statement.RETURN_GENERATED_KEYS` 告诉 JDBC 驱动保留数据库生成的主键，`getGeneratedKeys()` 再读取它。

##### 5.4.5 在 Service 写事务代码

建议新增 `publish(long userId, String content)`，按下面骨架手动完成：

```java
public void publish(long userId, String content) throws SQLException {
    MessageDao messageDao = new MessageDao();
    MessageLogDao messageLogDao = new MessageLogDao();

    try (Connection connection = ConnectionFactory.getConnection()) {
        connection.setAutoCommit(false);
        try {
            long messageId = messageDao.insertAndReturnId(connection, userId, content);
            messageLogDao.insertCreateLog(connection, messageId);

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        }
    }
}
```

逐项理解：

- `setAutoCommit(false)`：开始手动控制提交；必须在第一条 SQL 之前调用。
- `commit()`：只有两个 DAO 都执行成功后才调用。
- `rollback()`：第二条或第一条 SQL 抛出 `SQLException` 时撤销当前事务内的修改。
- `throw e`：回滚后继续把异常交给 Servlet，Servlet 才会返回 `500`，不能错误地返回 `201`。
- 外层 try-with-resources：无论成功或失败最终都会关闭 Connection；关闭连接不能替代明确的 `commit` 或 `rollback`。

当前项目每次都创建并立即关闭物理 Connection，所以这版代码不额外恢复 `autoCommit`，便于先理解事务主线。后续学习连接池时，Connection 会被归还并复用，届时必须在归还前谨慎恢复 `autoCommit` 等连接状态。

在 `MessageServlet.doPost()` 中，将原来的单次 `messageService.insert(...)` 改为调用 `publish(...)`。空内容的 `400` 校验仍放在事务之前；成功仍返回 `201`，`SQLException` 仍返回 `500`。

##### 5.4.6 在 Navicat 验证提交

先查询两张表并记住当前记录：

```sql
SELECT id, user_id, content, created_at FROM messages ORDER BY id DESC;
SELECT id, message_id, action, created_at FROM message_logs ORDER BY id DESC;
```

登录后提交内容 `tx-success-20260818`。预期 HTTP 为 `201`。刷新 Navicat 后应同时看到：

- `messages` 新增一行。
- `message_logs` 新增一行，`action` 是 `CREATE`。
- `message_logs.message_id` 等于新留言的 `messages.id`。

##### 5.4.7 主动制造失败并验证回滚

临时把 `MessageLogDao` SQL 中的表名改为不存在的 `message_logs_error`，然后提交内容 `tx-rollback-20260818`。预期第二条 SQL 抛出异常，接口返回 `500`。

在 Navicat 查询：

```sql
SELECT * FROM messages WHERE content = 'tx-rollback-20260818';
SELECT * FROM message_logs WHERE action = 'CREATE' ORDER BY id DESC;
```

第一句必须返回 0 行，这才证明第一条 `messages` 插入被回滚了。之后立即把表名恢复为 `message_logs`，再提交一次正常内容确认功能恢复。临时错误只用于本地训练，不要提交到 Git。

DAO 不要自行 `commit`、`rollback` 或关闭 Connection，否则 Service 无法控制两条写操作是否作为一个整体成功。

#### 5.5 手动验收顺序

1. 未登录 `POST /api/messages`：返回 `401`，Servlet 不应执行。
2. 登录后提交空内容：返回 `400`，数据库不新增记录。
3. 登录后提交正常内容：返回 `201`，Navicat 能看到 `user_id`、`content`、`created_at`。
4. 登录后 `GET /api/messages`：返回 `200`，能看到按时间倒序排列的留言。
5. 使用包含单引号、中文和 HTML 字符的内容测试，确认请求成功且 SQL 没有报错。
6. 制造第二条写入失败，确认事务回滚，`messages` 和 `message_logs` 都没有只成功一半。

本步骤的重点是“Session 用户 ID -> Service -> DAO -> MySQL”的数据流，以及事务为什么必须覆盖相关写操作；先完成这条主链路，再考虑响应结构和页面优化。

#### 5.6 当前验收记录（2026-08-18）

- 登录后提交空内容返回 `400`，消息为“信息为空”，参数校验在写入数据库前生效。
- 登录后提交正常留言返回 `201`，消息为“发送成功”。
- 登录后查询 `/api/messages` 返回 `200`，能够读取已写入的留言内容。
- 查询结果中出现两条相同留言，是前面两次成功提交相同内容造成的正常数据结果，不代表 `GET` 重复读取。
- 事务成功路径已通过：`messages` 新增 `id=6` 的留言，同时 `message_logs` 新增日志且 `message_id=6`、`action=CREATE`，两条记录的创建时间一致，证明生成主键传递和共同提交正确。
- `messages.id` 从 `3` 跳到 `6` 不代表数据丢失；MySQL 的 `AUTO_INCREMENT` 在插入失败或事务回滚后通常不会回收已经分配的序号，主键只要求唯一，不要求连续。
- 事务回滚路径已通过：将第二条 SQL 的表名临时改为 `message_logs_error` 后，`POST /api/messages` 返回 `500`，请求日志记录最终状态 `500`；Navicat 中 `messages` 仍以 `id=6` 为最新记录，`message_logs` 也仍只有 `message_id=6`，证明本次第一条留言插入已被 `rollback()` 撤销。
- 故障恢复复测已通过：将表名恢复为 `message_logs` 后，`POST /api/messages` 重新返回 `201`；Navicat 中 `messages.id=8` 与 `message_logs.message_id=8` 同时新增且时间一致，证明正常事务功能已恢复。回滚测试分配过的 `id=7` 没有被复用，属于 MySQL 自增序列的正常行为。

留言板的基本新增、查询、事务提交、回滚及故障恢复链路已通过当前手动验收。进入第 6 步，补充最小测试和 Maven 构建认知；它们属于辅助工程能力，不替代对 Servlet、Filter、Listener、JDBC 和事务机制的理解。

### 第 6 步：辅助测试、Maven 构建和最终复盘

本阶段的核心已经通过浏览器、curl 和 Navicat 手动验证。这里保留一个最小 JUnit 和 Maven 构建练习，用来认识测试与打包在工程中的位置，不要求把所有手工测试重写一遍，也不把测试命令作为本 Module 的主要学习成果。实际开发中可以借助 AI 生成测试，但仍要知道测试覆盖的对象和边界。

#### 6.1 手动测试与自动化测试的区别

| 类型 | 本 Module 的例子 | 优点 | 当前边界 |
| --- | --- | --- | --- |
| 手动 HTTP 验收 | 登录、Cookie、Filter、`401`、事务回滚 | 能验证 Tomcat 到 MySQL 的完整链路 | 每次都要人工操作 |
| JUnit 自动化测试 | 留言为空、全空格、长度边界 | 快、可重复、失败位置明确 | 默认不启动 Tomcat、不发送 HTTP、不访问 MySQL |
| Maven 构建 | `mvn clean test`、`mvn package` | 从源码重复编译、测试并产出 WAR | `package` 不等于部署和启动 |

本阶段不引入 Mockito、嵌入式 Servlet 容器或测试数据库。Servlet、Session、Filter 和事务使用现有手动证据；JUnit 先覆盖一个由你自己编写、可脱离容器运行的业务规则。

#### 6.2 最小 JUnit 练习：留言内容校验

当前空内容判断写在 `MessageServlet` 中，必须启动 Tomcat 才能测试。为了让规则可以单独测试，手动创建：

```text
src/main/java/cn/siyes/training/web/validation/MessageValidator.java
src/test/java/cn/siyes/training/web/validation/MessageValidatorTest.java
```

生产代码保持简单：

```java
package cn.siyes.training.web.validation;

public final class MessageValidator {
    private static final int MAX_CONTENT_LENGTH = 500;

    private MessageValidator() {
    }

    public static boolean isValidContent(String content) {
        return content != null
                && !content.isBlank()
                && content.length() <= MAX_CONTENT_LENGTH;
    }
}
```

然后把 `MessageServlet` 原来的空内容判断手动替换为：

```java
if (!MessageValidator.isValidContent(content)) {
    writeResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "信息为空或超过 500 个字符");
    return;
}
```

测试代码：

```java
package cn.siyes.training.web.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageValidatorTest {

    @Test
    void acceptsNormalContent() {
        assertTrue(MessageValidator.isValidContent("第一条留言"));
    }

    @Test
    void rejectsNullEmptyAndBlankContent() {
        assertFalse(MessageValidator.isValidContent(null));
        assertFalse(MessageValidator.isValidContent(""));
        assertFalse(MessageValidator.isValidContent("   "));
    }

    @Test
    void acceptsExactly500Characters() {
        assertTrue(MessageValidator.isValidContent("a".repeat(500)));
    }

    @Test
    void rejectsMoreThan500Characters() {
        assertFalse(MessageValidator.isValidContent("a".repeat(501)));
    }
}
```

这里练习三个测试概念：正常值、无效值、边界值。`@Test` 表示测试方法；`assertTrue` 和 `assertFalse` 表达预期。如果生产代码以后误把上限改成 499 或去掉空值判断，对应测试会失败。

`org.junit.jupiter:junit-jupiter` 已在 Module POM 中以 `test` 作用域声明，不需要新增依赖。`test` 表示它只参与测试编译和运行，不会作为运行依赖打进最终 WAR。

#### 6.3 可选：`mvn clean test` 具体做什么

本小节可以快速浏览或暂时跳过，不影响本 Module 的 Servlet、Filter、Listener、JDBC 和事务主线。保留它是为了以后需要时能看懂 Maven 如何调用 JUnit。

在项目根目录执行：

```powershell
mvn -pl 01-java-web-basics clean test
```

也可以先进入 Module 再执行：

```powershell
cd 01-java-web-basics
mvn clean test
```

根目录的 `-pl 01-java-web-basics` 表示只选择这个 Module。以后根项目加入更多 Module 时，不会连其他阶段一起测试。

Maven 按生命周期依次执行：

```text
clean
  -> 删除旧 target，避免旧 class 或旧 WAR 干扰结果
compile
  -> 编译 src/main/java 到 target/classes
test-compile
  -> 编译 src/test/java 到 target/test-classes
test
  -> Surefire 启动 JUnit 5 并执行 *Test 类
```

成功时重点观察：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- `Failures`：断言结果与预期不一致，例如期望 `false` 却返回 `true`。
- `Errors`：测试执行时抛出未预期异常，例如 `NullPointerException`。
- 详细报告位于 `01-java-web-basics/target/surefire-reports/`。

`mvn clean test` **不会**启动 Tomcat、不会自动发送 `/api/messages` 请求，也不会验证真实 MySQL 事务。那些链路已经由本阶段的手动验收覆盖；后续学习 Spring Boot 测试时再引入接口和数据库测试。

如果想顺便理解测试如何发现回归，可以把 `MAX_CONTENT_LENGTH` 临时改成 `499`，观察失败后恢复为 `500`；这一步不是本 Module 的必做项。

#### 6.4 可选：`mvn package` 具体做什么

测试通过后，在根目录执行：

```powershell
mvn -pl 01-java-web-basics package
```

`package` 位于 `test` 之后，因此它会再次执行编译和测试，然后根据 Module 的 `<packaging>war</packaging>` 生成：

```text
01-java-web-basics/target/01-java-web-basics.war
```

WAR 是交给 Tomcat 部署的应用包，大致包含：

```text
WEB-INF/classes/    编译后的项目类和资源
WEB-INF/lib/        MySQL 驱动、BCrypt 等运行依赖
WEB-INF/web.xml     Servlet 和 Filter 映射
WEB-INF/views/      JSP 页面
```

`jakarta.servlet-api` 使用 `provided` 作用域，因此不会重复打入 `WEB-INF/lib`，运行时由 Tomcat 10.1 提供。JUnit 使用 `test` 作用域，也不会进入 WAR。

检查包内容：

```powershell
jar tf 01-java-web-basics/target/01-java-web-basics.war
```

`mvn package` 只生成 WAR，不会自动复制到 Tomcat、不会启动服务器。最终仍需用 IDEA 的 Tomcat 配置部署该 Artifact，或手动把 WAR 放入 Tomcat `webapps`，再访问 `/health` 验证打包后的应用。

也可以用一个命令完成“清理旧产物、测试、打包”：

```powershell
mvn -pl 01-java-web-basics clean package
```

如果执行本命令，建议先保留默认测试阶段；使用 `-DskipTests` 时只能说明 WAR 生成成功，不能说明测试通过。两者的区别知道即可，不需要反复执行。

#### 6.5 辅助执行顺序和记录内容

1. 先完成 Listener 的手写、注册和回调观察。
2. 如果需要熟悉 Maven，再执行 `clean test` 或 `package`，理解其用途即可。
3. 部署新 WAR，复测 `/health` 和一个登录后的 `/api/messages` 请求。
4. 如需复盘，可在 `docs/ACCEPTANCE.md` 简要记录关键结果；不要求整理完整命令清单、异常矩阵或故障流水账。

本 Module 已完成数据库连接失败、Filter 拦截、参数错误和事务回滚等异常路径，不要求为了数量重复制造同类故障。

#### 6.6 当前验收记录（2026-08-18）

- IDEA 运行 `MessageValidatorTest`：4 个测试全部通过，进程退出码为 `0`。
- 根目录执行 `mvn -pl 01-java-web-basics clean test`：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，并输出 `BUILD SUCCESS`。
- 临时把最大长度改为 `499` 后重新测试：`acceptsExactly500Characters()` 按预期失败，输出 `expected: <true> but was: <false>`；恢复为 `500` 后再次测试，4 个测试全部通过并输出 `BUILD SUCCESS`。这证明测试能够发现业务规则回归，而不是无条件通过。
- 执行 `mvn -pl 01-java-web-basics package`：测试通过并成功生成 `01-java-web-basics/target/01-java-web-basics.war`。
- 执行 `jar tf 01-java-web-basics/target/01-java-web-basics.war`：确认 WAR 包含 `WEB-INF/classes/` 等 Web 应用目录。
- IDEA Tomcat Deployment 已从 `war exploded` 切换为 Maven 生成的 `target/01-java-web-basics.war`，实际 Application context 为 `/01_java_web_basics_war`。
- 部署后访问 `/01_java_web_basics_war/health` 返回 `200` 和 `{"status":"ok"}`。
- 在新 Context Path 下重新登录返回 `200`，响应设置了 Path 为 `/01_java_web_basics_war` 的新 `JSESSIONID`；随后访问 `/01_java_web_basics_war/api/messages` 返回 `200` 并读取到留言列表。

WAR 构建和部署复测已通过。后续复盘以用户能否用自己的话解释 Servlet、Filter、Listener、Session、JDBC 和事务边界为主；构建与测试记录只作为辅助证据。

#### 6.7 在 IDEA Tomcat 中部署 Maven 生成的 WAR

当前 `tomcat-javaweb` 运行配置部署的是 `01-java-web-basics:war exploded`。它适合日常开发，但不是刚才由 Maven 生成的 `target/01-java-web-basics.war`。最终构建验收临时改为部署 WAR 文件：

1. 停止当前 Tomcat，确认 IDEA 控制台中的服务器进程已经结束。
2. 打开 `Run -> Edit Configurations...`。
3. 选择 `Tomcat Server -> Local -> tomcat-javaweb`。
4. 打开 `Deployment` 标签页，选中现有的 `01-java-web-basics:war exploded`，点击 `-` 暂时移除，避免两个应用使用相同 Context Path。
5. 点击 `+`，选择 `External Source...`，指定：

```text
E:\本地项目\java-project\spring-training\01-java-web-basics\target\01-java-web-basics.war
```

6. 将 `Application context` 设置为原来使用的：

```text
/01_java_web_basics
```

7. 点击 `Apply`、`OK`，重新启动 `tomcat-javaweb`。继续使用同一个 Run Configuration，可以保留已经设置的 JDBC 环境变量。
8. 在启动日志中确认没有 `SEVERE`、类找不到或数据库配置缺失，并确认 WAR 部署完成。

如果 IDEA 的 `+` 菜单没有 `External Source`，可以在 Deployment 中选择 `Artifact -> 01-java-web-basics:war`；但最终仍要确认其输出指向 Maven 构建后的 WAR。不要同时保留 `war exploded` 和 WAR 且设置相同 Context Path。

先验证健康接口：

```text
http://localhost:8080/01_java_web_basics/health
```

预期 HTTP `200`，响应：

```json
{"status": "ok"}
```

WAR 重新部署后，旧 `JSESSIONID` 对应的服务端 Session 已经失效，必须重新登录。可以使用浏览器访问：

```text
http://localhost:8080/01_java_web_basics/login
```

登录成功后，在同一个浏览器中访问：

```text
http://localhost:8080/01_java_web_basics/api/messages
```

预期 HTTP `200` 并返回留言列表。也可以在 PowerShell 使用 Cookie 文件验证，先把示例用户名和密码替换为本地测试账号：

```powershell
$cookiePath = Join-Path $env:TEMP "spring-training-war-session.txt"

curl.exe -i -c $cookiePath -X POST `
  "http://localhost:8080/01_java_web_basics/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data "username=替换为测试用户名&password=替换为测试密码"

curl.exe -i -b $cookiePath `
  "http://localhost:8080/01_java_web_basics/api/messages"

Remove-Item -LiteralPath $cookiePath
```

最终需要保存三项证据：Tomcat 成功部署 WAR 的日志、`/health` 的 `200`、重新登录后 `/api/messages` 的 `200`。常见问题判断：

- `404`：优先检查 Deployment 中的 Application context 是否为 `/01_java_web_basics`。
- `401`：WAR 重部署后仍在使用旧 Cookie，重新登录并使用新 `JSESSIONID`。
- `500`：检查同一个 Tomcat Run Configuration 中的 JDBC 环境变量是否仍存在，并查看服务端异常日志。
- 仍表现为旧代码：确认部署源是 `target/01-java-web-basics.war`，并在重新打包后完全重启 Tomcat。

## 6. 验收清单

- [ ] 能说明浏览器请求如何到达 Servlet。
- [ ] 能解释 Servlet 生命周期和 `doGet` / `doPost`。
- [ ] 能完成注册、登录、当前用户查询、退出登录和留言增删查中的核心路径。
- [ ] 能解释 Cookie、Session、Filter、Listener 的职责、作用域和生命周期。
- [ ] 能手写并注册 `ServletContextListener`、`HttpSessionListener`，至少观察一次应用和 Session 回调。
- [ ] （扩展）能手写并注册 `ServletRequestListener`，观察一次请求创建和销毁回调。
- [ ] 能使用 `PreparedStatement` 和 `try-with-resources` 完成 JDBC 操作。
- [ ] 能说明事务提交、回滚和异常传播的位置。
- [ ] 有一条核心功能的手动验证证据；自动化测试和构建命令作为辅助能力理解即可。
- [ ] 能将 WAR 部署到 Tomcat 10.1，并说明 Maven 打包与 Tomcat 运行的关系。

## 7. 对前端开发者的理解重点

可以先用熟悉的前端概念建立映射：

| 前端经验 | Java Web 对应概念 |
| --- | --- |
| 路由表 | `@WebServlet` URL 映射或 `web.xml` 配置 |
| 请求对象 | `HttpServletRequest` |
| 响应对象 | `HttpServletResponse` |
| 中间件 | Filter |
| 浏览器 Cookie | `Cookie` / `JSESSIONID` |
| 服务端会话 | `HttpSession` |
| 数据访问封装 | DAO |
| 数据库驱动 | JDBC Driver |

这些只是帮助建立直觉，不代表两边的生命周期、线程模型和错误处理完全相同。编码时仍要以 Java Web 的接口契约为准。

## 8. 暂不做的内容

本轮暂不加入 Spring、Spring Boot、MyBatis、JPA、前后端打包整合、JWT 和完整的生产级认证体系。它们会在后续 Module 分别练习。即使不是完整认证系统，密码也必须使用 BCrypt 等带随机盐的慢哈希，不能明文持久化，也不能用普通 MD5 或 SHA-256 代替密码哈希。

## 9. 开始编码前的确认

完成项目创建后，先确认以下内容，再进入第 1 步：

```text
1. 目录是 spring-training/01-java-web-basics
2. pom.xml 的 packaging 是 war
3. JDK 是 21
4. mvn clean test 能成功
5. 能找到 src/main/java、src/main/resources、src/main/webapp
```

确认后再逐步实现，不要一次性生成全部业务代码。每完成一个步骤，先运行并解释结果，再进入下一步。
