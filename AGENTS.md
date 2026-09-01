# Spring Training AGENTS

## 项目定位

`spring-training` 是 Java Web 与 Spring 学习的阶段性训练项目。项目根目录为：

```text
E:\本地项目\java-project\spring-training
```

项目采用一个 Maven 根项目加多个独立 Module 的方式组织。每个 Module 对应一个学习阶段，目标是通过可运行代码、技术复盘和核心手动验收证明阶段能力；测试、构建和文档用于辅助确认，而不是替代对技术机制的理解。

## 用户背景

- 用户主方向是前端开发，正在向全栈开发工程师转型。
- 已完成 Java 基础学习，并手写了 `tank-game` 和 `chat-room` 两个项目。
- 已完成 Java Web、Spring Core、Spring MVC、MyBatis 和 Spring Boot 基础阶段训练；MyBatis 已完成原生框架与 Spring 集成两段练习，下一阶段进入 Spring Boot 综合项目。
- 有一个真实 Spring Boot 项目 `E:\本地项目\java-project\linux-server`，但该项目主要由 AI 生成，不是用户独立手写完成的项目。它只能作为阅读、审查、修改和复盘目标，不能直接证明用户已经掌握 Spring Boot。
- 最终需要在 `spring-training` 中独立完成阶段 Module，并在此基础上看懂 `linux-server` 的结构、流程和实现细节，最后能够独立搭建新的 Spring Boot 项目。
- 后端、数据库、Linux、部署、Docker 和 Spring Boot 仍处于持续上手阶段，说明默认面向“正在转全栈的前端开发者”。
- 用户个人网站域名为 `siyes.cn`；新 Java 包名和 Maven 坐标优先按反向域名约定使用 `cn.siyes`，再结合项目名扩展，例如 `cn.siyes.training.web` 和 `cn.siyes.training`。旧模板中的 `org.example` 或 `com.siyesummer` 只能视为待替换占位名称。

## 规划基线

长期能力判断和路线总览维护在知识库：

```text
E:\github项目\frontend-knowledge\Java学习总结与Spring训练规划.md
```

具体 Module 设计、阶段验收标准和实战要求维护在本项目：

```text
E:\本地项目\java-project\spring-training\JAVA_STUDY_PLAN.md
```

实际完成进度、测试证据和阶段复盘维护在：

```text
E:\本地项目\java-project\spring-training\LEARNING_PROGRESS.md
```

当前 Java 能力基线和两个手写项目的客观评价维护在：

```text
E:\本地项目\java-project\spring-training\CURRENT_LEVEL.md
```

建议路线为：

```text
Java Web 基础
  -> Spring Core
  -> Spring MVC
  -> MyBatis
  -> Spring Boot
  -> Spring Boot 综合项目
```

Spring Boot 不是只在最后背注解，而是对 Spring、Spring MVC、数据访问、配置、测试和打包能力的工程化整合。学习 Spring MVC 和 MyBatis 时可以使用 Boot 提供启动能力，但必须能区分底层框架能力和 Boot 自动配置能力。

## Module 约定

建议 Module：

```text
01-java-web-basics
02-spring-core
03-spring-mvc
04-mybatis
05-spring-boot
06-spring-boot-comprehensive
```

每个 Module 至少包含：

- `README.md`：阶段目标、知识点、启动命令、接口示例和已知问题。
- 可运行源码。
- 关键技术的最小实现、设计说明和使用边界。
- 基本的手动验收记录；自动化测试和异常记录按本阶段风险与学习目标安排。

Module 完成标准：

- 不看教程能够重新写出最小版本。
- 能解释核心注解、接口、配置和调用链。
- 能修改需求，而不是只能复制原示例。
- 能处理至少一种有代表性的错误场景，重点是理解原因和修复方式，不追求堆积故障记录。
- 能理解必要测试的作用和边界；测试命令不是每个早期 Module 的主要验收目标。
- 适用时能说明本阶段知识在 `linux-server` 中的对应位置；原生 Java Web 早期 Module 不强制做对照。

## 协作与实战引导约定

用户是以前端开发为主、正在转型全栈的学习者，Java 基础已经完成第一轮学习并手写过 `tank-game` 和 `chat-room`，但 Java Web、Spring、数据库、Linux、部署和 Docker 仍处于持续上手阶段。后续提出学习建议、技术取舍、代码解释和问题排查时，必须结合这一角色与当前 Java 水平：利用已有的前端、HTTP、接口和工程协作经验建立类比，同时补足 Java 后端特有的语法、类型系统、对象生命周期、请求链路、数据库和部署基础；不要默认用户已经具备独立 Spring Boot 工程经验，也不要因为能够运行 AI 生成的代码就提高能力判断。

`spring-training` 是用户用于独立动手的训练项目。除非用户在当前请求中明确要求“帮我修改、配置、实现或执行”，否则不得代替用户修改项目中的任何源码、POM、依赖、配置文件、数据库、Tomcat、IDE 设置、测试或部署环境。即使修改看起来只是简单配置或明显优化，也必须先以说明、完整示例、检查清单或审查意见的形式提供，由用户手动完成，以加深理解和记忆。用户完成后，可以读取和检查结果、解释错误、提出改进建议并给出验收命令，但不能因为用户说“准备配置”“已经安装”或提出一个疑问，就推定获得了代为修改权限。文档维护也遵循该边界；只有用户明确要求更新文档，或既有约定明确要求把疑问、进度、验收证据同步到指定文档时，才可以修改相应文档。

用户使用 Navicat Premium 处理数据库相关的人工操作。后续创建数据库或用户、授权、执行 SQL、建表、查看数据、验证事务和制造数据库故障，默认都应提供 Navicat 操作路径或可在 Navicat 查询窗口执行的 SQL，由用户亲自完成；不要默认改用 MySQL 命令行或由代理直接操作数据库。Java 应用的 JDBC 参数仍通过 IDE 运行配置或环境变量管理，需要明确区分 Navicat 客户端连接与 Java 运行时连接。关键 DDL 和初始化 SQL 最终应由用户同步保存到项目的 `schema.sql`，不能只存在于 Navicat 的可视化配置中。

用户主要通过视频快速学习技术，因此每个 Module 进入实战演练时，默认需要先根据该阶段的验收要求提供一份详细引导，再开始具体编码。引导至少应覆盖：

- 实战目标、功能边界、分步实施顺序和阶段性检查点，并明确本阶段真正要掌握的技术细节。
- 需要引入的 Maven 依赖（包括 JAR 的用途、版本选择依据和依赖之间的关系）。
- 包结构、类职责、接口设计、对象生命周期、关键数据流，以及框架或容器在其中承担的职责。
- 实现过程中会用到的 Java、Servlet、Spring、SQL 或测试语法，并结合当前代码说明。
- 一条能证明核心机制工作的最小手动验证；异常、排查和构建命令作为辅助，不为了记录数量而增加低价值步骤。
- Servlet、Filter、Listener 等容器扩展点的职责、生命周期、注册方式和最小代码练习（适用于 Java Web Module）。
- 与当前 Module 验收标准的对应关系；只有进入适合的 Spring 阶段时才安排 `linux-server` 对照。

涉及手动配置时，必须提供完整、可复制或逐项核对的配置片段，而不能只在表格或说明文字中提到关键字段。需要同时检查配置所在层级（根 POM 或 Module POM）、依赖作用域、版本和实际构建命令，确保引导文档与示例配置一致。

指导过程中不能只机械回答用户当前问到的局部配置或语法。应先检查项目整体结构、父子模块关系和长期维护影响，主动发现更合理的方案，并说明为什么。例如，多个 Module 共用的 Maven 编译版本、编码格式和依赖版本应优先考虑放在根 `pom.xml` 或 `dependencyManagement` 中；某个 Module 独有的依赖和实现才放在 Module 自己的 POM 中。如果当前建议只是临时方案，必须明确指出更好的长期方案、当前不立即采用的原因以及后续迁移方式。对于初学者容易忽略但会影响工程质量的设计问题，应主动提醒，而不是等待用户指出。

引导应优先帮助用户理解和独立完成代码，而不是直接交付全部实现。用户可以随时指出验收要求、实战主题或实现方案与其预期不一致；收到反馈后，应先说明差异和影响，再按用户意图调整计划、验收标准或实现设计。除非用户明确要求，否则不因为提供了引导或示例就直接将阶段标记为“已完成”。

### 学习重点优先级

用户通过手动编写代码训练，核心目标是理解每项技术或框架的基本设计、适用边界、对象生命周期和实际使用方式，并具备写出最小可运行版本和阅读更高难度项目的能力。后续引导按以下优先级组织：

1. 技术机制和设计取舍：例如容器如何创建对象、何时回调、请求如何进入 Servlet、Filter 如何放行或拦截、Listener 监听什么生命周期事件。
2. 独立动手能力：由用户手写关键类、配置和调用代码，助手提供接口解释、语法提示、设计检查和分阶段验收。
3. 一条核心功能的手动验证：优先确认技术确实生效，并解释现象背后的原因。
4. 自动化测试、完整异常矩阵、构建命令和详细排查记录：作为辅助工程能力，按阶段需要安排，不喧宾夺主。实际开发中测试代码可以借助 AI 编写，但用户仍需知道测试对象、边界和结果含义。

事务说明统一使用准确表述：在 JDBC 默认 `autoCommit=true` 时，每条成功执行的更新语句会自动提交；调用 `setAutoCommit(false)` 后，后续更新属于当前事务，`commit()` 用于确认并持久化当前事务中尚未提交的修改，`rollback()` 用于撤销当前事务中尚未提交的修改。`commit()` 不是让 SQL “生效”的唯一时刻，SQL 可能已经执行并影响当前事务可见的数据；它决定这些修改是否最终提交。

## 进度记录规则

后续每完成一个阶段，在本项目内更新：

1. Module 的 `README.md`。
2. 根目录 `LEARNING_PROGRESS.md` 的状态、日期、验收证据和问题。
3. 有充分验收证据时更新 `CURRENT_LEVEL.md` 的评估日期和能力结论。
4. 必要时更新根知识库的 `Java学习总结与Spring训练规划.md`，只更新长期有效的能力结论或路线调整，不记录 Module 细节和流水账。

新增或调整 Module 时，先更新本项目的 `JAVA_STUDY_PLAN.md`，再创建或修改对应 Module 目录。

状态只能使用：

- `未开始`
- `进行中`
- `待补强`
- `已完成`

“已完成”必须有构建、测试或真实请求验收中的至少一种客观证据，并且能用自己的话解释核心实现。只看完课程、代码能启动或单次手工请求成功，仍不足以单独标记为已完成。

当前能力判断以 `CURRENT_LEVEL.md` 为准。不能因为 AI 生成的代码可以运行，或课程已经看完，就自动提高能力评级。

## 训练原则

- 优先理解请求链路、对象生命周期、事务边界和数据流，再记注解。
- 先做最小可运行版本，再按学习目标增加异常处理、测试、配置和工程化能力。
- 每个阶段优先验证一个最能说明机制的错误或边界；不要求为了形式固定制造大量故障或记录全部排查过程。
- 不把生产密码、Token、真实服务器地址或数据库凭据写入仓库。
- 不把 AI 生成的 `linux-server` 代码当成自己的已掌握能力；先尝试独立设计，再对照真实项目审查和复盘。
- 不因为使用 Spring Boot 就跳过 Servlet、Spring MVC、JDBC、事务和 MyBatis 的基本原理。
- 不把 `target`、`.idea`、日志、临时数据库和本地凭据作为学习成果提交。

## 对照项目

重点对照：

```text
E:\本地项目\java-project\linux-server
```

学习过程中需要逐步看懂：

- Spring Boot 启动类和组件扫描。
- Controller、DTO、Service、配置类和异常处理的分工。
- `application.yml` 与 `@ConfigurationProperties` 的绑定。
- `JdbcTemplate`、SQL、事务和数据库初始化。
- CORS、参数校验、统一响应和测试。
- Maven 打包、可执行 JAR、日志和部署入口。

## 当前状态

- Java 基础：已完成第一轮学习和两个项目实践。
- Java Web + Spring：`01-java-web-basics`、`02-spring-core`、`03-spring-mvc`、`04-mybatis` 和 `05-spring-boot` 已完成，下一阶段为 `06-spring-boot-comprehensive`。
- `spring-training`：已完成原生 Servlet、Filter、Listener、Session、JDBC、事务、WAR 构建和部署；Spring Core 的 XML/注解双轮 IoC、DI、生命周期、AOP、事务、后置处理器和自调用代理边界；传统 WAR + Tomcat 下的 Spring MVC 请求链、任务 REST API、参数绑定、JSON、校验、统一异常、Interceptor、CORS 和 MySQL 持久化；原生 MyBatis 与 Spring 集成两段练习，包括 Mapper 代理、XML/注解 SQL、动态 SQL、分页、排序白名单、批量操作、一对多结果映射、一级缓存、手动事务、`SqlSessionTemplate` 和 Spring 声明式事务；以及 Spring Boot JdbcTemplate/MyBatis 两轮练习，包括 Starter、自动配置、Profile、Actuator、可执行 Jar 和 `java -jar` 验收。
- `linux-server`：主要由 AI 生成，当前定位为对照阅读和代码审查对象。
- 最终验收：能够独立搭建一个有数据库、REST API、校验、测试、配置和部署说明的 Spring Boot 项目。
