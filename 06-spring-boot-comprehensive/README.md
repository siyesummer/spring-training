# Module 06：Spring Boot 综合项目

> 当前状态：`已完成`（2026-09-03）
>
> 基线：Spring Boot `4.0.8`、Java `21`、Maven、Jar、内嵌 Tomcat

本 Module 不再以“把更多接口写出来”为目标，而是把前面已经练习过的 Spring Core、Spring MVC、MyBatis、事务、配置和打包能力，放进一条较完整的业务链路中。推荐主题是订单与库存管理系统。

## 项目目标

围绕商品、库存和订单完成一个单体 REST API，重点理解：

- 需求如何拆成领域对象、数据库表和接口；
- Controller、DTO、Service、Mapper、异常处理和配置的职责边界；
- 创建订单时“校验库存 -> 扣减库存 -> 写入订单 -> 写入明细 -> 写操作日志”为什么必须处于同一事务；
- 取消订单时如何恢复库存，并限制非法状态流转；
- Spring Boot 如何承载此前已经掌握的 MVC、MyBatis 和事务机制。

第一版明确不做支付、购物车、JWT、Redis、消息队列、微服务和前端页面。先把一个可解释、可验证的单体业务闭环做扎实。

## 当前生成项目

- 启动类：`cn.siyes.training.comprehensive.Application`
- Maven 坐标：`cn.siyes.training:06-spring-boot-comprehensive`
- Java：`21`
- Spring Boot：`4.0.8`
- 打包：`Jar`
- 数据库：MySQL，人工操作默认使用 Navicat
- 已有依赖：Web MVC、Validation、Actuator、MyBatis Starter、MySQL Driver 及对应测试 Starter

当前 `pom.xml` 由 Spring Initializr 生成，后续只在确实需要时调整依赖。MyBatis Starter `4.0.1` 与 Spring Boot `4.0.8` 的版本关系，先以当前项目能够解析和启动为准，不要为了追求版本数字一致而随意替换。

## 文档入口

- [COMPREHENSIVE_GUIDE.md](docs/COMPREHENSIVE_GUIDE.md)：第一版完整实战引导、数据库模型、分层设计、事务边界和验收顺序。

## 练习顺序

1. 先确认启动类、依赖和配置能够启动；不要一开始就写业务代码。
2. 在 Navicat 创建独立数据库、表、索引和少量测试数据，并将最终 DDL 保存到 `src/main/resources/schema.sql`。
3. 先完成商品和库存的查询，再完成订单创建的最小成功路径。
4. 增加库存不足、商品不存在、重复取消和非法状态等业务边界。
5. 验证创建订单和取消订单的事务提交、回滚与恢复。
6. 最后补充日志、Actuator、必要测试、Jar 打包和独立启动复测。

## 完成标准

- 能说明一次创建订单请求从 HTTP 进入 Controller，到 Service、Mapper、MySQL 和事务提交的完整链路。
- 能解释订单、订单明细、库存和操作日志为什么这样建模，以及外键、唯一约束和索引解决什么问题。
- 能独立写出成功路径和至少一个失败路径，并证明失败时没有留下半成品数据。
- 能说明 `@Transactional` 的代理边界、事务连接复用和异常回滚条件。
- 能使用 `mvn package` 生成可执行 Jar，并用 `java -jar` 启动后完成最小接口复测。

## 阶段验收记录

- 完成日期：2026-09-03
- 核心功能：商品与库存查询、订单创建、订单详情、订单取消、重复取消、库存不足和异常恢复。
- 数据一致性：使用 Navicat 对照 `products`、`inventories`、`orders`、`order_items` 和 `operation_logs`，确认成功路径的多表写入、取消后的库存恢复，以及异常路径没有留下半成品数据。
- 事务验证：创建订单和取消订单均由 Service 层 `@Transactional` 统一包围；主动制造运行时异常后确认订单、明细、库存和操作日志一起回滚，恢复代码后再次提交成功。
- 接口验证：使用 Apifox 完成正常请求、库存不足、重复取消和异常恢复验证；当前项目业务异常采用 HTTP `200` + 响应体 `code` 表示业务失败，参数校验和系统异常仍按实现返回 HTTP `400` / `500`。
- 运行验证：`mvn package` 成功生成可执行 Jar，使用 `java -jar` 启动后完成接口和数据库复测。
- 学习结论：本阶段已完成从单项技术练习到完整业务链路的整合，能够解释 DTO、Controller、Service、Mapper、数据库约束、事务和审计日志之间的职责边界。
- 当前边界：幂等、高并发库存、复杂状态机、消息最终一致性、生产级测试和部署治理仍未系统验证，保留为后续提升方向。
