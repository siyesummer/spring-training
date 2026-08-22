# Module 03：Spring MVC 请求处理实战

> 当前状态：`已完成`
> 开始日期：2026-08-21
> 完成日期：2026-08-22
> 项目类型：传统 Maven WAR，部署到 Tomcat 10.1
> 核心目标：把已经掌握的 Servlet 请求链与 Spring Core Bean 管理连接起来，独立完成一个最小 REST API。

## 1. 本阶段为什么不用 Spring Boot

本 Module 暂时不使用 Spring Boot。应用继续以 WAR 形式部署到外部 Tomcat，并由你手动注册 Spring MVC 的 `DispatcherServlet`。

这样可以直接观察：

```text
浏览器 / HTTP 客户端
  -> Tomcat
  -> Servlet Filter 链
  -> DispatcherServlet
  -> HandlerMapping
  -> HandlerAdapter
  -> Controller Bean
  -> Service Bean
  -> Repository Bean
  -> 返回值处理器 / HttpMessageConverter
  -> HTTP 响应
```

Spring Boot 后续会自动完成 Tomcat 启动、`DispatcherServlet` 注册和大量 MVC 默认配置。如果本阶段直接使用 Boot，代码更少，但不利于分清哪些能力来自 Servlet 容器、Spring Core、Spring MVC 和 Boot。

## 2. 实战主题与边界

本阶段实现一个“任务管理 REST API”，包括：

- 新增任务。
- 根据 ID 查询任务。
- 分页、关键字和状态筛选任务。
- 修改任务基本信息。
- 修改任务状态。
- 删除任务。
- 请求参数校验。
- 统一错误响应和全局异常处理。
- MVC Interceptor 和 CORS 配置。
- HikariCP、`JdbcTemplate` 与 MySQL 的真实持久化链路。

本阶段直接使用 MySQL，但数据访问保持为已经练习过的 Spring `JdbcTemplate`，不提前引入 MyBatis。这样既能验证真实的 HTTP -> Spring MVC -> Service -> Repository -> MySQL 链路，又能把新知识重点保持在 Spring MVC。`04-mybatis` 会继续使用同一张任务表，把 JDBC Repository 替换成 Mapper，从而比较两种数据访问方式。

本阶段不引入：

- Spring Boot。
- MyBatis、JPA；本轮数据库访问只使用 `JdbcTemplate`。
- Spring Security。
- JSP 或 Thymeleaf 页面渲染。
- 复杂认证授权和生产级分页组件。

## 3. 配置方案

本 Module 使用：

```text
web.xml
  -> 注册 DispatcherServlet
  -> 指定 AnnotationConfigWebApplicationContext
  -> 加载 WebMvcConfig

WebMvcConfig
  -> @EnableWebMvc
  -> @ComponentScan
  -> MVC Interceptor
  -> CORS
```

这是“Servlet 配置显式可见 + Controller 使用现代注解”的组合。不会重复做 Spring Core 的纯 XML / 纯注解双轮练习，也不会让 Boot 隐藏启动过程。

第一版只创建一个由 `DispatcherServlet` 管理的 `WebApplicationContext`，Controller、Service 和 Repository 都在该容器中。传统项目还可以通过 `ContextLoaderListener` 创建父容器，再让 `DispatcherServlet` 创建 MVC 子容器；当前业务规模不需要先增加这层复杂度，后续引导会说明它的边界。

## 4. 文档入口

详细演练步骤见：

- [SPRING_MVC_GUIDE.md](docs/SPRING_MVC_GUIDE.md)
- [SPRING_MVC_CONTEXT_HIERARCHY.md](docs/SPRING_MVC_CONTEXT_HIERARCHY.md)：单容器、父子容器、扫描隔离及当前项目的完整配置示例。
- [SPRING_MVC复盘.md](docs/SPRING_MVC复盘.md)：请求链、参数解析、消息转换、校验、异常处理和框架边界复盘。

建议严格按检查点逐步完成，不要一次性复制全部代码：

```text
依赖和 WAR 配置
  -> DispatcherServlet 启动
  -> 最小 /api/health
  -> 路由和四种参数来源
  -> JSON 与 DTO
  -> MySQL、JdbcTemplate CRUD 和分层
  -> Bean Validation
  -> 全局异常处理
  -> Interceptor 与 CORS
  -> 完整请求链复盘
```

## 5. 预期包结构

```text
03-spring-mvc/
├─ pom.xml
├─ README.md
├─ docs/
│  ├─ SPRING_MVC_GUIDE.md
│  ├─ SPRING_MVC_CONTEXT_HIERARCHY.md
│  └─ SPRING_MVC复盘.md
└─ src/
   ├─ main/
   │  ├─ java/cn/siyes/training/mvc/
   │  │  ├─ config/
   │  │  ├─ controller/
   │  │  ├─ dto/
   │  │  ├─ exception/
   │  │  ├─ interceptor/
   │  │  ├─ model/
   │  │  ├─ repository/
   │  │  └─ service/
│  ├─ resources/
│  │  └─ db/schema.sql
│  └─ webapp/WEB-INF/web.xml
   └─ test/
```

## 6. 阶段完成标准

本 Module 只有满足以下条件才标记为 `已完成`：

- 能解释 Tomcat、`DispatcherServlet`、HandlerMapping、HandlerAdapter 和 Controller 的调用关系。
- 能独立写出 `@RequestMapping`、`@PathVariable`、`@RequestParam`、`@RequestBody` 和 `@ModelAttribute` 的最小示例。
- 能说明 JSON 在什么位置转换为 Java DTO，以及返回对象如何转换为 JSON。
- 能完成任务 CRUD、筛选与简单分页，并保持 Controller、Service、Repository 职责清晰。
- 能说明 HikariCP、`DataSource`、`JdbcTemplate`、Repository 与 MySQL 的连接关系，并在 Navicat 验证数据持久化。
- 能使用 Bean Validation 校验请求，并通过全局异常处理返回合适的 HTTP 状态码。
- 能说明 Servlet Filter 与 Spring MVC Interceptor 的执行位置和能力差异。
- 至少完成一条完整成功链路和一种代表性错误链路的真实 HTTP 验证。
- 能对照 `linux-server` 找出 Controller、DTO、Service、配置和异常处理的对应位置，但不照抄其实现。

构建命令和自动化测试只作为辅助证据。本阶段最重要的成果是你能手写主要代码，并用自己的话解释一次请求怎样经过 Spring MVC。

## 7. 阶段验收结果（2026-08-22）

- 部署与入口：传统 Maven WAR 已部署到 Tomcat 10.1，`web.xml` 显式注册 `DispatcherServlet`，实际 Context Path 为 `/03_spring_mvc`。
- 功能链路：Apifox 已验证任务创建、按 ID 查询、关键字与状态筛选分页、基本信息修改、状态修改和删除；Navicat 已确认新增、更新和删除结果与接口一致。
- HTTP 与 JSON：创建返回真实 HTTP `201`；成功查询返回 `200`；Java 时间类型已输出 ISO 字符串；删除按本项目约定返回 `200 + JSON`。
- 校验与异常：空标题触发 Bean Validation 并返回真实 HTTP `400`；不存在任务由 Service 抛出 `TaskNotFoundException`，经 `@RestControllerAdvice` 转换为稳定的 `404 JSON`；兜底异常返回固定 `500` 信息，不向前端暴露内部异常详情。
- MVC 机制：四种主要参数来源均已在代码中使用；Interceptor 日志确认请求匹配到 `TaskController.queryById`，并在 `afterCompletion` 观察到最终状态 `200`。
- 持久化与分层：HikariCP、`JdbcTemplate`、`TaskRepository`、Service 和 Controller 的连接关系已跑通；分页列表与总数复用相同筛选条件，Service 依赖 Repository 接口。
- 构建证据：2026-08-22 执行 `mvn -s C:\Users\siyesummer\.m2\settings.xml -pl 03-spring-mvc -am clean package -DskipTests`，根项目与 `03-spring-mvc` 均为 `SUCCESS`，20 个主源码文件编译成功并生成 `target/03-spring-mvc.war`。
- 复盘结论：能够解释 Tomcat、Filter、`DispatcherServlet`、HandlerMapping、Interceptor、HandlerAdapter、参数解析、Controller、返回值处理和消息转换器的主要调用关系。

结论：本 Module 的核心实现、真实 HTTP 请求、数据库结果、Interceptor 观察和机制复盘均已形成证据闭环，状态更新为 `已完成`。当前能力定位是能够独立完成并解释最小 Spring MVC REST API，不等同于掌握 Spring MVC 内部源码或生产级 Web 工程设计。
