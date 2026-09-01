# Module 05：Spring Boot 基础实战

> 当前状态：`已完成`（2026-09-01）
>
> 基线：Spring Boot `4.0.8`、Java `21`、Maven、Jar、内嵌 Tomcat

本 Module 用来理解 Spring Boot 如何整合已经学过的 Spring Core、Spring MVC、JdbcTemplate、事务和 MyBatis 能力。重点是自动配置、Starter、配置来源、内嵌 Tomcat、可执行 Jar 和运行期观察，不是只记启动类注解。

本 Module 已完成两轮练习。第一轮保留 `JdbcTemplate`，用于观察 Boot 对 Web、数据源、JdbcTemplate 和事务基础设施的自动配置；第二轮接入 MyBatis，用于理解 Starter、Mapper 扫描、XML SQL 和 Spring 事务如何被 Boot 组装。

## 练习顺序

1. 第一轮：Spring Boot + JdbcTemplate，观察 Boot 如何自动创建 Web、DataSource、JdbcTemplate 和事务基础设施。
2. 第二轮：Spring Boot + MyBatis，观察 MyBatis 自动配置、Mapper 代理、动态 SQL、关联映射和声明式事务。

## 文档入口

- [SPRING_BOOT_GUIDE.md](docs/SPRING_BOOT_GUIDE.md)：第一轮完整引导、依赖、配置、包结构、请求链、验收和复盘问题。
- [SPRING_BOOT_MYBATIS_GUIDE.md](docs/SPRING_BOOT_MYBATIS_GUIDE.md)：第二轮 MyBatis Starter、Mapper、XML SQL、事务和验收引导。
- [一阶段复盘.md](docs/一阶段复盘.md)：第一轮自动配置、Profile、Actuator、JdbcTemplate 和可执行 Jar 复盘。
- [二阶段复盘.md](docs/二阶段复盘.md)：第二轮 MyBatis 集成、动态 SQL、映射和事务复盘。
- [自动配置导入流程.md](docs/自动配置导入流程.md)：`AutoConfigurationImportSelector` 触发候选自动配置的流程说明。

## 当前项目基线

- Spring Boot：`4.0.8`
- Java：`21`
- 打包：`Jar`
- Web 容器：内嵌 Tomcat
- 启动入口：`cn.siyes.training.boot.Application`

启动类位于 `cn.siyes.training.boot` 根包，Controller、Service、Repository、DTO 和 Config 位于其子包，以便默认组件扫描能够覆盖业务组件。

## 已完成内容与边界

已完成：自动配置与 Starter、组件扫描、Profile 和环境变量、Actuator 健康检查、`JdbcTemplate` 持久化、MyBatis Starter、Mapper 扫描、XML/动态 SQL、批量操作、一对多映射、Spring 声明式事务、SQL 日志、可执行 Jar 打包，以及 `java -jar` 启动后的真实请求验证。验收使用 Apifox、Navicat 和本地日志完成。

当前边界：这是学习型单体项目的基础闭环，不等同于生产级 Boot 能力；多数据源、复杂自动配置源码、连接池和生产配置、集成测试体系、高并发与复杂事务传播仍待后续综合项目练习。数据库人工操作继续通过 Navicat，Java 连接参数通过 IDEA 运行配置或环境变量提供。

已知待改进：第二轮查询条件中的 `status` 仍存在空值边界（实现中直接调用 `status.name()` 可能产生 `NullPointerException`）；本次验收使用了明确状态值，未影响已验证链路，后续应通过显式判空或条件 SQL 处理可选参数。

下一阶段进入 `06-spring-boot-comprehensive`，把已掌握的 Boot、MVC、MyBatis、事务和配置能力放入一个更完整的业务项目中。
