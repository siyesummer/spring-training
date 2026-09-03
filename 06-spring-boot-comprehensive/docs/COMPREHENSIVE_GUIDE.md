# Spring Boot 综合项目实战引导

> 阶段状态：`已完成`（2026-09-03）。本文件记录第一版订单与库存综合练习的设计、实施顺序和验收标准；实际验收结果以 Module README、`LEARNING_PROGRESS.md` 和复盘文档为准。

## 一、先理解本阶段要练什么

前面几个 Module 主要是按技术拆开练习：Servlet、Spring 容器、MVC、MyBatis 和 Boot。现在反过来按业务组织技术。你需要亲手回答的核心问题是：

> 一个订单请求为什么需要多个表、多个 Mapper 和一个事务？如果中途失败，怎样保证数据库没有半成品？

前端背景可以帮助你理解 HTTP、JSON 和接口联调；本阶段要重点补足后端特有的内容：领域对象与 DTO 的边界、Service 业务规则、数据库约束、事务边界和并发更新风险。

## 二、第一版功能边界

### 2.1 业务对象

| 对象 | 作用 |
| --- | --- |
| Product | 商品基础信息，如名称、价格、启用状态 |
| Inventory | 商品库存，如可用数量和版本号 |
| Order | 订单主表，保存买家、金额和订单状态 |
| OrderItem | 订单明细，保存商品、数量和成交单价 |
| OperationLog | 记录创建订单、取消订单等关键操作 |

### 2.2 建议接口

先实现最小接口，路径可按你的命名习惯调整：

| 方法 | 路径 | 目的 |
| --- | --- | --- |
| `GET` | `/api/products` | 查询商品和库存 |
| `POST` | `/api/orders` | 创建订单并扣减库存 |
| `GET` | `/api/orders/{id}` | 查询订单及明细 |
| `POST` | `/api/orders/{id}/cancel` | 取消订单并恢复库存 |

请求示例：

```json
{
  "buyerId": 1,
  "items": [
    {"productId": 1, "quantity": 2}
  ]
}
```

第一版不需要购物车和支付状态。订单状态建议只有 `CREATED`、`CANCELLED` 两个值，先把状态流转和事务做清楚。

## 三、数据库设计与 Navicat 操作

数据库人工操作由你在 Navicat 完成。建议创建独立数据库，例如 `spring_training_comprehensive`，避免污染前几个 Module 的表。

在 Navicat 新建查询，逐段执行并观察结果。下面是设计参考，字段名可以调整，但调整后要同步 Java 对象、Mapper 和文档：

```sql
CREATE DATABASE IF NOT EXISTS spring_training_comprehensive
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE spring_training_comprehensive;

CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventories (
  product_id BIGINT PRIMARY KEY,
  available_quantity INT NOT NULL,
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT ck_inventory_quantity CHECK (available_quantity >= 0)
);

CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  buyer_id BIGINT NOT NULL,
  total_amount DECIMAL(12, 2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancelled_at DATETIME NULL,
  INDEX idx_orders_buyer_created (buyer_id, created_at)
);

CREATE TABLE order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10, 2) NOT NULL,
  CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uk_order_product UNIQUE (order_id, product_id),
  CONSTRAINT ck_item_quantity CHECK (quantity > 0)
);

CREATE TABLE operation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  detail VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_logs_order_created (order_id, created_at),
  CONSTRAINT fk_log_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

INSERT INTO products (name, price, enabled)
VALUES ('机械键盘', 299.00, 1), ('USB-C 扩展坞', 159.00, 1);

INSERT INTO inventories (product_id, available_quantity)
VALUES (1, 10), (2, 5);
```

注意：`CHECK` 约束是否实际生效取决于 MySQL 版本；业务层仍要校验数量，数据库约束是最后一道保护，不是替代 Service 规则。执行完成后在 Navicat 查询：

```sql
SELECT p.id, p.name, p.price, i.available_quantity, i.version
FROM products p JOIN inventories i ON i.product_id = p.id;
```

最后把确认过的 DDL 保存到 `src/main/resources/schema.sql`。文件是项目的可重复初始化依据，Navicat 中的操作不能成为唯一记录。

## 四、建议包结构与职责

```text
cn.siyes.training.comprehensive
├─ Application.java
├─ config/          # Web、MyBatis、配置绑定等显式配置
├─ controller/      # HTTP 路由、参数接收、响应状态
├─ dto/             # 请求和响应模型，不直接暴露数据库模型
├─ exception/       # 业务异常和统一异常处理
├─ model/           # Product、Inventory、Order 等持久化/领域对象
├─ mapper/          # MyBatis Mapper 接口和 XML
└─ service/         # 订单规则、库存规则和事务边界
```

Controller 不直接写 SQL，也不负责决定库存是否足够；Mapper 不负责抛出 HTTP 异常；Service 是本阶段最重要的边界，负责组合多个 Mapper 并定义事务。

Repository 是否需要保留由你决定：如果希望延续前面 `TaskRepository` 的抽象，可以让 Service 依赖 `OrderRepository`，由实现类调用 Mapper；如果当前重点是 Boot + MyBatis 事务，也可以让 Service 直接依赖 Mapper。两种方案都能工作，但要在复盘中说明取舍，不要为了“看起来分层”增加无实际价值的转发类。

## 五、按步骤实现

### 步骤 1：确认启动和配置

手动检查 `pom.xml`：

- Web MVC：提供内嵌 Tomcat、DispatcherServlet、JSON 转换和 Controller；
- Validation：提供 DTO 参数校验；
- MyBatis Starter：创建 `SqlSessionFactory`、Mapper 代理和相关自动配置；
- MySQL Driver：让运行时能够连接 MySQL；
- Actuator：提供健康检查；
- 对应 test Starter：只负责测试支持，不参与业务运行。

在 `application.yaml` 中配置数据库 URL、用户名和密码。密码继续通过 IDEA 运行配置或环境变量提供，不写入 Git。建议先只验证 `Application` 能启动，再观察启动日志中的数据源和 Mapper 信息。

可以先按下面的完整结构手动配置，再根据你的数据库名称调整 URL。`${...}` 表示从运行环境读取变量，不是要原样写入真实密码：

```yaml
spring:
  application:
    name: 06-spring-boot-comprehensive
  datasource:
    url: ${COMPREHENSIVE_DB_URL}
    username: ${COMPREHENSIVE_DB_USERNAME}
    password: ${COMPREHENSIVE_DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

server:
  port: 8087

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: cn.siyes.training.comprehensive.model
  configuration:
    map-underscore-to-camel-case: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

在 IDEA 的运行配置中设置以下三个环境变量，值使用你在 Navicat 中确认的连接信息：

```text
COMPREHENSIVE_DB_URL=jdbc:mysql://localhost:3306/spring_training_comprehensive?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
COMPREHENSIVE_DB_USERNAME=你的数据库账号
COMPREHENSIVE_DB_PASSWORD=你的数据库密码
```

这里要区分两条连接链：Navicat 用于创建和查看数据库；Java 进程通过这些环境变量创建自己的 DataSource。不要把 `username` 和 `password` 改成 Navicat 的保存名称，也不要把真实密码写进 `application.yaml` 或 Git。

### 步骤 2：先完成只读查询

先写 `ProductMapper` 和 `GET /api/products`：

1. Mapper XML 查询商品与库存；
2. 映射到明确的响应对象；
3. Controller 返回 `200`；
4. 使用 Navicat 确认返回值与表数据一致。

这一步先不引入订单事务，目的是确认配置、Mapper XML、数据库连接和 JSON 返回都没有问题。

### 步骤 3：设计创建订单的输入和规则

`CreateOrderRequest` 至少包含 `buyerId` 和 `items`。每个明细需要 `productId`、`quantity`。校验分两层：

- DTO 校验格式和明显边界，例如数量必须大于 0、明细不能为空；
- Service 校验业务事实，例如商品存在且启用、库存足够、同一订单不能重复出现同一商品。

不要从前端传入的 `totalAmount` 直接信任金额。Service 应根据数据库中的商品单价和数量重新计算总金额，并把计算后的单价写入 `order_items`。

### 步骤 4：实现订单事务

创建订单的 Service 方法是第一版核心：

```text
@Transactional
createOrder(request)
  -> 查询并校验商品、库存
  -> 扣减库存
  -> 插入 orders，取得 orderId
  -> 批量插入 order_items
  -> 插入 operation_logs
  -> 返回订单响应
```

每一步为什么都在同一个事务里：如果订单主表写成功、库存扣了，但明细写入失败，数据库就会出现无法解释的状态。事务让这些更新对外表现为“全部提交”或“全部撤销”。`commit()` 由 Spring 事务拦截器在方法正常结束后完成；运行时异常从 Service 冒出时执行回滚。不要在 Mapper 或 Service 中手动 `openSession()`、`commit()` 或 `rollback()`。

库存扣减 SQL 不要只写成先查询再无条件更新。第一版可以使用带条件的更新：

```sql
UPDATE inventories
SET available_quantity = available_quantity - #{quantity},
    version = version + 1
WHERE product_id = #{productId}
  AND available_quantity >= #{quantity}
  AND version = #{version}
```

通过返回更新条数判断是否成功。更新条数为 `0` 时说明库存不足或版本冲突，应抛出业务异常，让事务回滚。这里先理解乐观锁式条件更新的思想，不要求现在实现重试机制。

### 步骤 5：实现查询详情和取消订单

订单详情使用主表 + 明细 + 商品名称的关联查询，沿用前面 MyBatis 的 `resultMap` / `<collection>` 经验。取消订单需要：

1. 查询订单并确认存在；
2. 只允许 `CREATED` 状态取消；
3. 恢复每个明细的库存；
4. 修改订单状态为 `CANCELLED`；
5. 写入取消操作日志。

恢复库存和修改状态必须在同一事务中。重复取消应返回明确的业务错误，而不是再次增加库存。

### 步骤 6：统一异常和响应

至少区分：

- 请求格式/字段校验失败：`400`；
- 商品、订单不存在：`404`；
- 库存不足或状态不允许：当前项目按既定协议返回 HTTP `200`，由响应体 `code=400` 表示业务失败；如果项目改用标准 HTTP 语义，再统一调整为 `409`；
- 未预期数据库或系统错误：`500`。

使用 `@RestControllerAdvice` 统一转换异常。Service 抛出领域异常，Controller 不要到处 `try/catch` 和拼响应。响应结构保持稳定即可，第一版不用引入复杂错误码体系。

### 步骤 7：完成核心手动验收

使用 Apifox 和 Navicat 按以下最小顺序验证：

1. 查询商品，确认初始库存；
2. 创建一个数量为 `2` 的订单，确认返回订单 ID；
3. Navicat 检查订单、明细、日志新增，库存减少 `2`；
4. 查询订单详情，确认商品、数量、单价和总额；
5. 取消订单，确认库存恢复、订单状态变化和日志新增；
6. 重复取消，确认业务响应表示失败且库存不再变化；当前项目检查 HTTP `200` 和响应体 `code=400`；
7. 用库存不足的数量创建订单，确认业务响应表示失败，并确认订单、明细、日志均没有半成品记录；当前项目检查 HTTP `200` 和响应体 `code=400`；
8. 在写日志后临时制造一个运行时异常，确认所有更新回滚；恢复代码后再次创建，确认正常提交。

### 步骤 8：打包和运行

```text
mvn package
java -jar target/06-spring-boot-comprehensive-0.0.1-SNAPSHOT.jar
```

打包和启动是工程闭环的最后一步，不是本阶段主要学习成果。重点观察：Jar 是否包含 Mapper XML，外部数据库配置是否仍能被读取，独立启动后的接口是否和 IDEA 运行结果一致。

## 六、阶段检查点（已完成）

- [x] 数据库由 Navicat 创建，最终 DDL 已保存到 `schema.sql`。
- [x] 应用能启动，配置没有提交真实密码。
- [x] 商品库存查询成功。
- [x] 创建订单的成功路径完成，多个表结果一致。
- [x] 库存不足、商品不存在、重复取消至少各验证一次。
- [x] 事务异常能够回滚订单、库存、明细和日志。
- [x] 能解释条件更新、主键回写、批量插入和一对多查询。
- [x] 能解释为什么事务放在 Service，而不是 Controller 或 Mapper。
- [x] Jar 打包并独立启动后完成一次最小请求复测。

## 七、后续复盘问题

完成后用自己的话回答：

1. 为什么 `CreateOrderRequest` 不能直接作为数据库插入对象？
2. 为什么订单总额必须由后端根据数据库价格计算？
3. 为什么扣库存不能只依赖 Java 层的“先查询再更新”？
4. 如果日志插入失败，哪些表必须回滚，为什么？
5. `@Transactional` 由哪个代理边界触发？同类内部调用会有什么影响？
6. `operation_logs` 是业务审计记录还是调试日志？两者有什么区别？
7. 如果未来改成异步消息扣库存，当前事务边界会发生什么变化？
