# Java 学习进度

> 当前能力基线和项目评价见 `CURRENT_LEVEL.md`；具体阶段要求见 `JAVA_STUDY_PLAN.md`。

## 当前总状态

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| Java 基础 | 已完成 | 已手写 `tank-game`、`chat-room`；当前能力与项目评价见 `CURRENT_LEVEL.md` |
| Java Web 基础 | 已完成 | 已完成 Tomcat、MySQL、JDBC、用户注册、登录/Session/登出、Filter、Listener、留言板、事务提交/回滚/恢复、WAR 构建和部署复测；核心技术复盘已完成 |
| Spring Core | 未开始 | 待创建 `02-spring-core` |
| Spring MVC | 未开始 | 待创建 `03-spring-mvc` |
| MyBatis | 未开始 | 待创建 `04-mybatis` |
| Spring Boot | 未开始 | 待创建 `05-spring-boot` |
| Spring Boot 综合项目 | 未开始 | 待创建 `06-spring-boot-comprehensive` |

## 当前结论

- `linux-server` 主要由 AI 生成，当前只作为阅读、审查、修改和复盘目标。
- 尚未通过 `spring-training` 的独立 Module 证明 Spring Boot 能力。
- Java Web 基础 Module 已完成：注册、登录/Session/登出、Filter、Listener、留言板新增/查询、事务提交/回滚/恢复、Maven WAR 构建和实际 WAR 部署均已验证；下一步进入 Spring Core。

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

后续每个 Module 完成后，记录以下内容：

- 完成日期
- 实现范围
- 构建与测试命令
- 测试结果
- 主动制造并排查的故障
- 能够独立解释的核心原理
- 对照 `linux-server` 的对应代码
- 未掌握内容和下一步计划
