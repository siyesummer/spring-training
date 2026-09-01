# 当前 Java 能力基线

> 评估日期：2026-09-01
> 评估依据：用户手写的 `tank-game`、`chat-room`，`01-java-web-basics` 中独立完成的原生 Java Web 实践，`02-spring-core` 中完成的 XML/注解双轮 IoC、DI、生命周期、AOP、事务和后置处理器实践，`03-spring-mvc` 中独立完成的传统 WAR 任务 REST API，`04-mybatis` 中完成的原生 MyBatis 与 Spring 集成实践，以及 `05-spring-boot` 中完成的 JdbcTemplate/MyBatis 两轮、自动配置、Profile、可执行 Jar 和真实 HTTP/数据库验收。
> `linux-server` 主要由 AI 生成，只作为阅读和审查对象，不计入用户独立编码能力。

## 一、当前阶段定位

当前水平可以判断为：

> 已完成 Java 基础、Java Web 基础、Spring Core、Spring MVC、MyBatis 和 Spring Boot Module 的实践闭环；能够独立完成学习型 Java Web 服务、Spring 容器与事务练习、传统 WAR + Tomcat 下的 REST API、原生和 Spring 集成两种 MyBatis 数据访问实现，以及基于 Boot 的 JdbcTemplate/MyBatis 服务。下一阶段进入 `06-spring-boot-comprehensive`，重点验证综合设计和工程化能力。

这意味着：

- 已经越过只会语法、控制台小题和单文件练习的阶段。
- 具备进入 Spring Boot 综合项目、继续审查自动配置结果并整合既有技术的基础。
- 目前具备的是“能够独立完成和解释学习型 Java Web、Spring Core、Spring MVC、MyBatis 与 Spring Boot 最小项目”的能力，暂时不能认定已经具备独立设计生产级 Java 服务的能力。
- 已具备 Spring Boot 的基础独立实践能力，但能力范围仍限于学习型单体服务；尚未达到生产级 Boot 工程设计水平。

## 二、已有能力与代码证据

### 2.1 Java 语言与面向对象

已经能够使用：

- 类、对象、构造方法、封装、继承和枚举。
- 集合保存和管理业务对象。
- 异常处理、文件读写、Properties 和资源文件。
- 多个对象之间的状态协作和生命周期变化。

代码证据：

- `tank-game` 使用 `Tank`、`EnemyTank`、`Bullet`、`Boom`、`Direction` 等对象表达游戏角色和状态。
- `EnemyTank extends Tank` 表明已能使用继承表达类型关系。
- `chat-room` 使用 `User`、`Message`、`MessageType` 表达聊天领域数据。

当前判断：面向对象语法已会用，但职责划分、依赖方向、抽象边界和可测试设计还需要通过后续 Module 加强。

### 2.2 线程与并发入门

已经实际使用：

- `Runnable` 和 `Thread`。
- 阻塞等待和循环任务。
- 游戏更新线程、子弹线程、敌方坦克线程和连接处理线程。
- 多个线程共同访问对象和集合。

代码证据：

- `tank-game` 中敌方坦克、子弹和游戏面板分别执行周期性任务。
- `chat-room` 为客户端连接维护服务端和客户端处理线程。

当前边界：目前属于“会创建和使用线程”，尚未系统证明掌握线程池、锁、内存可见性、原子性、任务取消、并发容器和完整的线程安全设计。

### 2.3 网络编程

已经实际完成：

- TCP Socket 客户端与服务端连接。
- `ServerSocket` 监听和 `Socket` 双向通信。
- UDP 基础练习。
- 私聊、群聊、在线连接管理和文件传输。

代码证据：

- `tank-game` 中有 TCP、UDP 和文件传输练习。
- `chat-room` 完成注册登录、在线用户、私聊、群发和文件发送的客户端/服务端闭环。

当前判断：已经理解基础网络通信链路，但 BIO 并发模型、消息边界协议、断线重连、半包粘包、超时、背压和安全认证仍需要继续学习。

### 2.4 数据库和 JDBC 入门

已经实际接触：

- MySQL 建库建表和基本 SQL。
- JDBC 数据源和连接池。
- Druid、DbUtils 和 DAO 封装。
- 数据库配置外置。
- 实体对象与表数据的基本映射。

代码证据：

- `chat-room` 有 `BasicDAO`、实体 DAO、`JDBCUtils` 和 `MySQLService`。
- 用户和消息数据能够持久化到 MySQL。
- 发布资料包含数据库初始化和连接配置说明。

当前判断：已经在 `01-java-web-basics` 中独立使用 `PreparedStatement`、`ResultSet`、生成主键、`try-with-resources` 和显式事务完成 MySQL 写入；能够解释同一 Connection 如何覆盖两次写操作，以及 `commit()` / `rollback()` 的边界。

当前边界：连接池、批量操作、索引设计、SQL 性能、隔离级别、并发一致性和生产级数据访问抽象还没有得到系统训练。现有代码中 `UserDao.existsByUsername` 的 `ResultSet` 没有显式使用 `try-with-resources`，说明资源管理原则已经会用，但还没有做到所有路径都一致。

### 2.5 工程交付意识

已经表现出：

- 不只关注源码，也关注启动、打包和给其他人运行。
- 能编写 README、启动脚本和 PowerShell 构建脚本。
- 能处理依赖、资源复制、JAR Manifest、编码和发布目录。

代码证据：

- `chat-room` 有 `build.ps1`、打包指南、发布目录和客户端/服务端启动脚本。
- 已经处理过 Windows 中文路径、命令行编码和外部配置问题。

当前判断：工程意识是明显优势。已经迁移到 Maven 标准目录，完成了环境变量、日志、WAR 构建和 Tomcat 部署；后续需要继续加强统一响应、异常分层、依赖注入、可重复验收和生产配置管理。

### 2.6 `01-java-web-basics` 代码能力评价

本 Module 的代码能够证明以下能力已经从“看过概念”进入“亲手实现并运行”：

- 能用 `web.xml` 配置 Servlet、Filter 和 Listener，并理解 Tomcat 的对象创建与生命周期回调。
- 能把请求参数、Session、业务 Service、DAO 和 MySQL 串成可运行的注册、登录和留言流程。
- 能使用 `getSession(false)` 做登录状态判断，使用 Session 保存用户身份，并在登录时调用 `changeSessionId()`。
- 能使用 BCrypt 保存密码哈希，而不是保存明文密码；能将数据库唯一约束异常转换为业务冲突。
- 能让多个 DAO 共用同一条 Connection，在 Service 层控制事务提交与回滚，并通过故障验证回滚结果。
- 能完成从 Maven 编译、WAR 打包到 IDEA/Tomcat 部署的基本交付闭环。

代码中体现出的主要优点：

- 分层方向基本正确：Servlet 负责 HTTP 适配，Service 负责业务流程，DAO 负责 SQL，Filter/Listener 负责容器横切机制。
- 能主动处理资源生命周期，而不是把 Connection 保存为静态全局对象；事务边界放在 Service 也符合业务操作组合的职责。
- 能使用参数化 SQL、唯一索引和 BCrypt，已经具备基本的安全意识和数据一致性意识。
- 能通过实际错误验证设计，而不是只验证成功页面，说明有初步的问题定位和边界意识。

代码中需要继续改进的边界：

- `UserDao.existsByUsername` 没有显式关闭 `ResultSet`；资源所有权和关闭习惯需要在后续 Module 中做到一致。
- Service 和 Servlet 每次手动 `new` DAO/Service，适合本阶段理解调用关系，但还没有体现依赖注入、对象复用和容器管理，这正是 Spring Core 要解决的问题。
- `BaseServlet` 使用 `String[]` 传递参数并用 `null` 作为失败信号，`createResponse` 和留言响应手动拼接 JSON，尚未形成类型安全的请求对象、响应对象和统一异常处理。
- 当前 JSON 拼接没有完整处理引号、换行和 HTML 等字符的转义；这不影响本阶段理解 Servlet 链路，但不能直接作为生产 API 响应方案。
- `FirstSessionListener` 使用普通 `int` 统计并发 Session，适合观察回调，不足以作为线程安全的在线人数实现。
- `web.xml` 仍使用旧 Java EE 4.0 命名空间，而代码使用 Jakarta Servlet 6 API；Tomcat 当前能够运行，但后续应统一为 Jakarta 6 的描述符格式。

综合评价：你已经达到“能够独立手写并解释原生 Java Web 基础项目”的阶段，具备进入 Spring Core 的条件。当前最重要的提升方向不是继续堆 Servlet API，而是理解 Spring 如何用 IoC、DI、Bean 生命周期、代理和 AOP 管理并抽象这些手动代码。

### 2.7 `02-spring-core` 代码能力评价

本 Module 的 XML 与注解两轮代码能够证明以下能力已经从“看过概念”进入“亲手实现并运行”：

- 能分别使用 `ClassPathXmlApplicationContext` 和 `AnnotationConfigApplicationContext` 启动 Spring 容器。
- 能通过 XML `<bean>`、组件扫描和 `@Bean` 形成 BeanDefinition，并使用构造器或 Setter 完成依赖注入。
- 能区分 BeanDefinition、目标对象和代理对象，理解 `getBean()` 可能返回代理或包装对象。
- 能通过 XML AOP 和 `@Aspect` / `@Around` 创建耗时切面，并解释 `proceed()` 决定是否继续调用目标方法。
- 能通过 XML 事务配置和 `@Transactional` 完成正常提交与异常回滚，理解事务管理器与 `JdbcTemplate` 共享事务 Connection 的关系。
- 能手写 `BeanDefinitionRegistryPostProcessor`、`BeanFactoryPostProcessor` 和 `BeanPostProcessor`，解释定义层修改、实例层处理和最终 Wrapper 的差异。
- 能通过事务状态与 AOP 日志证明 `this` 自调用绕过代理，并通过拆分到另一个注入的 Service 重新进入代理。

代码和复盘体现出的主要优点：

- 没有停留在背诵注解，而是使用相同业务分别实现 XML 与注解配置，能比较两种 BeanDefinition 来源。
- 通过正常提交、主动异常回滚和删除异常后的恢复提交，实际验证了声明式事务边界。
- 能根据运行现象修正 `@Configurable` / `@Configuration`、JDBC URL、余额判断和金额校验等问题。
- 能将后置处理器与 AOP 自动代理创建联系起来，开始从容器扩展机制而不是 API 清单理解 Spring。

当前边界：

- 目前使用的是学习型 `DriverManagerDataSource`，尚未证明连接池配置和生产级数据源管理能力。
- 已理解 `REQUIRED`、`READ_COMMITTED` 和 `rollbackFor` 的当前用法，但复杂传播行为、隔离异常和并发事务尚未系统验证。
- 后置处理器练习能够说明扩展点时机，但尚未涉及循环依赖、早期代理或复杂处理器排序。
- Spring Core 练习本身不覆盖 Web 层；Spring MVC 能力已由后续 `03-spring-mvc` 单独完成验证，Spring Boot 自动配置则在 `05-spring-boot` 中完成了基础观察和验证。

综合评价：已具备 Spring Core 的基础独立实践能力，能够写出并解释最小 IoC、DI、生命周期、AOP 和事务实现。能力定位仍是学习型项目的扎实基础，不等同于生产级 Spring 工程经验。

### 2.8 `03-spring-mvc` 代码能力评价

本 Module 的传统 WAR 任务管理 API 能够证明以下能力已经从“看过概念”进入“亲手实现并运行”：

- 能通过 `web.xml` 在 Tomcat 10.1 中显式注册 `DispatcherServlet`，使用 `@EnableWebMvc`、组件扫描和 Java 配置启用 MVC 基础设施。
- 能使用 `@PathVariable`、`@RequestParam`、`@ModelAttribute` 和 `@RequestBody` 接收不同来源的参数，并解释各自的数据来源。
- 能借助 Jackson 完成 JSON 与 DTO 的双向转换，理解请求反序列化发生在 Controller 前、响应序列化发生在 Controller 返回后。
- 能使用 Bean Validation 与 `@Valid` 阻止非法请求进入 Controller，并区分校验失败与 JSON/枚举反序列化失败。
- 能使用 `@RestControllerAdvice`、`@ExceptionHandler` 和 `ResponseEntity` 将业务异常转换成真实 HTTP `400`、`404`、`500` 与统一 JSON 响应。
- 能完成任务创建、查询、筛选分页、修改、状态修改和删除，并通过 HikariCP、`JdbcTemplate`、Repository 与 MySQL 形成完整持久化链路。
- 能通过 `HandlerMethod` 和 Interceptor 日志观察具体 Controller 方法及最终响应状态，并说明 Filter、DispatcherServlet、Interceptor 和 Controller 的位置关系。
- 能解释 HandlerMapping 负责“找到谁”、HandlerAdapter 负责“怎样调用”，并将参数解析器、返回值处理器和消息转换器放入完整请求链。

代码和复盘体现出的主要优点：

- 没有使用 Spring Boot 隐藏启动过程，能够将 Servlet 入口、Spring Core Bean 管理和 Spring MVC 请求处理连接起来。
- DTO、Controller、Service、Repository 的职责边界已经基本形成；Service 依赖 `TaskRepository` 接口，为下一阶段替换 MyBatis 数据访问实现保留了边界。
- 不只验证成功 CRUD，还实际处理了 JDBC URL、JSON 构造、`@RequestBody`、JavaBean 序列化、枚举写库、分页总数、校验异常和业务 `404` 等问题。
- 使用 Apifox、Navicat、Tomcat 日志和复盘交叉验证代码行为，已经具备从 HTTP 现象追踪到框架层和数据层的初步排查能力。

当前边界：

- 对 HandlerMapping、HandlerAdapter、参数解析器和返回值处理器的理解达到调用链层面，尚未进入 Spring MVC 内部源码和自定义扩展实现。
- CORS 已完成配置，但尚未通过独立浏览器前端应用系统验证预检、凭据和多环境来源管理。
- 当前分页、统一响应、日志和异常模型适合训练项目，尚未覆盖生产级幂等、安全、权限、审计、可观测性和接口版本治理。
- HikariCP 已完成基础连接池使用，但连接池容量、超时、故障恢复和并发压力没有经过生产级验证。
- 自动化 Web/数据库集成测试不是本阶段重点，尚未形成 MockMvc 与数据库测试体系。

综合评价：已具备 Spring MVC 的基础独立实践能力，能够手写并解释最小 REST API 及其主要请求链。能力定位仍是学习型单体 Web 服务的扎实基础，不等同于 Spring MVC 源码能力或生产级后端设计经验。

### 2.9 `04-mybatis` 代码能力评价

本 Module 的原生与 Spring 集成两段代码能够证明以下能力已经从“看过概念”进入“亲手实现并运行”：

- 能手动创建 `SqlSessionFactory` 和 `SqlSession`，获取 Mapper 代理并控制会话、提交、回滚和关闭。
- 能通过 `namespace + statement id` 说明 Mapper 方法怎样定位 XML SQL，并区分 XML SQL 与注解 SQL 的使用边界。
- 能使用 `#{}` 完成预编译参数绑定，并通过白名单控制必须使用 SQL 结构文本的排序场景。
- 能使用 `<where>`、`<set>`、`<foreach>`、`<choose>` 完成动态查询、动态更新、批量操作和排序分支。
- 能使用 JOIN、`<resultMap>`、父子 `<id>` 与 `<collection>` 将一对多结果行折叠为任务和评论集合，并解释 JOIN 结果行不能直接作为父对象分页依据。
- 能观察 `SqlSession` 一级缓存，并理解其会话作用域及更新操作对缓存的影响。
- 能通过 `SqlSessionFactoryBean`、`@MapperScan`、Mapper 动态代理和 `SqlSessionTemplate` 将 MyBatis 接入 Spring。
- 能让多个 Mapper 在同一个 `@Transactional` Service 方法中共享事务关联的 JDBC Connection，并用异常和 Navicat 结果证明同时回滚。

代码和复盘体现出的主要优点：

- 先隔离 Spring 学习 MyBatis 自身，再接入 Spring，对 `SqlSession` 生命周期和事务职责的理解没有被自动装配隐藏。
- 不只完成简单 CRUD，还实际实现了动态 SQL、排序安全、批量插入、生成主键、一对多映射和事务组合。
- 能从 Mapper 参数、XML 表达式、数据库列、Java 属性和 DTO 之间追踪数据映射问题。
- 能明确接受适合当前项目的 JOIN + `<collection>` 方案，同时了解 `column + select` 及 N+1 边界，没有为了覆盖 API 而增加无收益复杂度。

当前边界：

- 当前 SQL 与分页适合训练数据量，尚未通过执行计划、索引分析或压力数据验证性能。
- 尚未练习二级缓存、缓存一致性、自定义 `TypeHandler`、MyBatis 插件和复杂映射扩展。
- 事务练习证明了单数据源下多个 Mapper 的原子性，尚未覆盖复杂传播、多数据源、并发更新和隔离级别问题。
- 已通过手动请求和数据库结果验证主要链路，但尚未形成生产级数据库集成测试和可观测体系。

综合评价：已具备 MyBatis 的基础独立实践能力，能够手写并解释 Mapper 代理、SQL 映射、动态 SQL、结果映射、会话和 Spring 事务整合。能力定位仍是学习型单体项目的扎实基础，不等同于生产级 SQL 调优或数据访问架构经验。

### 2.10 `05-spring-boot` 代码能力评价

两轮 Spring Boot 练习能够证明以下能力已经从“看过概念”进入“亲手实现并运行”：

- 能从启动类、组件扫描和 Starter 依赖解释 Boot 如何创建应用上下文，而不是把 `@SpringBootApplication` 当作黑盒。
- 能通过自动配置导入流程、条件注解和运行日志，定位 Web、数据源、JdbcTemplate、事务、MyBatis 和 Actuator 基础设施的来源。
- 能使用 `application.yml`、环境变量和 Profile 管理配置，并理解配置覆盖优先级与敏感信息外置边界。
- 能使用 JdbcTemplate 与 MyBatis 两种数据访问方式完成任务 CRUD、条件分页、评论关联和事务验证，并将 Boot 自动配置与此前手动配置对应起来。
- 能打包可执行 Jar，使用 `java -jar` 独立启动，并用 Apifox、Navicat 和日志交叉确认 HTTP、数据库与事务结果。

当前边界：

- 尚未系统阅读复杂自动配置源码或编写自定义 Starter、条件装配和环境后处理器。
- 当前只验证单数据源和学习型连接池配置，尚未覆盖多数据源、连接池调优、生产密钥管理和配置中心。
- 尚未形成完整的 MockMvc、数据库集成测试、容器化部署和高并发验证体系。
- MyBatis 与事务仍以已有任务领域为主，复杂传播、并发更新、幂等和大数据量 SQL 性能尚未证明。

综合评价：已具备 Spring Boot 的基础独立实践能力，能够从配置、自动装配、请求链和数据访问角度解释一个最小 Boot 服务；能力定位仍是学习型单体项目基础，不等同于生产级 Boot 工程经验。

### 2.11 阶段能力定位

| 能力层级 | 当前判断 | 依据 |
| --- | --- | --- |
| Java 基础与项目动手 | 已具备 | 独立手写 `tank-game`、`chat-room`，并能持续修改和运行项目 |
| 原生 Java Web 基础 | 已具备入门到初级独立实践能力 | `01-java-web-basics` 完成 Servlet、Filter、Listener、Session、JDBC、事务和 WAR 部署 |
| Java Web 工程化 | 初步具备 | 已有 Maven、环境变量、日志、测试和部署意识，但抽象、并发和统一响应仍较原始 |
| Spring Core | 已具备基础独立实践能力 | `02-spring-core` 完成 XML/注解双轮 IoC、DI、生命周期、AOP、事务、后置处理器与自调用边界 |
| Spring MVC | 已具备基础独立实践能力 | `03-spring-mvc` 完成传统 WAR 请求链、REST CRUD、参数绑定、JSON、校验、统一异常、Interceptor、CORS 与 MySQL 验收 |
| MyBatis | 已具备基础独立实践能力 | `04-mybatis` 完成原生与 Spring 集成两段练习，覆盖 Mapper 代理、动态 SQL、一对多映射、一级缓存、手动事务和声明式事务 |
| Spring Boot | 已具备基础独立实践能力 | `05-spring-boot` 完成自动配置、Starter、Profile、JdbcTemplate、MyBatis、事务、可执行 Jar 和真实请求/数据库验收 |
| 生产级后端设计 | 尚未证明 | 尚未系统验证并发、连接池、统一错误模型、数据一致性、可观测性和安全边界 |

## 三、两个手写项目的客观评价

### 3.1 `tank-game`

值得肯定：

- 项目不是单一画图示例，包含移动、射击、碰撞、爆炸、复活、计分和位置存档。
- 能把输入、状态更新、绘制和线程任务串成可运行闭环。
- 使用类、继承和枚举组织游戏对象。
- 已经实际遇到共享状态、对象生命周期和资源加载问题。

主要问题：

- `Panel`、`Tank` 等类职责偏重，绘制、状态、碰撞、存档和游戏循环耦合。
- 使用 `Panel.width`、`Panel.height`、`Tank.tanks` 等静态全局状态。
- 多线程直接读写共享对象与集合，缺少明确线程安全模型。
- 正式源码中混入 TestNG 测试注解。
- 存档路径使用本地绝对路径，换机器或打包后不可移植。
- 线程中断被简单包装为运行时异常，缺少统一停止和资源释放机制。

评价结论：项目能够证明 Java 基础和综合实现能力，但同时暴露了并发设计、职责拆分、路径配置和测试隔离方面的短板。

### 3.2 `chat-room`

值得肯定：

- 功能完整度更接近后端业务，具备注册、登录、在线用户、私聊、群聊和文件传输。
- 已开始划分 client、server、DAO、domain、service 和 util。
- 使用连接池和 DAO 抽取数据库重复操作。
- 有完整的构建、打包、启动和使用说明。

主要问题：

- BIO 一连接一线程适合作为学习项目，不适合作为高并发架构模板。
- 密码明文存储，不符合真实系统安全要求。
- 网络协议、消息边界、断线和异常关闭处理仍较脆弱。
- DAO、业务逻辑和网络处理线程之间仍有较强耦合。
- 流、连接和数据库资源需要更系统地使用 `try-with-resources` 管理。
- 缺少单元测试、集成测试、并发测试和自动化验收。
- 依赖管理仍以手动 JAR 为主，后续应统一迁移到 Maven。

评价结论：项目能够证明你理解客户端请求、服务端处理、数据库访问和响应返回的基本链路，但不能证明已经掌握 Web API、事务一致性、认证授权和生产级异常处理。

## 四、当前尚未证明掌握的能力

下面这些内容不能因为看过课程、运行过 AI 生成项目或使用过相关注解，就判断为已掌握：

- Spring MVC 内部源码、自定义参数解析器/消息转换器、复杂内容协商和异步请求处理。
- Spring AOP 的复杂 Advisor 排序、循环依赖代理和声明式事务的复杂传播组合。
- MyBatis 二级缓存、自定义 `TypeHandler`、插件、复杂映射扩展和生产级 SQL 调优。
- Spring Boot 复杂自动配置源码、自定义 Starter、生产级配置和完整测试体系。
- 生产级 REST API 的幂等、安全、接口版本、复杂分页和错误模型治理。
- Web 层测试、数据库集成测试和并发测试。
- 复杂业务下的表结构、索引、幂等、并发和事务设计。

这些内容仍未被当前已完成的 Module 证明：

- 生产级线程安全、连接池调优、统一 JSON/错误模型治理和敏感信息日志控制。
- 复杂并发下的 Session、事务隔离、幂等和一致性设计。
- Spring Boot 环境下的复杂配置治理、模块化测试、容器化部署和高并发运行验证。

这些能力将通过 `JAVA_STUDY_PLAN.md` 中的 Module 逐项验证。

## 五、当前优势和主要风险

### 5.1 当前优势

- 动手能力强，愿意把知识做成可运行项目。
- 已经跨过 Java 基础、网络和数据库的第一道门槛。
- 有前端背景，理解浏览器、HTTP、接口和前后端联调会更快。
- 已有真实 Linux、Docker、MySQL 和服务部署经验，后续能把 Java 学习接到真实工程链路。
- 已经开始关注打包、配置、日志和交付，不只关注功能代码。

### 5.2 主要风险

- 容易把“代码能运行”当成“已经掌握原理和边界”。
- AI 可以快速生成完整项目，但会掩盖分层、事务、测试和错误处理是否真正理解。
- 过早进入微服务或复杂基础设施，可能跳过单体 Web 服务的核心训练。
- 如果每个阶段只新建示例、不测试异常路径，能力会停留在教程复现层面。
- 如果直接照抄 `linux-server`，会降低独立设计和问题定位的训练效果。

## 六、下一阶段判断标准

当前已完成 `05-spring-boot`。前五个训练 Module 已经证明能够：

- 解释 Servlet 容器、Spring Core、Spring MVC、Spring 事务和 MyBatis 在完整请求链中的职责边界。
- 使用 Controller、DTO、Service、Mapper 和 MySQL 完成学习型单体 REST API。
- 使用 Mapper 动态代理、XML/注解 SQL、动态 SQL、批量操作和一对多结果映射完成数据访问。
- 区分原生 `SqlSession` 的手动生命周期与 Spring 集成后的 `SqlSessionTemplate`、事务 Connection 管理。
- 通过真实 HTTP 请求、Navicat 数据结果和主动异常验证正常路径与事务回滚。
- 解释 Starter、条件自动配置、Profile、内嵌 Tomcat 和可执行 Jar，并在 Boot 中分别使用 JdbcTemplate 与 MyBatis 完成真实数据访问。

下一阶段 `06-spring-boot-comprehensive` 需要重点证明：

- 在完整业务中继续保持 Controller、Service、Mapper、DTO 和配置职责清晰，并完成需求拆分与数据建模。
- 设计更可靠的事务、幂等、并发和错误模型，验证索引、约束及数据一致性。
- 建立按风险分层的单元测试、Web 测试和数据库集成测试，补足当前手动验收边界。
- 完成外部配置、日志、健康检查、容器化或服务器部署和回滚说明。

`06-spring-boot-comprehensive` 应继续复用已经掌握的 Spring MVC、事务、MyBatis 与 Boot 自动装配机制；不能把“少写配置”误认为底层框架已经消失。

## 七、能力基线更新规则

本文件是“当前能力判断”，不是不可修改的最终结论。

只有满足以下条件，才更新某项能力为已掌握：

- 对应 Module 有独立完成的源码。
- 有构建、测试或真实请求验收证据。
- 能解释核心调用链和设计选择。
- 覆盖至少一个异常场景。
- 能在不复制旧代码的情况下完成需求变更。

更新时应修改：

1. 本文件的评估日期和能力结论。
2. `LEARNING_PROGRESS.md` 的阶段状态和验收证据。
3. 对应 Module 的 `README.md` 和复盘。

长期路线或总体能力结论发生变化时，再同步到知识库的 `Java学习总结与Spring训练规划.md`。
