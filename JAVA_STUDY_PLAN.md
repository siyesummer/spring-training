# Java 与 Spring 阶段实战计划

> 本文件属于 `E:\本地项目\java-project\spring-training` 项目。
> Java 长期能力判断和路线总览见知识库的 [Java学习总结与Spring训练规划.md](E:\github项目\frontend-knowledge\Java学习总结与Spring训练规划.md)。
> 本文件维护具体 Module、实战要求、验收标准和阶段复盘；每次学习进展维护在同项目的 `LEARNING_PROGRESS.md`。
> 当前 Java 能力判断和两个手写项目的客观评价见同项目的 `CURRENT_LEVEL.md`。

## 一、训练目标

通过一组逐步递进的 Module，把“学过 Java Web 和 Spring”转化为能理解设计并独立动手的工程能力：

```text
理解概念 -> 手写最小例子 -> 完成 Module
  -> 解释机制和设计取舍 -> 核心手动验收 -> 适量工程化补充
```

最终需要达到：

- 能看懂 AI 生成的 `linux-server`，并能审查其中的设计、实现和潜在问题。
- 能说明 `linux-server` 从启动、接收请求、校验参数、执行 Service 到返回响应的完整流程。
- 能不依赖复制现有项目，从零搭建一个 Spring Boot + MySQL + REST API 项目。
- 能完成构建、测试、打包、启动、日志排查和基础部署。

重要边界：`E:\本地项目\java-project\linux-server` 主要由 AI 生成，不是用户独立手写完成的项目。它是对照阅读和代码审查材料，不是已掌握能力的证明。每个阶段的能力必须由 `spring-training` 中用户自己完成的代码和验收证据证明。

## 二、Module 总览

| Module | 学习阶段 | 建议产物 | 核心验收结果 |
| --- | --- | --- | --- |
| `01-java-web-basics` | Servlet / HTTP / Tomcat / JDBC | 原生 Web 小服务 | 能解释请求生命周期，并完成参数、Cookie、Session、Filter 和 JDBC 操作 |
| `02-spring-core` | IoC / DI / Bean 生命周期 / AOP / 事务基础 | 一个 Spring 容器练习，分纯 XML 和纯注解两轮 | 能分别用 XML 与注解完成 Bean 创建、注入、生命周期、代理和事务，并解释两种配置方式的差异 |
| `03-spring-mvc` | MVC 请求处理 | REST API 小项目 | 能完成路由、参数绑定、校验、统一响应和异常处理 |
| `04-mybatis` | Mapper / SQL 映射 | 数据访问项目 | 能完成 CRUD、动态 SQL、事务、分页并解释对象映射 |
| `05-spring-boot` | Boot 自动配置与工程化 | 可运行 Boot 服务 | 能独立创建、配置、测试、打包和运行项目 |
| `06-spring-boot-comprehensive` | 综合项目 | 接近真实业务的小服务 | 能完成设计、开发、测试、日志、配置、部署和回滚说明 |

Module 名称可以调整，但不要把所有阶段都堆在同一个包里。每个 Module 都应该能独立回答：学了什么、实现了什么、怎么验证、还不会什么。

## 三、通用完成标准

每个 Module 至少包含：

- `README.md`：阶段目标、知识点、启动命令、请求示例和已知问题。
- 可运行源码：不能只有注释或课堂代码片段。
- 核心手动验收：证明主要技术机制和功能可用。
- 关键设计说明：用自己的话解释调用链、生命周期和核心取舍。
- 测试、异常路径和构建记录：按阶段风险与学习目标安排，作为辅助工程能力，不追求数量。

达到以下条件才允许标记为“已完成”：

- 不看教程能够重新写出最小版本。
- 能解释核心注解、接口、配置和调用链。
- 能修改需求，而不是只能复制原示例。
- 能处理至少一种有代表性的失败或边界场景，并解释原因。
- 能理解必要测试的对象、覆盖范围和边界；不要求早期 Module 把测试作为主要成果。
- 原则上逐阶段建立知识映射；`01-java-web-basics` 可暂不对照 `linux-server`，从 Spring MVC/Spring Boot 阶段开始正式对照，且不把 AI 生成代码当成自己的实现。

## 四、各 Module 详细要求

### 4.1 `01-java-web-basics`

本 Module 专注原生 Servlet、Filter、Session、JDBC 和事务，不要求对照 `linux-server`。等进入 Spring MVC 或 Spring Boot 阶段、已经具备 Controller、Service、配置和测试概念后，再进行真实项目映射与复盘。

必须掌握：

- HTTP 请求方法、状态码、请求头、响应头和 Cookie。
- Servlet 生命周期、请求和响应对象、转发与重定向。
- Filter、Listener、Session 的作用和执行顺序。
- Tomcat 如何接收请求并调用 Servlet。
- JDBC 连接、预编译 SQL、结果集、事务和连接池基本原理。
- `try-with-resources`、字符集和请求参数处理。

建议项目：用户注册登录和留言板，使用原生 Servlet + JSP，或纯 HTML 前端 + Servlet API，不用 Spring 隐藏底层流程。

本项目的实际演练环境已安装 MySQL，因此 `01-java-web-basics` 使用 MySQL + 原生 JDBC，不再以 H2 作为主数据库。H2 仅作为理解嵌入式数据库或隔离测试的备选方案；本阶段需要实际处理数据库服务启动、专用账号、连接配置、字符集和连接失败。

必须能回答：

- 一个请求从浏览器到 Servlet 经历了什么。
- Cookie 和 Session 如何配合。
- 为什么使用 `PreparedStatement`。
- 事务什么时候提交，异常时如何回滚。
- Filter 和 Servlet 的执行顺序是什么。

验收建议：使用浏览器或 HTTP 客户端完成注册、登录、留言、退出登录，并手写 `ServletContextListener` 或 `HttpSessionListener` 观察生命周期回调；未登录、错误参数、重复用户名和数据库异常按需要选择性验证。

### 4.2 `02-spring-core`

必须掌握：

- IoC 容器解决了什么问题。
- Bean 的创建、注入、初始化和销毁生命周期。
- 构造器注入与字段注入的取舍。
- `@Component`、`@Service`、`@Repository`、`@Configuration`、`@Bean` 的作用。
- AOP 的连接点、切点、通知、代理和适用场景。
- 声明式事务的基本原理和事务失效的常见原因。
- 为什么同一个类内部方法调用可能绕过代理。

本 Module 只创建一个普通 Maven `jar` 项目，分两轮完成同一个账户扣款与操作日志练习：

1. 第一轮使用纯 XML：`ClassPathXmlApplicationContext`、`<bean>`、构造器/Setter 注入、XML AOP 和 XML 事务配置；业务类不使用 Spring 组件、配置、切面和事务注解。
2. 第二轮使用纯注解 / Java 配置：`AnnotationConfigApplicationContext`、组件注解、`@Configuration`、`@Bean`、`@Aspect`、`@EnableAspectJAutoProxy` 和 `@Transactional`，重新实现同一组能力。
3. 两轮代码放在不同包中，先完成 XML 轮并复盘，再开始注解轮；不能在 XML 轮提前混入注解。

两轮先通过普通 Probe 观察 IoC、DI、Bean 生命周期和代理机制，再接入现有 MySQL，用账户转账和操作日志验证声明式事务。数据库连接配置保持外置，避免凭据进入仓库。

当前详细引导维护在：

```text
02-spring-core/README.md
02-spring-core/docs/XML_GUIDE.md
02-spring-core/docs/XML复盘.md
02-spring-core/docs/ANNOTATION_GUIDE.md
02-spring-core/docs/ANNOTATION复盘.md
```

完成状态：`已完成`（2026-08-20）。XML 与注解两轮均已完成 IoC/DI、生命周期、AOP、事务提交与回滚、后置处理器和自调用代理边界验收；详细证据见 Module README 与 `LEARNING_PROGRESS.md`。

实际项目：账户转账与操作日志，加入耗时切面、事务边界、后置处理器和自调用代理实验；XML 轮与注解轮使用同一业务主题进行对照。测试或 Maven 命令只作为辅助确认，重点是观察容器、代理和事务行为。

验收建议：打印或断点观察 Bean 生命周期；分别验证 XML 和注解下的构造器注入；验证切面是否在目标方法前后执行；制造异常确认事务能够回滚；单独验证内部方法调用导致的代理失效场景；最后用自己的话比较两轮配置差异。

### 4.3 `03-spring-mvc`

完成状态：`已完成`（2026-08-22）。传统 Maven WAR 已部署到 Tomcat 10.1；使用 `web.xml` 显式注册 `DispatcherServlet`，Java 配置类启用 MVC，不使用 Spring Boot。已手写 MySQL 任务管理 REST API，并用 `JdbcTemplate` 完成真实持久化；`04-mybatis` 将继续围绕数据访问层练习 Mapper 与动态 SQL。

必须掌握：

- `DispatcherServlet`、HandlerMapping、Controller、参数解析和消息转换器。
- `@RequestMapping`、`@GetMapping`、`@PostMapping` 的路由匹配。
- `@PathVariable`、`@RequestParam`、`@RequestBody`、`@ModelAttribute` 的区别。
- JSON 序列化和反序列化。
- Bean Validation、`@Valid` / `@Validated` 和统一错误响应。
- `@RestControllerAdvice` 的异常处理。
- CORS、分页参数和 REST API 状态码设计。

建议项目：任务管理 API，至少包含任务增删改查、分页、状态过滤、参数校验和统一异常响应。

验收建议：使用 MockMvc 或 HTTP 客户端覆盖成功请求、缺少参数、类型错误、校验失败、业务异常和未知异常；确认响应状态码和响应体格式稳定。

验收结果：Apifox 已验证创建 `201`、查询与分页 `200`、更新、状态修改、删除、DTO 校验 `400` 和任务不存在 `404`；Navicat 已确认数据库结果；Interceptor 已打印 `TaskController.queryById` 和最终状态；复盘能够说明 HandlerMapping、HandlerAdapter、参数解析器、消息转换器、校验与异常处理链。2026-08-22 执行 Module Maven 构建成功并生成 WAR，详细证据见 `03-spring-mvc/README.md` 与 `LEARNING_PROGRESS.md`。

### 4.4 `04-mybatis`

完成状态：`已完成`（2026-08-25）。本阶段使用同一个 WAR Module，完成“原生 MyBatis”和“Spring + MyBatis + Spring MVC”两段；SQL 以 XML 为主，使用少量注解 SQL 对照，不引入 Spring Boot 或 MyBatis-Plus。

必须掌握：

- `SqlSessionFactory`、Mapper、映射文件或注解的关系。
- `#{}` 与 `${}` 的区别以及 SQL 注入风险。
- 结果映射、关联查询、动态 SQL 和批量操作。
- MyBatis 如何参与 Spring 事务。
- 分页查询、排序白名单和索引意识。
- 数据库字段、Java 属性、DTO 和领域对象之间的边界。

建议项目：延续任务管理领域，使用独立数据库和 `tasks`、`task_comments` 两张一对多关联表。第一段通过普通 Java 入口练习原生 MyBatis；第二段接入 Spring 事务和已有的 Spring MVC 请求层，用 MyBatis Mapper 替换 `JdbcTemplate` Repository。

验收建议：完成 CRUD、条件查询、分页、关联查询和事务回滚；检查 SQL 日志；验证恶意排序字段不能直接拼接进 SQL；说明每个 Mapper 方法对应的 SQL 和返回对象。

引导入口：`04-mybatis/README.md`、`04-mybatis/docs/MYBATIS_STANDALONE_GUIDE.md` 和 `04-mybatis/docs/MYBATIS_SPRING_GUIDE.md`。

验收结果：原生阶段完成 `SqlSessionFactory` / `SqlSession` / Mapper 代理链路、XML CRUD、动态 SQL、分页与排序白名单、批量评论、一对多 JOIN 映射、一级缓存及手动提交/回滚；Spring 集成阶段完成 `SqlSessionFactoryBean`、`@MapperScan`、`SqlSessionTemplate`、Spring MVC 接口和 `@Transactional` 多 Mapper 事务。Apifox 与 Navicat 已验证 CRUD、任务评论关联、输入校验、任务不存在 `404` 和事务回滚；2026-08-25 Maven 离线打包成功，生成 `04-mybatis.war`。复盘见 `04-mybatis/docs/Mybatis验收.md`。

### 4.5 `05-spring-boot`

完成状态：`已完成`（2026-09-01）。本 Module 使用 Spring Boot `4.0.8`、Java `21` 和可执行 Jar，分两轮完成：第一轮为 Boot + JdbcTemplate，第二轮为 Boot + MyBatis。

必须掌握：

- Starter、自动配置、组件扫描和主启动类。
- `application.yml`、环境变量、Profile 和配置绑定。
- Maven 生命周期、依赖管理、插件、可执行 JAR 和依赖排查。
- Spring Boot 测试、MockMvc、切片测试和集成测试的区别。
- 日志配置、健康检查和基础 Actuator 使用。
- 如何从零创建项目，而不是依赖现有项目复制改名。

建议项目：将前面的任务服务整理为一个完整 Boot 项目，完成配置外置、打包和本地真实启动；在第二轮接入 MyBatis，保留对底层 Mapper 与事务机制的观察。

验收结果：已通过 Apifox 验证任务 CRUD、条件分页、评论和详情接口；通过 Navicat 对照数据库并验证事务回滚；通过 Profile、环境变量和 Actuator 观察配置与运行状态；完成 SQL 日志、自动配置和 Mapper 扫描观察；执行 `mvn package` 生成可执行 Jar，并用 `java -jar` 独立启动后复测接口。第一轮和第二轮复盘分别见 `05-spring-boot/docs/一阶段复盘.md`、`05-spring-boot/docs/二阶段复盘.md`，自动配置触发过程见 `05-spring-boot/docs/自动配置导入流程.md`。

当前能力边界：已具备 Starter、条件自动配置、配置来源、Profile、JdbcTemplate、MyBatis、事务和可执行 Jar 的基础独立实践能力；尚未覆盖复杂自动配置源码、自定义 Starter、多数据源、生产配置、完整集成测试、高并发与复杂事务传播。

### 4.6 `06-spring-boot-comprehensive`

完成状态：`已完成`（2026-09-03）。项目为独立 Spring Boot `4.0.8`、Java `21`、Maven、Jar Module，包名为 `cn.siyes.training.comprehensive`。第一版主题为订单与库存管理系统。

综合项目不追求功能数量，而追求完整链路：

```text
需求 -> 数据建模 -> API 设计 -> 分层实现 -> 参数校验
  -> 事务与并发边界 -> 测试 -> 日志 -> 打包 -> 部署 -> 验收 -> 回滚
```

至少应包含：

- 用户或业务主体、核心业务数据和操作记录。
- REST API、统一响应、统一异常处理和参数校验。
- MySQL 表结构、索引、事务和幂等处理。
- Service 层业务规则，Controller 不直接堆业务逻辑。
- 单元测试、Web 层测试和至少一类数据库集成测试。
- 外部化配置，敏感信息不进入 Git。
- Docker 或服务器部署说明、健康检查、日志查看和回滚步骤。

引导与复盘：`06-spring-boot-comprehensive/docs/COMPREHENSIVE_GUIDE.md`、`06-spring-boot-comprehensive/docs/复盘.md`。第一版已完成商品库存查询、订单创建、订单详情、订单取消、库存条件更新、操作日志和事务回滚；按学习边界未引入支付、Redis、消息队列、微服务或安全认证。

验收结果：已使用 Apifox 验证订单创建、订单详情、取消、重复取消、库存不足和异常恢复；使用 Navicat 对照订单、明细、库存和操作日志，确认成功路径多表一致以及异常路径整体回滚；执行 `mvn package` 生成可执行 Jar，并使用 `java -jar` 独立启动后复测接口。当前业务异常遵循 HTTP `200` + 响应体业务 `code` 的项目约定，参数校验和系统异常按实现返回 HTTP `400` / `500`。

能力结论：已具备将 Spring Boot、Spring MVC、MyBatis、事务、数据库建模和外部配置组合成学习型单体业务服务的基础独立实践能力；并发压力、幂等、异步一致性、生产级测试和部署治理仍属于后续提升内容。

最终要提交的不只是代码，还应包括：

- 一张系统结构图。
- 一张核心请求时序图。
- 数据库表结构和关键索引说明。
- 构建、启动、验收、日志排查和回滚文档。
- 已知限制和下一步改进计划。

## 五、与 `linux-server` 的对照顺序

`linux-server` 主要由 AI 生成，不能直接照抄。建议在每个阶段完成后，按以下方式对照：

1. 先不看实现，自己画出本阶段相关的调用链。
2. 独立写一个最小版本并完成测试。
3. 再阅读 `linux-server` 对应代码。
4. 标记其中使用到的框架机制、工程约定和可能的问题。
5. 用自己的话写一段“为什么这样设计、我是否会这样设计”。
6. 必要时对 AI 生成代码提出修改意见，修改后重新测试。

建议对照内容：

- 启动类、组件扫描和 Maven 配置。
- Controller、DTO、Service、配置类和异常处理的分工。
- `application.yml` 与 `@ConfigurationProperties` 的绑定。
- `JdbcTemplate`、SQL、事务和数据库初始化。
- CORS、参数校验、统一响应和测试。
- Maven 打包、可执行 JAR、日志和部署入口。

## 六、阶段复盘模板

每个 Module 完成后，在该 Module 的 `README.md` 或进度记录中回答：

```markdown
## 本阶段完成情况

- 完成日期：
- 当前状态：
- 独立实现了什么：
- 使用了哪些核心 API / 注解 / 配置：
- 请求或数据调用链：
- 正常路径测试：
- 异常路径测试：
- 主动制造并排查的故障：
- 对照 linux-server 看到的相似设计：
- AI 生成代码中发现的问题：
- 目前仍不理解的内容：
- 下一阶段计划：
```

## 七、当前起点

- Java 基础：已完成第一轮学习和两个手写项目实践。
- Java Web + Spring：`01-java-web-basics`、`02-spring-core`、`03-spring-mvc`、`04-mybatis`、`05-spring-boot` 和 `06-spring-boot-comprehensive` 第一版均已完成。
- `spring-training`：已完成原生 Java Web、Spring Core、Spring MVC、MyBatis、Spring Boot 基础阶段和 `06-spring-boot-comprehensive` 第一版综合业务练习；后续根据整体学习计划继续补强幂等、并发、测试和部署等工程能力。
- `linux-server`：主要由 AI 生成，当前仅作为对照阅读和代码审查对象。
