# 学习疑问与解答

本文档用于集中记录 Java Web、Spring 及后续全栈学习过程中产生的疑问和结论。

记录新问题时，尽量包含：

- 问题产生时所处的学习阶段。
- 简短结论。
- 原理或关键概念。
- 容易混淆的边界。
- 与当前项目或 `linux-server` 的对应位置（适用时）。

## 目录

1. [现代前后端分离项目的 Java 后端一般都使用 Spring MVC 提供接口吗？](#1-现代前后端分离项目的-java-后端一般都使用-spring-mvc-提供接口吗)
2. [为什么最初建议 H2，Module 01 最终为什么改用 MySQL？](#2-为什么最初建议-h2module-01-最终为什么改用-mysql)
3. [Servlet 项目是否需要先安装 Tomcat？](#3-servlet-项目是否需要先安装-tomcat)
4. [Tomcat 已启动，为什么访问应用仍然返回 404？](#4-tomcat-已启动为什么访问应用仍然返回-404)
5. [IDEA 中 war exploded 和 war 有什么区别？](#5-idea-中-war-exploded-和-war-有什么区别)
6. [自己编写的 JSP 应该放在哪个目录？](#6-自己编写的-jsp-应该放在哪个目录)
7. [使用 IDEA 启动 Tomcat 时，JDBC 环境变量在哪里配置？](#7-使用-idea-启动-tomcat-时jdbc-环境变量在哪里配置)
8. [JSP 页面放在 WEB-INF 下，为什么访问页面会 404？](#8-jsp-页面放在-web-inf-下为什么访问页面会-404)
29. [Spring 中的 Repository 是否相当于 Java Web 中的 DAO？](#29-spring-中的-repository-是否相当于-java-web-中的-dao)

## 1. 现代前后端分离项目的 Java 后端一般都使用 Spring MVC 提供接口吗？

**简短结论：在传统 Java Web 和 Spring Boot 项目中，Spring MVC 确实是目前最常见的接口实现方案之一，但不是 Java 后端唯一的选择。**

前后端分离只是一种系统协作方式：前端和后端分别开发、部署，通过 HTTP 接口交换 JSON 等数据。它并没有规定后端必须使用哪一种 Java 框架。

在常见的 Spring Boot 项目中，引入 `spring-boot-starter-web` 后，Spring Boot 会自动配置 Spring MVC。开发者通常使用以下注解提供 REST API：

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
}
```

一次典型请求可以简化为：

```text
前端发送 HTTP 请求
  -> Servlet 容器（通常是内嵌 Tomcat）
  -> Spring MVC 的 DispatcherServlet
  -> Controller
  -> Service
  -> 数据访问层
  -> Controller 返回 Java 对象
  -> Spring MVC 将对象序列化为 JSON
  -> 前端收到响应
```

这里需要区分两个概念：

- **Spring MVC**：负责 HTTP 请求映射、参数绑定、Controller 调用、响应转换等 Web 层能力。
- **Spring Boot**：负责自动配置、依赖整合、应用启动和工程化约定。它经常自动配置 Spring MVC，但它本身不等于 Spring MVC。

Java 后端还可能采用其他方案：

- Spring WebFlux：面向响应式、非阻塞 Web 场景。
- Jakarta REST（JAX-RS）及其实现：例如 Jersey、RESTEasy。
- Quarkus、Micronaut 等框架提供的 HTTP/REST 能力。
- Vert.x、Netty：用于更底层或高并发、事件驱动的网络服务。
- gRPC：服务之间使用 RPC 通信时的常见选择之一，通常不是浏览器直接调用的普通 REST API。

因此，更准确的理解是：

> 现代前后端分离的 Java 项目通常通过 HTTP API 向前端提供服务；在 Spring 技术栈中，这些 API 大多由 Spring MVC 实现，并经常由 Spring Boot 完成自动配置和启动。

当前学习路线先学习 Servlet 和 Java Web 基础，再学习 Spring MVC，是为了理解 Spring MVC 替你完成了哪些底层工作，而不是只会使用 Controller 注解。

## 2. 为什么最初建议 H2，Module 01 最终为什么改用 MySQL？

**简短结论：H2 是第一轮训练的降复杂度方案，不是 MySQL 的替代品；由于本机已经安装 MySQL，本项目最终改为直接使用 MySQL + 原生 JDBC。**

最初建议 H2，是因为 Module 01 的主要目标是理解 Servlet 请求链路和原生 JDBC API。一个随应用启动的嵌入式数据库可以减少数据库安装、服务启动、账号权限、端口和字符集等环境问题，让注意力先集中在 Java Web 与 JDBC 本身。

H2 的优点包括：

- 只需引入一个 Maven 依赖，不需要单独安装和启动数据库服务。
- 可以使用内存模式快速重建测试数据，也可以使用文件模式保留本地数据。
- 同样通过 JDBC 使用 `Connection`、`PreparedStatement`、`ResultSet`、提交和回滚，适合练习 JDBC 的核心流程。
- 项目和测试更容易在其他机器上重复运行。

但 H2 不能证明已经掌握 MySQL。两者在 SQL 方言、字段类型、自增规则、字符集与排序规则、事务隔离、锁、索引、执行计划、用户权限和连接配置等方面存在差异。即使启用 H2 的 MySQL 兼容模式，也不能把它当成真实 MySQL。

本项目结合当前环境后采用：

```text
Module 01：MySQL + 原生 JDBC
  -> 掌握 Connection、PreparedStatement、ResultSet 和事务
  -> 同时练习服务启动、专用账号、字符集和连接失败

Module 04：MySQL + MyBatis
  -> 在真实数据库上继续学习 SQL、索引、事务和对象映射

Module 06：MySQL + 综合项目
  -> 完成接近真实业务的开发、测试和部署
```

当前本机安装的是 MySQL 5.7.19。它可以用于本地学习，但 MySQL 5.7 已停止官方支持，不应作为新生产环境的版本。Module 01 使用与其兼容的 Connector/J 8.0.33；在综合项目阶段前应升级到仍受支持的 MySQL 版本。H2 保留为后续理解嵌入式测试数据库的备选项，不再是本 Module 的主数据库。

## 3. Servlet 项目是否需要先安装 Tomcat？

**简短结论：当前 Module 需要安装 Tomcat 才能运行 Servlet，但编译项目本身不一定需要本机安装 Tomcat。**

当前项目采用 `WAR + 外部 Tomcat` 的传统部署方式：

```text
mvn compile / mvn test
  -> 编译阶段使用 jakarta.servlet-api

mvn package
  -> 生成 WAR

Tomcat 启动
  -> 提供 Servlet 运行环境
  -> 部署 WAR
  -> 接收 HTTP 请求并调用 Servlet
```

`jakarta.servlet-api` 在 Module POM 中使用 `provided`，表示编译时需要它，运行时由 Tomcat 提供。因此，Tomcat 不是普通业务 JAR，也不是通过 `mvn package` 自动替代安装的依赖。

本项目选择 Tomcat 10.1.x，因为它使用 Jakarta Servlet 6，与 `jakarta.servlet-api:6.0.0` 和 `jakarta.servlet.*` 包名匹配。Tomcat 9 使用旧的 `javax.servlet.*` 命名空间，不能与当前代码直接混用。

第一轮建议使用 ZIP 版本并通过 `catalina.bat run` 启动，以便直接看到部署日志；暂时不必安装成 Windows 服务。完成 WAR 部署后，Servlet 的访问地址通常包含 WAR 文件名形成的上下文路径，例如：

```text
http://localhost:8080/01-java-web-basics/health
```

如果只是测试普通 Java 类或执行 `mvn test`，可以暂时不启动 Tomcat；如果要访问 Servlet、验证请求生命周期或完成 Module 验收，就必须启动 Tomcat。

## 4. Tomcat 已启动，为什么访问应用仍然返回 404？

**本次原因：IDEA 的 Tomcat Deployment 添加了 Module 项目目录，而不是 `war exploded` Web Artifact。**

Tomcat 返回自己的 404 页面，说明服务器和 `8080` 端口正常；错误信息中的上下文路径也说明请求已经到达 Tomcat。当前运行配置部署的是：

```text
spring-training/01-java-web-basics
```

但可直接部署的 Web 根目录应包含：

```text
index.jsp
WEB-INF/
  web.xml
  classes/
  lib/
```

项目源码目录中的真实布局是：

```text
01-java-web-basics/
├─ src/main/webapp/index.jsp
├─ src/main/webapp/WEB-INF/web.xml
└─ target/classes/cn/siyes/training/web/Hello.class
```

直接部署 Module 根目录时，Tomcat 不会自动把 Maven 的源码、编译输出和依赖重新组合成标准 Web 应用目录，因此根路径和 `/health` 都可能返回 404。

正确处理方式是在 IDEA 中停止 Tomcat，然后：

1. 打开 `Run/Debug Configurations -> Tomcat Server -> Deployment`。
2. 删除当前带文件夹图标的 `01-java-web-basics` 部署项。
3. 点击 `+ -> Artifact`，选择 `01-java-web-basics:war exploded`。
4. 如果没有该 Artifact，进入 `File -> Project Structure -> Artifacts`，选择 `+ -> Web Application: Exploded -> From Modules`，再选择 `01-java-web-basics` 创建。
5. 建议把 Application context 统一设置为 `/01-java-web-basics`。
6. 重新启动 Tomcat，先访问根路径确认 `index.jsp`，再访问 `/01-java-web-basics/health`。

`war exploded` 会把 Web 资源、编译后的类和运行依赖按 Web 应用规范组合成目录，适合开发调试。阶段验收时仍需执行 `mvn package` 并验证实际生成的 WAR，避免只证明 IDEA 部署功能可用。

## 5. IDEA 中 war exploded 和 war 有什么区别？

两者内容目标相同，都是标准 Java Web 应用；主要区别是部署形态和更新方式。

| Artifact | 形态 | 特点 | 适用场景 |
| --- | --- | --- | --- |
| `01-java-web-basics:war exploded` | 已展开的目录 | IDEA 可以较快更新 class、JSP 和静态资源，便于调试 | 日常开发和断点调试 |
| `01-java-web-basics:war` | 单个压缩的 `.war` 文件 | 修改后通常需要重新构建和部署，更接近发布产物 | 打包验收和部署验证 |

`war exploded` 的目录大致是：

```text
01-java-web-basics_war_exploded/
├─ index.jsp
└─ WEB-INF/
   ├─ web.xml
   ├─ classes/
   └─ lib/
```

`war` 则把这些内容压缩成一个文件：

```text
01-java-web-basics.war
```

Tomcat 收到 WAR 后通常会将其解压再运行。压缩和解压不会改变 Servlet 的业务逻辑。

当前开发阶段只在 IDEA Deployment 中保留 `01-java-web-basics:war exploded`，不要把两个 Artifact 同时部署到同一个 Application context，否则可能重复部署或产生上下文冲突。阶段验收时再执行 Maven：

```bash
mvn package
```

并单独验证 `target/01-java-web-basics.war`。IDEA 生成的 `war` Artifact 与 Maven `mvn package` 的构建入口不是同一件事；验收应以 Maven 产物为准，证明项目不依赖 IDEA 也能完成打包。

## 6. 自己编写的 JSP 应该放在哪个目录？

JSP 必须放在 Maven Web 根目录 `src/main/webapp` 内，但具体位置取决于是否允许浏览器直接访问。

推荐结构：

```text
src/main/webapp/
├─ index.jsp
├─ assets/
│  ├─ css/
│  ├─ js/
│  └─ images/
└─ WEB-INF/
   ├─ web.xml
   └─ views/
      ├─ login.jsp
      ├─ register.jsp
      └─ messages.jsp
```

- 放在 `src/main/webapp/index.jsp` 等根目录中的 JSP 可以被浏览器直接访问，例如 `/01-java-web-basics/index.jsp`。
- 放在 `src/main/webapp/WEB-INF/views` 中的 JSP 不能被浏览器直接访问，只能由 Servlet 在服务端转发，适合业务页面。

Servlet 转发示例：

```java
request.setAttribute("pageTitle", "留言板");
request.getRequestDispatcher("/WEB-INF/views/messages.jsp")
       .forward(request, response);
```

JSP 可以通过表达式语言读取请求属性：

```jsp
<h1>${pageTitle}</h1>
```

业务 JSP 推荐放在 `WEB-INF/views`，因为这样可以强制请求先经过 Servlet，由 Servlet 完成参数处理、权限判断和数据准备，再转发到 JSP 渲染。它与前端路由守卫或服务端中间层控制页面入口有相似之处，但实际机制是 Servlet 容器禁止外部直接访问 `WEB-INF`。

JSP 只负责展示，不应直接编写 JDBC、事务或复杂业务逻辑。`<%= ... %>` 等 Scriptlet 语法可以用于观察 JSP 原理，但正式练习优先使用请求属性和 EL；循环、条件等需求出现时再引入 JSTL。

当前 `/health` 是 JSON 接口，应继续由 Servlet 直接返回 JSON。页面渲染可以另外设计 `/home`、`/login` 或 `/messages` 等 Servlet 路径，再转发到相应 JSP，避免把 API 响应与页面渲染混在同一个端点中。

## 7. 使用 IDEA 启动 Tomcat 时，JDBC 环境变量在哪里配置？

`System.getenv` 读取的是当前 Java 进程的环境变量。Web 应用运行在 Tomcat JVM 中，因此变量必须传给 IDEA 启动的 Tomcat，而不是只配置在 Navicat 或另一个 PowerShell 窗口中。

在 IDEA 中打开：

```text
Run
  -> Edit Configurations
  -> Tomcat Server
  -> Local
  -> Environment variables
```

不同 IDEA 版本的字段位置可能是 `Server -> Modify options -> Environment variables`，也可能位于 `Startup/Connection -> Run`。需要添加：

```text
TRAINING_DB_URL
TRAINING_DB_USER
TRAINING_DB_PASSWORD
```

URL 示例：

```text
jdbc:mysql://localhost:3306/spring_training_web?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
```

保存后必须完整重启 Tomcat。IDEA Run Configuration 中的变量只作用于该运行配置，不会自动同步给 Navicat、系统中的其他终端或其他项目；反过来，Navicat 保存的连接也不会自动提供给 Java 应用。

不要把密码写进源码、POM、`src/main/resources` 或日志。如果改用 Windows 系统环境变量，并且 IDEA 在设置变量之前已经启动，需要重启 IDEA，让它继承新的系统环境。

另外，放在 `src/main/java` 中的参考代码仍属于 Maven 编译输入。即使没有被调用，引用缺失依赖的 `JDBCUtils.java` 也会造成编译失败；纯参考代码应移到源码目录之外，或改成不会参与编译的文本/IDE Scratch 文件。

## 8. JSP 页面放在 WEB-INF 下，为什么访问页面会 404？

`WEB-INF` 是 Servlet 容器保护目录，浏览器不能直接访问其中的文件。因此：

```text
/register
```

不会自动找到：

```text
/WEB-INF/views/register.jsp
```

当前 `web.xml` 只映射了：

```text
/api/register
```

它是处理注册提交的 POST 接口，不是渲染注册页面的 GET 地址。推荐增加一个页面 Servlet，将 GET `/register` 转发到 JSP：

```java
protected void doGet(
        HttpServletRequest req,
        HttpServletResponse resp
) throws ServletException, IOException {
    req.getRequestDispatcher("/WEB-INF/views/register.jsp")
       .forward(req, resp);
}
```

这个页面 Servlet 需要映射到 `/register`；可以使用 `@WebServlet("/register")`，也可以在 `web.xml` 中配置，但不要同时使用两套映射。

JSP 表单的 `action` 指向注册 POST 接口是正确的，但表单控件必须有 `name`，否则 `request.getParameter()` 取不到值：

```jsp
<form action="${pageContext.request.contextPath}/api/register" method="post">
    <input name="username" placeholder="请输入用户名">
    <input name="password" type="password" placeholder="请输入密码">
    <button type="submit">注册</button>
</form>
```

最终请求链路是：

```text
浏览器 GET /register
  -> RegisterPageServlet
  -> forward 到 /WEB-INF/views/register.jsp

浏览器提交表单 POST /api/register
  -> RegisterServlet
  -> 参数校验、重复校验、写入数据库
```

如果只是临时验证 JSP 是否能打开，也可以把 JSP 放在 `src/main/webapp/register.jsp`，但这会绕过页面 Servlet，不适合作为本阶段推荐结构。

## 9. IDEA 已配置环境变量，为什么 `System.getenv` 仍然取不到？

排查时必须逐字核对环境变量名称。`ConnectionFactory` 读取的是：

```text
TRAINING_DB_URL
TRAINING_DB_USER
TRAINING_DB_PASSWORD
```

如果 IDEA 中写成 `TRAINING_DB_USE`，它与 `TRAINING_DB_USER` 不是同一个变量，`System.getenv("TRAINING_DB_USER")` 会返回 `null`。

环境变量只在进程启动时传入。修改 Run Configuration 后，需要先完全停止当前 Tomcat，再用同一个 Tomcat 配置重新启动；仅重新部署 Artifact 或继续使用已有 Debug 进程不会刷新环境变量。`Pass environment variables` 必须保持启用，并确认工具栏实际选择的是修改过的 `tomcat` 配置。

`JAVA_OPTS` 是 Tomcat 启动脚本支持的环境变量，在通过 `catalina.bat` 启动时通常可以传递 JVM 参数；但它与 `TRAINING_DB_*` 是两类不同配置。为了让 IDEA 运行配置中的 JVM 参数更直观、便于确认，编码参数建议直接放到 Tomcat Run Configuration 的 `VM options`，例如：

```text
-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
```

验证时可以在 `requireEnv` 断点观察变量是否为非空，但不要打印数据库密码。截图中出现了真实密码，应该立即在 Navicat 中为应用账号修改密码，并同步更新 IDEA 本地运行配置；密码不应继续保留在截图、日志或项目文件中。

## 10. JDBC URL 正确，为什么提示 `No suitable driver`？

如果异常信息中已经出现完整的 `jdbc:mysql://...` URL，说明环境变量读取已经通过；`No suitable driver found` 表示当前 Tomcat Web 应用的运行时类路径没有找到能够处理该 URL 的 MySQL Connector/J 驱动。

本项目的 Module POM 已声明 `com.mysql:mysql-connector-j`，并且 Maven 生成的目录中应有：

```text
01-java-web-basics/target/01-java-web-basics/WEB-INF/lib/mysql-connector-j-8.0.33.jar
```

还必须检查 Tomcat 实际部署目录中的：

```text
<实际 docBase>/WEB-INF/lib/mysql-connector-j-8.0.33.jar
```

IDEA 部署 `war exploded` 时不一定复制到 Tomcat 的 `webapps`，也可能通过临时 Context 指向项目的 `target` 或 `out/artifacts`；实际 `docBase` 可以从 Tomcat 启动或部署日志确认。如果实际目录没有驱动，说明 IDEA 使用了旧的 exploded Artifact 或部署到了其他输出目录。重新加载 Maven 项目，重新构建 `war exploded` Artifact，确认 Tomcat Deployment 选择当前 Module，然后完全停止并重新启动 Tomcat。

现代 Connector/J 支持 JDBC 4 自动注册，通常不需要永久添加 `Class.forName`。排查时可以临时执行：

```java
Class.forName("com.mysql.cj.jdbc.Driver");
```

如果这里抛出 `ClassNotFoundException`，可以确定部署类路径缺少驱动；如果能加载，再继续检查实际运行的 Artifact 和类加载器。

连接创建失败时，`connection` 仍然是 `null`。如果 `finally` 无条件执行 `connection.close()`，会产生后续 `NullPointerException`，它不是数据库根因。连接资源应改用 `try-with-resources`，或至少先判断 `connection != null`。

## 11. MySQL 驱动已加载，为什么提示 `Malformed database URL`？

`MySQL 驱动加载成功` 说明 Connector/J 已经进入 Tomcat 类路径。本次错误出在 JDBC URL 本身：异常中的 `\\=true` 表示 URL 实际包含反斜杠，MySQL 驱动无法把它解析为查询参数。

在 IDEA 的 Environment Variables 中，`TRAINING_DB_URL` 的 Value 应填写普通文本，不要加反斜杠，也不要加引号：

```text
jdbc:mysql://localhost:3306/spring_training_web?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
```

特别注意：

- 写 `useUnicode=true`，不要写 `useUnicode\\=true`。
- `&` 在 IDEA 环境变量输入框中直接写，不需要改成 `&amp;`。
- 只有把 URL 写进 XML 时才需要考虑 XML 转义；IDEA 环境变量输入框不是 XML。
- 不要把从异常、转义字符串或某些日志展示中复制出的 `\\=` 当成实际配置格式。

修改后完全停止并重新启动 Tomcat。可以在断点中观察 `TRAINING_DB_URL` 的值，确认查询参数中没有任何反斜杠；不要输出数据库密码。

## 12. Filter 已设置 UTF-8，为什么浏览器中的中文响应仍然乱码？

`response.setCharacterEncoding("UTF-8")` 决定 Servlet 使用什么编码把字符写成字节，但浏览器还需要通过 HTTP `Content-Type` 响应头知道如何解码。只有编码而没有明确的媒体类型和 `charset`，浏览器可能按其他编码猜测，最终出现乱码。

如果当前接口返回普通文本，应在调用 `getWriter()` 之前设置：

```java
response.setContentType("text/plain;charset=UTF-8");
```

如果约定 `/api/*` 全部返回 JSON，可以在 API Filter 调用 `chain.doFilter` 之前统一设置：

```java
response.setCharacterEncoding(StandardCharsets.UTF_8.name());
response.setContentType("application/json;charset=UTF-8");
```

此时响应体也必须是合法 JSON，例如：

```json
{"message":"注册成功"}
```

不能声明 `application/json`，却只返回没有 JSON 引号或对象结构的 `注册成功`。编码和 Content-Type 都必须在首次调用 `getWriter()`、写响应体或提交响应之前设置；响应提交后再修改不会生效。

请求体编码与响应体编码也要区分：`request.setCharacterEncoding("UTF-8")` 用于读取表单参数，`response.setCharacterEncoding` 和 `response.setContentType` 用于浏览器解码响应。

## 13. Java 文本块中如何替换变量？

Java 的文本块 `""" ... """` 只是多行字符串，不等同于 JavaScript 的模板字符串，不会解析 `${code}` 或 `${message}`。在当前 JDK 21 训练项目中，可以使用格式占位符和 `String.formatted`：

```java
private String createResponse(int code, String message) {
    return """
        {
          "code": %d,
          "message": "%s"
        }
        """.formatted(code, message);
}
```

`%d` 对应整数，`%s` 对应字符串，参数按照出现顺序传入 `formatted(code, message)`。JSON 的字段名和字符串值必须使用双引号，不能写成 JavaScript 对象字面量的宽松形式。

也可以连续调用 `replace` 替换 `${...}`，但占位符维护、类型转换和 JSON 转义更容易出错，因此不作为当前推荐写法。当前方法只适合由服务端控制的简单消息；如果 `message` 可能包含双引号、反斜杠、换行或用户输入，手工拼 JSON 会生成非法内容，后续应使用 Jackson 等 JSON 序列化库。

## 14. 注册流程中的 HTTP 状态码应该在哪里设置？

HTTP 状态码属于 Servlet/API 边界，不应由 `RegisterService` 返回。当前 `register()` 声明返回 `long`，却返回 `201`、`409`、`500`，混淆了“新用户 ID”和“HTTP 状态码”两种含义；成功时也无法返回真正的数据库生成 ID。

推荐职责划分：

```text
Servlet：读取参数、设置 HTTP 状态码和 JSON 响应
Service：执行注册业务，成功返回用户 ID，业务冲突抛出明确异常
DAO：执行 SQL，传播 SQLException，不知道 HTTP 状态码
```

推荐的 Service 契约可以是：

```java
public long register(String username, String password)
        throws SQLException, UsernameAlreadyExistsException {
    // 不存在则哈希密码并插入，成功返回 userId
    // 已存在或唯一键冲突则抛出 UsernameAlreadyExistsException
}
```

Servlet 再把业务结果转换为协议结果：

```java
try {
    long userId = registerService.register(username, password);
    resp.setStatus(HttpServletResponse.SC_CREATED);
    // 返回 userId 和成功消息
} catch (UsernameAlreadyExistsException e) {
    resp.setStatus(HttpServletResponse.SC_CONFLICT);
} catch (SQLException e) {
    // 记录服务端日志，不把 SQL 细节返回给前端
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
}
```

不要在 Service 中导入或调用 `HttpServletResponse.SC_*`。如果当前阶段还不准备定义自定义异常，至少使用业务枚举或结果对象，而不要让 `long` 同时表示用户 ID 和状态码。状态码常量应使用 `HttpServletResponse.SC_BAD_REQUEST`、`SC_CREATED`、`SC_CONFLICT` 和 `SC_INTERNAL_SERVER_ERROR`，不要直接写数字。

另外，`UserDao.insert()` 的 `executeUpdate()` 返回的是受影响行数，不是数据库生成的用户 ID。如果 Service 需要返回新用户 ID，DAO 应使用 `Statement.RETURN_GENERATED_KEYS` 并读取生成键。

## 15. Service 中如何抛出“用户名已存在”异常？

先创建独立的业务异常类，例如：

```text
src/main/java/cn/siyes/training/web/exception/UsernameAlreadyExistsException.java
```

```java
package cn.siyes.training.web.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("用户名已存在: " + username);
    }

    public UsernameAlreadyExistsException(String username, Throwable cause) {
        super("用户名已存在: " + username, cause);
    }
}
```

Service 中先查询到重复用户时，使用 `throw` 主动抛出异常：

```java
if (exists) {
    throw new UsernameAlreadyExistsException(username);
}
```

Servlet 在 API 边界捕获它并设置 HTTP 状态码：

```java
try {
    long userId = registerService.register(username, password);
    resp.setStatus(HttpServletResponse.SC_CREATED);
} catch (UsernameAlreadyExistsException e) {
    resp.setStatus(HttpServletResponse.SC_CONFLICT);
} catch (SQLException e) {
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
}
```

这里选择 `RuntimeException` 是因为“用户名已存在”是可预期的业务结果，不是底层系统故障；Servlet 仍然可以显式捕获它。`SQLException` 继续作为数据库异常传播，由 Servlet 统一转换为 500。

仅查询后再抛出还不能应对并发注册：两个请求可能同时查到不存在。`users.username` 的唯一索引触发 `SQLIntegrityConstraintViolationException` 时，Service 还应将这个特定数据库异常转换为 `UsernameAlreadyExistsException(username, e)`；其他 SQL 异常继续按系统错误处理。

## 16. Java Web 中是否大量使用 `UsernameAlreadyExistsException`？

`UsernameAlreadyExistsException` 不是 Java Web 或 Servlet 规定的固定类名，而是项目自定义的业务异常。实际开发中常见的是这种模式：Service 表达业务结果或业务失败原因，Servlet、Controller 或全局异常处理器再把它转换为 HTTP 响应。

传统 Servlet 项目通常在 Servlet 中捕获业务异常；Spring MVC 项目则常用 `@ExceptionHandler` 或 `@RestControllerAdvice` 统一映射异常。异常类名称和包结构会因项目而变化，但分层思想相同。

不要把所有普通分支都设计成异常。用户名重复、权限不足、资源不存在等需要跨层传播并转换为不同响应的业务失败，可以使用明确的业务异常；简单的“成功/失败”或查询结果，更适合使用布尔值、枚举或结果对象。数据库唯一约束异常也应只在确认是重复键时转换，连接失败等系统错误不能伪装成业务冲突。

本 Module 使用该异常是为了练习 Service 与 HTTP 层解耦。后续进入 Spring MVC 时会看到相同的业务异常可以交给全局异常处理器统一返回 JSON，不需要每个 Controller 重复写 `try/catch`。

## 17. 业务异常分支中的 `writer` 为什么是 `null`？

`HttpServletResponse.getWriter()` 正常情况下不会返回 `null`。如果先声明：

```java
PrintWriter writer = null;
```

然后只在成功分支中执行：

```java
writer = response.getWriter();
```

而注册业务在此之前抛出 `UsernameAlreadyExistsException`，进入 `catch` 时变量仍然是 `null`。此时调用 `writer.print(...)` 会失败，或者因为 `finally` 中只判断 `writer != null` 而没有真正写出任何响应体。

推荐让成功、409 和 500 分支都调用同一个写响应方法：

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

然后分别执行 `writeResponse(response, SC_CREATED, ...)`、`writeResponse(response, SC_CONFLICT, ...)` 或 `writeResponse(response, SC_INTERNAL_SERVER_ERROR, ...)`。不要把响应写入依赖放在 `finally` 中；`finally` 更适合资源清理，而且响应应在首次写入前完成状态码和 Content-Type 设置。

## 18. 把 `getWriter()` 放到业务调用前，是否就能在 `finally` 写响应？

技术上可以：如果在业务调用前执行 `writer = response.getWriter()`，后续业务异常进入 `finally` 时，Writer 已经完成赋值。但这只解决空变量问题，不代表 `finally` 是合适的响应处理位置。

`finally` 无论成功、业务异常、系统异常还是提前返回都会执行。把响应写入放在这里可能导致：覆盖前面已经设置的状态或响应体；响应已经提交后再次修改状态；写响应失败时掩盖原始数据库异常；本来不应该返回正文的路径也被强制写入正文。`getWriter()` 本身也可能抛出 `IOException`。

更清晰的结构是：`try/catch` 只决定状态码和消息，离开 `try/catch` 后统一调用 `writeResponse`；`finally` 只做数据库连接等资源清理。只有在明确保证所有路径都必须返回同一种响应、且能处理响应已提交等边界时，才考虑在 `finally` 中写响应。

## 19. `try-with-resources` 中的 `connection` 为什么在 `finally` 里找不到？

资源写在：

```java
try (Connection connection = ConnectionFactory.getConnection()) {
    // connection 只在这里有效
}
```

括号中声明的 `connection` 作用域只覆盖这个 `try` 代码块，`finally` 不在该作用域内，因此 IDEA 会提示 `Cannot resolve symbol 'connection'`。这不是需要把变量提升到外层的信号；`try-with-resources` 已经在离开代码块时自动调用 `close()`，应直接删除手写的 `finally`。

如果需要处理数据库异常，不要写没有上下文的 `throw new SQLException()`，这会丢失原始异常原因。可以让原异常继续传播：

```java
catch (SQLException e) {
    throw e;
}
```

或者增加上下文并保留 cause：

```java
throw new SQLException("注册用户失败", e);
```

注册流程中只把唯一键冲突转换为 `UsernameAlreadyExistsException`，其他 `SQLException` 继续传播：

```java
try (Connection connection = ConnectionFactory.getConnection()) {
    if (userDao.existsByUsername(connection, username)) {
        throw new UsernameAlreadyExistsException(username);
    }

    try {
        return userDao.insert(connection, username, passwordHash);
    } catch (SQLIntegrityConstraintViolationException e) {
        throw new UsernameAlreadyExistsException(username, e);
    }
}
```

## 20. JDBC 使用 `try-with-resources` 时还需要设置参数怎么办？

`PreparedStatement` 必须先创建并设置参数，再执行 SQL。不能在资源声明中直接调用 `executeQuery()`，因为那时还没有机会调用 `setString` 等参数绑定方法。

正确顺序是：

```java
try (PreparedStatement statement = connection.prepareStatement(sql)) {
    statement.setString(1, username);

    try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
    }
}
```

这里外层 `try` 自动关闭 `PreparedStatement`，内层 `try` 自动关闭 `ResultSet`；参数绑定发生在执行查询之前。插入语句同样先创建 Statement、设置所有参数，再调用 `executeUpdate()`。

## 21. JSON 中的 `code: 201` 为什么不能让 Network 显示 201？

JSON 响应体中的 `code` 字段只是业务响应内容，不能改变 HTTP 协议状态。浏览器 Network 面板显示的状态来自 `HttpServletResponse`，必须显式调用：

```java
response.setStatus(status);
```

统一响应方法应在写入正文前设置状态码：

```java
private void writeResponse(
        HttpServletResponse response,
        int status,
        String message
) throws IOException {
    response.setStatus(status);
    response.getWriter().print(createResponse(status, message));
}
```

如果只在 JSON 中写 `"code": 201` 而没有调用 `setStatus(201)`，Network 仍会显示默认的 `200 OK`。验收时应同时检查响应体和 Network 的 HTTP Status Code；两者应保持一致。`Content-Type` 和字符集也必须在首次写入响应前设置。

## 22. MySQL 5.7 中如何把 `create_at` 改成 `created_at`？

先在 Navicat 的 `spring_training_web` 查询窗口确认当前表结构：

```sql
SHOW CREATE TABLE users;
```

MySQL 5.7 使用 `CHANGE COLUMN` 重命名字段，并且需要重新写出完整字段定义：

```sql
ALTER TABLE users
    CHANGE COLUMN create_at created_at
    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

执行后验证：

```sql
DESCRIBE users;
SELECT id, username, password_hash, created_at
FROM users;
```

这个操作只修改列名和列定义，不会删除已有用户数据。MySQL 8 可以使用 `RENAME COLUMN`，但当前训练环境是 MySQL 5.7，因此优先使用 `CHANGE COLUMN`。

数据库修改成功后，还要把 `src/main/resources/schema.sql` 中的建表语句同步为 `created_at`，否则以后根据脚本重建数据库时会再次生成旧字段名。不要直接在应用启动时自动执行这类结构修改；当前项目的数据库操作统一通过 Navicat 手动完成。

## 23. 如何主动制造数据库连接失败并验收 500？

为了练习异常路径，可以只在 IDEA Tomcat Run Configuration 中临时把 JDBC URL 端口从 `3306` 改为没有服务监听的 `3307`：

```text
jdbc:mysql://localhost:3307/spring_training_web?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
```

完全停止并重新启动 Tomcat 后，用一个新的用户名提交 POST 注册请求，预期得到：

```text
HTTP 500 Internal Server Error
{"code":500,"message":"未预期数据库错误"}
```

Network 中应检查真实 HTTP 状态码，浏览器响应不能暴露 JDBC URL、数据库凭据、SQL 或异常堆栈；详细异常只允许出现在服务端日志。Navicat 中不应出现这次测试用户名的记录。

测试结束后必须把端口恢复为 `3306`，完全重启 Tomcat。使用 Windows 服务管理器停止 MySQL 也能制造同类错误，但会同时影响 Navicat，因此优先使用错误端口的方式。

## 24. 删除 `Class.forName("com.mysql.cj.jdbc.Driver")` 后为什么变成 500？

Connector/J 8 支持 JDBC 4 自动注册，但驱动自动发现依赖类加载器。在当前 Tomcat Web 应用中，`DriverManager` 的加载器可能看不到位于应用 `WEB-INF/lib` 中的驱动服务提供者；删除显式加载后，`DriverManager.getConnection` 会抛出 `No suitable driver`，Servlet 再将它转换为 500。

本 Module 可以保留显式加载，但不应在每次注册请求中执行。推荐放在 `ConnectionFactory` 的静态初始化块中：

```java
static {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        throw new ExceptionInInitializerError(e);
    }
}
```

静态初始化块在该类第一次被加载时执行一次；Tomcat 重新部署应用时会创建新的 Web 应用类加载器并再次初始化。独立 Java 程序通常可以依赖 JDBC 4 自动注册，但不能把这个结论直接套用到所有 Servlet 容器部署方式。

## 25. 登录时为什么不在 SQL 中同时查询 `username` 和 `password_hash`？

登录请求拿到的是用户输入的原始密码，数据库保存的是 BCrypt 哈希，两者不能直接用 SQL 的 `=` 比较。BCrypt 哈希包含算法版本、成本参数、随机盐和摘要，同一个原始密码每次生成的哈希都可能不同。

正确流程是：

```text
先按 username 查询数据库中的 password_hash
  -> BCrypt.verifyer().verify(原始密码, 数据库哈希)
  -> 根据 verified 判断登录成功或失败
```

`BCrypt.verifyer()` 会从数据库哈希中读取盐和成本参数，因此不需要应用另外保存盐，也不能把原始密码或新生成的哈希直接拼进登录 SQL。用户名不存在和密码错误应返回同一个业务异常，避免泄露账号是否存在。

## 26. IDEA 为什么找不到 JUnit 5 的 `@Test`？

当前 `MessageValidatorTest.java` 被放在了 `src/main/java`，而 POM 中的 `junit-jupiter` 使用 `<scope>test</scope>`。测试作用域依赖只对测试源码可见，因此主源码目录无法解析 `org.junit.jupiter.api.Test`。测试类本身还必须导入 JUnit 5 的注解。

正确目录应为：

```text
src/main/java/cn/siyes/training/web/validation/MessageValidator.java
src/test/java/cn/siyes/training/web/validation/MessageValidatorTest.java
```

测试类顶部需要：

```java
package cn.siyes.training.web.validation;

import org.junit.jupiter.api.Test;
```

在 IDEA 中可以使用 `Refactor -> Move` 把测试类移动到 `src/test/java` 下的同名包。如果 `src/test/java` 没有被识别为测试目录，右键该目录并选择 `Mark Directory as -> Test Sources Root`；然后重新加载 Maven 项目。不要把 JUnit 的作用域改成 `compile` 来绕过目录问题，否则测试框架会进入主代码的依赖范围。

## 27. MessageValidator 的 JUnit 测试应该怎么运行？

先保证生产类和测试类位于不同源码目录，但使用相同包名：

```text
src/main/java/cn/siyes/training/web/validation/MessageValidator.java
src/test/java/cn/siyes/training/web/validation/MessageValidatorTest.java
```

两个文件的第一行都应为：

```java
package cn.siyes.training.web.validation;
```

生产代码不能放在 `src/test/java`，否则最终 WAR 不会包含它；包名 `validation` 与目录名也必须统一。整理后在 IDEA 中右键 `MessageValidatorTest`，选择 `Run 'MessageValidatorTest'`，或者点击测试类/方法左侧的绿色运行图标。最终还要在根目录运行：

```powershell
mvn -pl 01-java-web-basics clean test
```

预期看到 4 个测试、0 个失败、0 个错误和 `BUILD SUCCESS`。为了确认测试确实能发现错误，可以临时把最大长度从 500 改成 499，确认 `acceptsExactly500Characters` 失败；随后恢复为 500 并重新运行到全部通过。故障值不能提交到 Git。

## 28. 如何把 Maven 生成的 WAR 部署到 IDEA Tomcat 复测？

日常使用的 `war exploded` 是展开目录，最终验收需要让 IDEA Tomcat 部署 `01-java-web-basics/target/01-java-web-basics.war`。在 `Run -> Edit Configurations -> tomcat-javaweb -> Deployment` 中停止并暂时移除旧的 `war exploded`，通过 `External Source` 选择该 WAR，Application context 保持 `/01_java_web_basics`，然后完全重启 Tomcat。

部署后先访问 `/01_java_web_basics/health`，预期 `200` 和 `{"status":"ok"}`。重新部署会使旧 Session 失效，因此必须重新登录取得新的 `JSESSIONID`，再访问 `/01_java_web_basics/api/messages`，预期 `200`。`404` 优先检查 Context Path，`401` 重新登录，`500` 检查 IDEA Tomcat 运行配置中的 JDBC 环境变量和服务端日志。完整操作和 curl 命令见 Module README 的 `6.7`。

## 29. Spring 中的 Repository 是否相当于 Java Web 中的 DAO？

**简短结论：在当前 Spring Core 练习中，可以把 `Repository` 理解为 Java Web 阶段的 DAO；二者都属于数据访问层，但命名强调的角度不同。**

两轮练习中的分层可以对应为：

```text
原生 Java Web：
Servlet -> Service -> DAO -> JDBC -> MySQL

当前 Spring Core：
应用入口 -> Service -> Repository -> JdbcTemplate -> MySQL
```

因此，在当前项目中：

```text
@Repository 标注的数据访问类 ≈ Java Web 阶段的 DAO
```

二者的区别主要体现在命名语义和 Spring 提供的容器能力上：

- `DAO` 是 Data Access Object 的缩写，强调它是负责访问数据库的对象，方法通常直接体现增删改查和 SQL 操作。
- `Repository` 强调它是业务对象或领域对象的存取入口，方法通常使用 `findById`、`save`、`updateBalance` 等业务含义更明确的名称。
- `@Repository` 是 Spring 的组件注解。配合组件扫描后，Spring 会把这个类注册为 Bean，因而可以通过构造器注入 `JdbcTemplate` 等依赖。
- 在启用了 Spring 持久层异常转换的环境中，`@Repository` 还可以参与把部分底层数据库异常转换为 Spring 统一的 `DataAccessException` 异常体系；不能把它理解成仅仅换了一个 DAO 类名。

当前注解练习中的数据访问类可以写成：

```java
@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int updateBalance(Long accountId, BigDecimal amount) {
        // 使用 jdbcTemplate 执行 SQL
    }
}
```

这里的 `AccountRepository` 与 Java Web 阶段的 `AccountDao` 承担同一层职责：封装数据库访问，不负责 HTTP 响应，也不承担完整的转账业务流程。事务边界通常放在调用多个 Repository 方法的 Service 层。

后续学习 MyBatis 时还会遇到 `Mapper`：

```text
DAO        通用的数据访问层名称
Repository Spring 项目中常见的数据存取层名称
Mapper     MyBatis 中负责 Java 方法与 SQL 映射的接口名称
```

它们在典型分层中都位于 Service 和数据库之间。现阶段使用 `Repository` 命名，是为了熟悉 Spring 项目的常见表达；重点仍然是理解它负责封装数据访问，而不是死记名称。
