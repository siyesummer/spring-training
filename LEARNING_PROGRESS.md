# Java 学习进度

> 当前能力基线和项目评价见 `CURRENT_LEVEL.md`；具体阶段要求见 `JAVA_STUDY_PLAN.md`。

## 当前总状态

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| Java 基础 | 已完成 | 已手写 `tank-game`、`chat-room`；当前能力与项目评价见 `CURRENT_LEVEL.md` |
| Java Web 基础 | 已完成 | 已完成 Tomcat、MySQL、JDBC、用户注册、登录/Session/登出、Filter、Listener、留言板、事务提交/回滚/恢复、WAR 构建和部署复测；核心技术复盘已完成 |
| Spring Core | 已完成 | `02-spring-core` 已完成 XML 与注解两轮，并通过 IoC/DI、生命周期、AOP、事务、后置处理器和自调用代理边界验收 |
| Spring MVC | 未开始 | 待创建 `03-spring-mvc` |
| MyBatis | 未开始 | 待创建 `04-mybatis` |
| Spring Boot | 未开始 | 待创建 `05-spring-boot` |
| Spring Boot 综合项目 | 未开始 | 待创建 `06-spring-boot-comprehensive` |

## 当前结论

- `linux-server` 主要由 AI 生成，当前只作为阅读、审查、修改和复盘目标。
- 尚未通过 `spring-training` 的独立 Module 证明 Spring Boot 能力。
- Java Web 基础 Module 已完成：注册、登录/Session/登出、Filter、Listener、留言板新增/查询、事务提交/回滚/恢复、Maven WAR 构建和实际 WAR 部署均已验证；代码复盘确认已具备独立完成原生 Java Web 小服务的能力。
- Spring Core Module 已完成：使用同一个账户转账主题分别完成纯 XML 与纯注解 / Java 配置，两轮放在不同包中，并通过运行结果对照 BeanDefinition 来源、依赖注入、生命周期、AOP、事务和后置处理器机制。

## Module 验收记录

### 01-java-web-basics 当前阶段记录

- 状态：已完成
- 已完成：`HealthServlet`、JSP 页面转发、`/api/register`、API 编码 Filter、MySQL JDBC 连接、BCrypt 密码哈希、用户名重复校验，登录、Session 创建、当前会话读取和登出主链路，请求日志与登录保护 Filter，留言板新增/查询基础接口，以及留言与操作日志的事务成功提交。
- 已验证证据：IDEA/Tomcat Network 面板确认注册相关 HTTP `400`/`201`/`409`/`500` 路径；Navicat 确认用户记录、`created_at` 和 BCrypt 哈希；临时错误端口确认数据库连接失败时返回 `500` 且不向前端暴露 SQL 细节。2026-08-18 使用 curl 确认不存在用户和错误密码均返回相同的 `401`，两种缺参均返回 `400`，登录成功返回 `200` 和 `JSESSIONID`，携带 Cookie 访问 `/api/me` 返回 `200`，登出返回 `204`，登出后再访问返回 `401`；随后通过浏览器和 Navicat 对照确认 Session 数据正确。Filter 验收中，未登录访问 `/api/messages/6` 返回 `401`，登录后访问同一路径返回 `404`，证明登录 Filter 分别完成拦截与放行；请求日志记录了最终状态和耗时。留言板验收中，空内容 `POST` 返回 `400`，正常 `POST` 返回 `201`，`GET /api/messages` 返回 `200` 并读取到数据库中的留言。
- 已验证事务证据：Navicat 确认 `messages.id=6` 与 `message_logs.message_id=6` 正确关联，日志动作为 `CREATE`，两条记录同时写入，证明生成主键传递和事务提交正确。随后将第二条 SQL 临时指向不存在的 `message_logs_error`，接口返回 `500`；两张表均未出现本次数据，证明第一条写入已回滚。恢复正确表名后接口重新返回 `201`，`messages.id=8` 与 `message_logs.message_id=8` 同步新增，证明故障恢复成功。
- 已验证构建证据：IDEA 和 Maven 均确认 `MessageValidatorTest` 的 4 个测试通过；临时修改边界值后测试按预期失败，恢复后再次通过；`mvn -pl 01-java-web-basics package` 成功生成 `target/01-java-web-basics.war`，`jar tf` 确认 WAR 包含 `WEB-INF/classes/`。
- 已验证部署证据：IDEA Tomcat 已部署 `target/01-java-web-basics.war`，实际 Context Path 为 `/01_java_web_basics_war`；部署后的 `/health` 返回 `200`，重新登录返回 `200` 和新 `JSESSIONID`，登录后 `/api/messages` 返回 `200`。
- Listener 验收证据：Tomcat 启动日志出现 `contextInitialized`，Session 创建日志出现数量 `1`、`2`，Session 销毁日志回到 `1`、`0`；Tomcat 停止日志出现 `contextDestroyed`，证明两个 Listener 已注册并收到对应生命周期回调。
- 结论：本 Module 的 Servlet、Filter、Listener、Session、JDBC、事务、WAR 构建和部署等核心内容已通过手动实践和真实运行证据，状态更新为“已完成”。JUnit、Maven 命令和异常记录作为辅助工程能力，不作为主要学习成果。

### 01-java-web-basics 代码复盘结论（2026-08-19）

- 能力结论：已达到“能够独立手写并解释原生 Java Web 基础项目”的阶段，具备进入 `02-spring-core` 的条件。
- 主要优点：分层方向基本正确；Servlet、Filter、Listener、Session、Service、DAO 和 MySQL 调用关系清晰；已经实际处理 BCrypt、参数化 SQL、唯一约束、事务提交和回滚。
- 主要边界：`ResultSet` 关闭不够一致；依赖仍通过手动 `new` 管理；响应和参数模型较原始；JSON 手动拼接不具备完整转义能力；Listener 计数不是并发安全实现；`web.xml` 命名空间仍需从旧 Java EE 描述符迁移到 Jakarta 6 格式。
- 能力判断：这些问题不影响本阶段学习目标和已完成结论，但说明当前水平是“扎实的原生 Web 入门和独立动手能力”，还不是生产级 Java 后端工程能力。

### 02-spring-core 完成记录（2026-08-20）

- 状态：已完成。
- Module 形式：普通 Maven `jar` Module，不使用 Tomcat、Spring MVC 或 Spring Boot。
- 练习结构：第一轮纯 XML，第二轮纯注解 / Java 配置；两轮使用相同的账户转账业务并放在独立包中，分别完成引导和复盘。
- 当前文档：`02-spring-core/README.md` 为两轮总览和最终证据；`docs/XML_GUIDE.md`、`docs/XML复盘.md` 记录 XML 轮；`docs/ANNOTATION_GUIDE.md`、`docs/ANNOTATION复盘.md` 记录注解轮。
- XML 容器与 AOP 证据：`ClassPathXmlApplicationContext` 完成 Bean 创建和依赖注入；`invocation.proceed()` 正常时目标方法执行并打印耗时，去掉后目标方法被截断；获取到 CGLIB 代理类；singleton/prototype 与销毁回调结果符合预期。
- XML 事务证据：Navicat 确认正常转账后账户余额为 `900.00`、`600.00` 并新增 `100.00` 日志；日志写入后主动抛出异常时，余额恢复为 `1000.00`、`500.00` 且日志回滚；删除模拟异常后恢复提交。
- 注解容器证据：`@Configuration`、`@ComponentScan`、`@Service`、`@Repository`、构造器注入和 `@Bean` 完成容器组装；`Environment` 正确读取 IDEA 环境变量；`@PostConstruct`、`@PreDestroy` 回调已观察。
- 注解 AOP 与事务证据：`@Aspect`、`@Around`、`@EnableAspectJAutoProxy` 输出方法耗时；`@Transactional` 正常提交转账，主动抛出受检 `TransferException` 后账户与日志一起回滚，删除异常后恢复成功提交。
- 后置处理器证据：`BeanDefinitionRegistryPostProcessor` 动态注册 BeanDefinition；`BeanFactoryPostProcessor` 在实例化前修改属性；`BeanPostProcessor` 在初始化前修改实例、初始化后返回 Wrapper；重复获取 singleton 得到相同最终对象。
- 自调用边界证据：`outer -> this.inner()` 中没有独立 `inner` AOP 日志且事务状态为 `false`；从代理直接调用 `inner()` 或由另一个注入的 Service 调用时，出现 `inner` AOP 日志且事务状态为 `true`。
- 构建证据：2026-08-20 执行 `mvn -s C:\Users\siyesummer\.m2\settings.xml clean verify`，4 个既有测试全部通过，Java Web WAR 与 Spring Core JAR 均重新生成，Reactor 中根项目、`01-java-web-basics` 和 `02-spring-core` 均为 `SUCCESS`。
- 能力结论：已具备 Spring Core 的基础独立实践能力，能解释 XML 与注解只是 BeanDefinition 来源和注册方式不同，容器、生命周期、后置处理器、代理和事务核心机制仍然相同。
- 当前边界：尚未通过训练项目证明 Spring MVC、Spring Boot 自动配置、复杂事务传播、连接池调优和生产级 Spring 工程设计；下一阶段进入 `03-spring-mvc`。

后续每个 Module 完成后，记录以下内容：

- 完成日期
- 实现范围
- 构建与测试命令
- 测试结果
- 主动制造并排查的故障
- 能够独立解释的核心原理
- 对照 `linux-server` 的对应代码
- 未掌握内容和下一步计划
