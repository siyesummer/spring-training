# Module 02：纯 XML 方式实战引导

> 当前轮次：第一轮，纯 XML
> 目标：不依赖组件扫描和业务注解，手动配置 Spring IoC、DI、Bean 生命周期、AOP 和事务。

## 1. 本轮规则

本轮所有 Bean 都由 XML 显式注册。业务类中不要出现以下注解：

```java
@Component
@Service
@Repository
@Configuration
@Bean
@Autowired
@Aspect
@Transactional
```

也不要使用：

- `AnnotationConfigApplicationContext`。
- `@ComponentScan`。
- Spring Boot 启动类。
- `spring-context` 的注解扫描命名空间。

这样做不是实际项目的唯一推荐方式，而是为了先把“容器如何根据配置创建对象”看清楚。XML 练习完成后，第二轮再用注解实现同样功能。

## 2. 手动配置依赖

你需要手动编辑 `02-spring-core/pom.xml`。根 POM 已统一 Java 21、编码和 JUnit 版本；Spring 依赖版本建议统一写成 Module 的属性，避免 XML 轮和注解轮出现版本漂移。

在 `<properties>` 中添加：

```xml
<spring.version>6.2.8</spring.version>
<aspectj.version>1.9.24</aspectj.version>
```

在 `<dependencies>` 中添加完整依赖：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>${spring.version}</version>
</dependency>

<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
    <version>${spring.version}</version>
</dependency>

<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>${spring.version}</version>
</dependency>

<!-- JdbcTemplate、DataSourceTransactionManager 和 JDBC 异常转换 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>${spring.version}</version>
</dependency>

<!-- 版本由根 POM 的 dependencyManagement 统一管理 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- XML AOP 运行时织入和 AspectJExpressionPointcut 需要 -->
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>${aspectj.version}</version>
</dependency>

<!-- 第二阶段的可重复验证使用；不是业务运行依赖 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

当前不加入 Spring Boot、Spring MVC、MyBatis 和 JPA。IoC、DI、生命周期和 AOP 使用普通内存对象观察；进入事务练习后直接接入 MySQL，使用真实的 JDBC 事务验证提交和回滚。

依赖用途：

| 依赖 | 作用 |
| --- | --- |
| `spring-context` | ApplicationContext、BeanFactory、XML Bean 定义加载 |
| `spring-aop` | Spring AOP 基础接口和代理支持 |
| `spring-tx` | PlatformTransactionManager、事务定义和事务拦截器 |
| `spring-jdbc` | JdbcTemplate、JDBC 事务管理器以及 JDBC 异常转换 |
| `mysql-connector-j` | Java 连接 MySQL 所需的 JDBC 驱动 |
| `aspectjweaver` | AspectJ 表达式切点和运行时代理支持 |
| `junit-jupiter` | 可选的最小规则测试 |

## 3. 第一阶段：创建最小容器

### 3.1 先写普通 Java 类

先不要写 Spring 注解。创建：

```text
src/main/java/cn/siyes/training/spring/xml/model/Account.java
src/main/java/cn/siyes/training/spring/xml/repository/AccountRepository.java
src/main/java/cn/siyes/training/spring/xml/service/AccountService.java
src/main/java/cn/siyes/training/spring/xml/XmlApplication.java
```

职责先保持最小：

- `Account`：账号编号和余额。
- `AccountRepository`：保存和查询账号，先使用内存 Map。
- `AccountService`：编排扣款业务，依赖 `AccountRepository`。
- `XmlApplication`：加载 XML 容器并调用 Service。

先用普通构造器表达依赖，不使用无参构造器隐藏依赖：

```java
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
}
```

这里先理解 Java 设计，再理解 XML 如何调用这个构造器。Spring 不是凭空产生依赖，它只是按照配置替你创建对象并传入依赖。

### 3.2 写 XML Bean 定义

创建：

```text
src/main/resources/spring/xml/applicationContext.xml
```

先配置最小版本：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="accountRepository"
          class="cn.siyes.training.spring.xml.repository.AccountRepository"/>

    <bean id="accountService"
          class="cn.siyes.training.spring.xml.service.AccountService">
        <constructor-arg ref="accountRepository"/>
    </bean>
</beans>
```

重点观察：

- `id` 是容器中的 Bean 名称，不是 Java 类名。
- `class` 是 Spring 反射创建实例的全限定类名。
- `ref` 表示引用另一个 Bean；`value` 表示传入字面量。
- `<constructor-arg>` 对应构造器参数，顺序和类型必须匹配。

### 3.3 启动容器

在 `XmlApplication` 中手动加载：

```java
try (ClassPathXmlApplicationContext context =
         new ClassPathXmlApplicationContext("spring/xml/applicationContext.xml")) {
    AccountService service = context.getBean("accountService", AccountService.class);
    service.withdraw(1L, 10L);
}
```

这里的关键不是调用业务，而是理解：

1. `ClassPathXmlApplicationContext` 读取 classpath 下的 XML。
2. Spring 根据 `<bean>` 创建 Repository。
3. Spring 创建 Service 时，把 Repository 传入构造器。
4. `getBean` 取得容器管理的对象，而不是你自己 `new AccountService(...)`。
5. `try` 结束时 Context 关闭，容器执行销毁流程。

先完成这一步，再进入生命周期，不要一次性添加 AOP 和事务。

## 4. 第二阶段：构造器注入和 Setter 注入

### 4.1 构造器注入

保持 `AccountService` 的依赖为 `final`，使用：

```xml
<constructor-arg ref="accountRepository"/>
```

理解它的特点：对象创建时依赖必须存在，对象创建完成后依赖不可替换，适合核心必需依赖。

### 4.2 Setter 注入

新增一个可选的 `AuditLogRepository`，为它提供 setter：

```java
public void setAuditLogRepository(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
}
```

XML：

```xml
<property name="auditLogRepository" ref="auditLogRepository"/>
```

对比构造器注入和 Setter 注入，不要只记 XML 标签：

| 方式 | 对象创建时 | 适合场景 |
| --- | --- | --- |
| 构造器注入 | 依赖必须传入 | 核心依赖、不可变字段 |
| Setter 注入 | 创建后再设置 | 可选依赖、需要后置调整的属性 |

当前练习的建议结论是：核心依赖优先构造器注入，Setter 不是为了省略依赖，而是表达“可选或可变属性”。

## 5. 第三阶段：Bean 生命周期

创建一个 `LifecycleProbe`，在初始化和销毁时打印日志：

```java
public class LifecycleProbe {
    public void init() {
        System.out.println("LifecycleProbe init");
    }

    public void destroy() {
        System.out.println("LifecycleProbe destroy");
    }
}
```

XML：

```xml
<bean id="lifecycleProbe"
      class="cn.siyes.training.spring.xml.lifecycle.LifecycleProbe"
      init-method="init"
      destroy-method="destroy"/>
```

然后观察：

```text
创建 ApplicationContext
  -> Bean 实例化
  -> 依赖注入
  -> init-method
  -> getBean / 业务调用
关闭 ApplicationContext
  -> destroy-method
```

再增加一个 `BeanNameAware` 或 `InitializingBean` 只是为了认识 Spring 生命周期接口，代码完成后删掉也可以。核心要理解：生命周期回调由容器触发，业务代码不应该主动调用这些方法。

同时验证单例和原型作用域：

```xml
<bean id="singletonProbe"
      class="cn.siyes.training.spring.xml.lifecycle.LifecycleProbe"
      scope="singleton"/>

<bean id="prototypeProbe"
      class="cn.siyes.training.spring.xml.lifecycle.LifecycleProbe"
      scope="prototype"/>
```

用两次 `getBean` 比较对象引用：

```java
context.getBean("singletonProbe") == context.getBean("singletonProbe")
context.getBean("prototypeProbe") == context.getBean("prototypeProbe")
```

预期前者为 `true`，后者为 `false`。注意：原型 Bean 的销毁回调默认不由容器完整托管，这是生命周期边界的一部分。

## 6. 第四阶段：纯 XML AOP

### 6.1 先写普通切面类

这一轮不使用 `@Aspect`。创建 `TimingAspect`，使用 Spring AOP 的 `MethodInterceptor`：

```java
public class TimingInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long cost = (System.nanoTime() - start) / 1_000_000;
            System.out.println(invocation.getMethod().getName() + " cost=" + cost + "ms");
        }
    }
}
```

`invocation.proceed()` 相当于 Filter 中的 `chain.doFilter`：调用它才会继续执行目标方法；不调用就会在代理处截断。

### 6.2 用 XML 创建代理

在 XML 根节点加入 AOP 命名空间：

```xml
xmlns:aop="http://www.springframework.org/schema/aop"
```

并在 `xsi:schemaLocation` 增加：

```xml
http://www.springframework.org/schema/aop
https://www.springframework.org/schema/aop/spring-aop.xsd
```

然后添加：

```xml
<bean id="timingInterceptor"
      class="cn.siyes.training.spring.xml.aspect.TimingInterceptor"/>

<bean id="accountServiceTarget"
      class="cn.siyes.training.spring.xml.service.AccountService">
    <constructor-arg ref="accountRepository"/>
</bean>

<bean id="accountService"
      class="org.springframework.aop.framework.ProxyFactoryBean">
    <property name="target" ref="accountServiceTarget"/>
    <property name="interceptorNames">
        <list>
            <value>timingInterceptor</value>
        </list>
    </property>
</bean>
```

这里故意使用 `ProxyFactoryBean`，让你直接看到“目标对象”和“代理对象”是两个 Bean。以后再用 `<aop:config>` 的自动代理方式，理解 Spring 如何根据切点自动创建代理。

### 6.3 AOP 验收

1. 通过 `getBean("accountService")` 调用扣款。
2. 观察 `TimingInterceptor` 是否打印方法耗时。
3. 临时注释 `invocation.proceed()`，确认目标方法不再执行。
4. 打印 `context.getBean("accountService").getClass()`，观察拿到的是代理类而不是原始 Service 类。
5. 解释为什么直接 `new AccountService(...)` 不会自动经过 Spring AOP。

### 6.4 本次验收记录（2026-08-19）

- `invocation.proceed()` 正常调用时，控制台出现 `transfer执行` 和 `transfer cost0ms`，证明拦截器继续执行了目标方法，并在调用后完成耗时记录。
- 临时注释 `invocation.proceed()` 后，目标方法不再执行，但拦截器的后置日志仍然输出，证明拦截器可以在代理处截断调用链。
- `context.getBean("accountService").getClass()` 输出类似 `AccountService$$SpringCGLIB$$1`，证明容器返回的是 CGLIB 代理对象，而不是原始 `AccountService` 实例。
- 两次获取 singleton Bean 的比较结果为 `true`，两次获取 prototype Bean 的比较结果为 `false`，证明两种作用域的实例创建策略不同。
- 关闭 `ClassPathXmlApplicationContext` 时输出销毁日志，证明 `destroy-method` 在容器关闭阶段被回调。
- 原理复盘：XML 会被解析为 `BeanDefinition` 元数据；Spring 根据元数据创建对象、注入依赖并执行生命周期回调；AOP 再根据配置为目标对象创建代理。直接 `new AccountService(...)` 只得到普通 Java 对象，不会自动经过 Spring 的依赖注入、生命周期和 AOP 代理处理。

6.3 验收完成，下一步进入第 7 节的 XML + JDBC 真实转账事务练习。

## 7. 第五阶段：XML + JDBC 真实转账事务

前四个阶段已经分别观察了容器、依赖注入、生命周期和 AOP。这一阶段把它们串成一条真实链路：

```text
XmlApplication
  -> 从 Spring 容器取得 accountService 代理对象
  -> TimingInterceptor
  -> TransactionInterceptor 开启事务
  -> AccountService.transfer(...)
      -> AccountRepository 扣减付款账户
      -> AccountRepository 增加收款账户
      -> AuditLogRepository 写入转账日志
  -> 三步成功：commit
  -> 任一步抛出异常：rollback
```

这次直接使用 MySQL，不再实现训练用的内存事务管理器。原因是内存 `Map` 本身不受 JDBC 事务管理器控制，自己模拟 `begin/commit/rollback` 容易把重点变成编写事务框架，而不是理解 Spring 如何管理真实数据库事务。

### 7.1 本阶段要掌握的边界

- `AccountService` 决定一个完整业务包含哪些数据库操作，因此事务边界放在 Service 方法上。
- Repository 只负责执行一条具体 SQL，不调用 `commit()` 或 `rollback()`。
- `DataSourceTransactionManager` 负责取得连接、关闭自动提交、提交和回滚。
- `JdbcTemplate` 负责执行 SQL，并自动参与当前线程中已经开启的 Spring 事务。
- `<tx:advice>` 负责描述哪些方法需要事务以及出现什么异常时回滚。
- `<aop:advisor>` 把事务规则应用到目标 Service 方法，最终得到代理对象。

特别注意：本阶段不要在 Repository 中继续调用 `DriverManager.getConnection()`。那样得到的可能是另一条连接，不是 Spring 事务管理器绑定的连接，最终会出现“事务回滚了，但 DAO 的 SQL 已经提交”的问题。`JdbcTemplate` 内部会通过 Spring 的连接管理机制复用当前事务连接。

### 7.2 手动补充 Maven 依赖

在 `02-spring-core/pom.xml` 的 `<dependencies>` 中补充：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>${spring.version}</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

MySQL 驱动没有在 Module 中重复填写版本，因为根 POM 的 `dependencyManagement` 已经统一管理 `${mysql.connector.version}`。`dependencyManagement` 只管理版本，不会自动把依赖加入 Module，所以这里仍然要声明 `<dependency>`。

完成后在 IDEA 的 Maven 工具窗口重新加载项目。此时只验证依赖能够解析，不要求数据库已经连接成功。

### 7.3 使用 Navicat 创建数据库

在 Navicat Premium 中使用你现有的本地 MySQL 连接：

1. 打开连接。
2. 右键连接，选择“新建查询”。
3. 执行下面的完整 SQL。
4. 刷新数据库列表，确认出现 `spring_training_core`。
5. 把同一份 SQL 手动保存到项目的 `src/main/resources/db/schema.sql`，确保表结构不只存在于 Navicat 中。

```sql
CREATE DATABASE IF NOT EXISTS spring_training_core
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE spring_training_core;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_name VARCHAR(50) NOT NULL,
    balance DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS transfer_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transfer_logs_amount CHECK (amount > 0),
    CONSTRAINT fk_transfer_logs_from_account
        FOREIGN KEY (from_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfer_logs_to_account
        FOREIGN KEY (to_account_id) REFERENCES accounts (id)
) ENGINE = InnoDB;

INSERT INTO accounts (id, owner_name, balance)
VALUES
    (1, 'Alice', 1000.00),
    (2, 'Bob', 500.00);
```

如果重复执行初始化 SQL 时提示主键冲突，说明测试数据已经存在，不要继续重复插入。需要恢复初始余额时，在 Navicat 中手动执行：

```sql
USE spring_training_core;

DELETE FROM transfer_logs;
UPDATE accounts SET balance = 1000.00 WHERE id = 1;
UPDATE accounts SET balance = 500.00 WHERE id = 2;
```

这里使用 `DECIMAL(12, 2)` 和 Java 的 `BigDecimal` 表示金额，不使用 `double`。`double` 是二进制浮点数，可能产生金额精度误差。

### 7.4 配置 Java 运行时数据库参数

继续沿用环境变量，不把数据库密码写进 Git。打开 IDEA 的：

```text
Run -> Edit Configurations -> XmlApplication -> Environment variables
```

配置：

```text
DB_URL=jdbc:mysql://localhost:3306/spring_training_core?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=你的本地 MySQL 用户名
DB_PASSWORD=你的本地 MySQL 密码
```

URL 中的等号直接写成 `=`，不要写成 `\=`。环境变量属于运行配置，所以 `mvn compile` 不需要连接数据库，但运行 `XmlApplication` 时必须能读取这些变量。

在 `applicationContext.xml` 中加入 `context` 命名空间：

```xml
xmlns:context="http://www.springframework.org/schema/context"
```

并在 `xsi:schemaLocation` 中加入：

```xml
http://www.springframework.org/schema/context
https://www.springframework.org/schema/context/spring-context.xsd
```

然后配置环境变量解析器和数据源：

```xml
<context:property-placeholder/>

<bean id="dataSource"
      class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="${DB_URL}"/>
    <property name="username" value="${DB_USERNAME}"/>
    <property name="password" value="${DB_PASSWORD}"/>
</bean>

<bean id="jdbcTemplate"
      class="org.springframework.jdbc.core.JdbcTemplate">
    <constructor-arg ref="dataSource"/>
</bean>
```

`DriverManagerDataSource` 没有连接池，适合当前最小训练。生产项目通常使用 HikariCP 等连接池；此处不立即加入，是为了先看清 `DataSource -> TransactionManager -> JdbcTemplate` 的关系。

### 7.5 设计 Repository 方法

把两个 Repository 都改为通过构造器接收同一个 `JdbcTemplate`：

```java
public AccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
}
```

```java
public AuditLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
}
```

对应 XML：

```xml
<bean id="accountRepository"
      class="cn.siyes.training.spring.xml.repository.AccountRepository">
    <constructor-arg ref="jdbcTemplate"/>
</bean>

<bean id="auditLogRepository"
      class="cn.siyes.training.spring.xml.repository.AuditLogRepository">
    <constructor-arg ref="jdbcTemplate"/>
</bean>
```

`AccountRepository` 先实现三个方法：

```java
public int debit(long accountId, BigDecimal amount)
public int credit(long accountId, BigDecimal amount)
public BigDecimal findBalance(long accountId)
```

扣款 SQL：

```sql
UPDATE accounts
SET balance = balance - ?
WHERE id = ? AND balance >= ?
```

对应的 `JdbcTemplate` 语法：

```java
return jdbcTemplate.update(sql, amount, accountId, amount);
```

这个 SQL 同时完成“扣款”和“余额是否足够”的判断。`update(...)` 的返回值是受影响行数：

- 返回 `1`：账户存在且余额足够，扣款成功。
- 返回 `0`：账户不存在或余额不足，Service 必须抛出异常。

入账 SQL：

```sql
UPDATE accounts
SET balance = balance + ?
WHERE id = ?
```

入账也要检查受影响行数是否为 `1`，否则说明收款账户不存在。

查询余额可以使用：

```java
return jdbcTemplate.queryForObject(
        "SELECT balance FROM accounts WHERE id = ?",
        BigDecimal.class,
        accountId
);
```

`AuditLogRepository` 实现：

```java
public int insert(long fromAccountId, long toAccountId, BigDecimal amount)
```

SQL：

```sql
INSERT INTO transfer_logs (from_account_id, to_account_id, amount)
VALUES (?, ?, ?)
```

这里仍然使用 `?` 参数绑定，不拼接 SQL 字符串。`JdbcTemplate` 会负责创建和关闭 `PreparedStatement`，同时把底层 `SQLException` 转换成 Spring 的运行时 `DataAccessException`。

### 7.6 设计 Service 转账流程

事务阶段中，`AuditLogRepository` 已经是转账必需依赖，不再是可选依赖。因此把前面为练习 Setter 注入而编写的 setter 改成构造器注入：

```java
public AccountService(AccountRepository accountRepository,
                      AuditLogRepository auditLogRepository) {
    this.accountRepository = accountRepository;
    this.auditLogRepository = auditLogRepository;
}
```

Setter 注入练习已经达到目的；真实业务设计中，缺少任意一个 Repository 都无法完成转账，构造器注入更准确。

将 `transfer()` 调整为：

```java
public void transfer(long fromAccountId,
                     long toAccountId,
                     BigDecimal amount)
```

按以下顺序由你手写方法体：

1. 判断付款账户和收款账户不能相同。
2. 使用 `amount.compareTo(BigDecimal.ZERO) <= 0` 判断金额必须大于零。
3. 调用 `accountRepository.debit(...)`，返回值不是 `1` 时抛出运行时异常。
4. 调用 `accountRepository.credit(...)`，返回值不是 `1` 时抛出运行时异常。
5. 调用 `auditLogRepository.insert(...)`，返回值不是 `1` 时抛出运行时异常。
6. 方法正常结束时，不要在 Service 中手写 `commit()`。

可以创建一个最小业务异常：

```java
public class TransferException extends RuntimeException {
    public TransferException(String message) {
        super(message);
    }
}
```

建议放在：

```text
src/main/java/cn/siyes/training/spring/xml/exception/TransferException.java
```

使用运行时异常是有意的：Spring 默认对 `RuntimeException` 和 `Error` 回滚。XML 中也会显式写出 `rollback-for`，帮助你观察回滚规则，而不是依赖记忆中的默认值。

### 7.7 把 AOP 配置升级为统一自动代理

你在前一阶段使用 `ProxyFactoryBean`，已经观察了“目标对象”和“代理对象”。进入事务阶段后，不要同时保留下面两套代理：

```text
ProxyFactoryBean 手动代理
+
<aop:config> 自动代理
```

否则容易形成嵌套代理，让调用链变得不必要地复杂。请删除 `accountServiceTarget` 和 `ProxyFactoryBean`，恢复一个普通的 `accountService` Bean：

```xml
<bean id="accountService"
      class="cn.siyes.training.spring.xml.service.AccountService">
    <constructor-arg ref="accountRepository"/>
    <constructor-arg ref="auditLogRepository"/>
</bean>
```

在 XML 根节点加入事务命名空间：

```xml
xmlns:tx="http://www.springframework.org/schema/tx"
```

在 `xsi:schemaLocation` 中加入：

```xml
http://www.springframework.org/schema/tx
https://www.springframework.org/schema/tx/spring-tx.xsd
```

配置 JDBC 事务管理器：

```xml
<bean id="transactionManager"
      class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <constructor-arg ref="dataSource"/>
</bean>
```

配置事务规则：

```xml
<tx:advice id="transactionAdvice"
           transaction-manager="transactionManager">
    <tx:attributes>
        <tx:method name="transfer"
                   propagation="REQUIRED"
                   isolation="READ_COMMITTED"
                   rollback-for="java.lang.Exception"/>
    </tx:attributes>
</tx:advice>
```

配置同一个方法上的耗时和事务 Advisor：

```xml
<aop:config proxy-target-class="true">
    <aop:pointcut id="transferMethod"
                  expression="execution(* cn.siyes.training.spring.xml.service.AccountService.transfer(..))"/>

    <aop:advisor advice-ref="timingInterceptor"
                 pointcut-ref="transferMethod"
                 order="1"/>

    <aop:advisor advice-ref="transactionAdvice"
                 pointcut-ref="transferMethod"
                 order="2"/>
</aop:config>
```

这里设置 `proxy-target-class="true"`，让 Spring 使用基于类的代理，因此当前代码仍然可以按 `AccountService.class` 获取 Bean。被代理的类和 `transfer` 方法不能声明为 `final`，否则基于继承的代理无法覆盖该方法。

`order="1"` 的耗时拦截器在外层，`order="2"` 的事务拦截器在内层，因此本次耗时包含事务开启、业务 SQL 和提交/回滚。如果交换顺序，统计边界也会变化。

### 7.8 第一次运行：验证成功提交

在 `XmlApplication` 中从容器取得代理对象并调用：

```java
AccountService accountService =
        context.getBean("accountService", AccountService.class);

System.out.println(accountService.getClass());
accountService.transfer(1L, 2L, new BigDecimal("100.00"));
```

金额使用字符串创建：

```java
new BigDecimal("100.00")
```

不要使用 `new BigDecimal(100.00)`，因为它会先接收不精确的 `double`。

运行成功后，在 Navicat 查询：

```sql
SELECT id, owner_name, balance FROM accounts ORDER BY id;
SELECT id, from_account_id, to_account_id, amount, created_at
FROM transfer_logs
ORDER BY id;
```

从初始数据转账 `100.00` 后，预期结果：

```text
Alice: 900.00
Bob:   600.00
transfer_logs: 新增 1 条记录
```

同时应看到 `TimingInterceptor` 输出耗时，且打印出来的 Bean 类型是 Spring 生成的代理类。

### 7.9 第二次运行：主动制造异常并验证回滚

先在 Navicat 使用 7.3 的重置 SQL 恢复余额和日志。然后在 `transfer()` 的“入账成功”和“写日志”之间临时加入：

```java
throw new TransferException("模拟入账后发生异常");
```

再次运行后应观察到 Java 抛出异常。然后在 Navicat 查询：

```sql
SELECT id, owner_name, balance FROM accounts ORDER BY id;
SELECT * FROM transfer_logs;
```

正确结果是：

```text
Alice: 1000.00
Bob:    500.00
transfer_logs: 0 条记录
```

虽然扣款和入账 SQL 在异常前已经执行，但它们只在当前事务中可见，还没有最终提交。异常穿过代理返回给 `TransactionInterceptor` 后，事务管理器调用 `rollback()`，撤销当前事务中尚未提交的修改。这比“SQL 要等 commit 才生效”更准确。

验证完成后删除这行模拟异常代码，再恢复正常转账。

### 7.10 这条事务链路要能够解释

```text
调用代理对象 transfer(...)
  -> TransactionInterceptor 匹配 XML 事务规则
  -> DataSourceTransactionManager 取得 Connection
  -> connection.setAutoCommit(false)
  -> 把 Connection 绑定到当前线程
  -> AccountRepository 的 JdbcTemplate 取得同一条 Connection
  -> AuditLogRepository 的 JdbcTemplate 仍取得同一条 Connection
  -> 正常返回：commit
  -> 抛出符合回滚规则的异常：rollback
  -> 解绑并关闭 Connection
```

`commit()` 不是由 Repository 调用，也不是由 Service 调用，而是由代理外层的事务管理器调用。业务代码只表达“转账由哪几步组成”，XML 决定“哪些业务方法需要事务”。

### 7.11 本阶段暂停点

本次已完成以下四项：

- [x] Navicat 中能看到数据库、两张表和两条初始账户数据。
- [x] Repository 能通过 `JdbcTemplate` 完成扣款、入账和日志插入。
- [x] 正常转账后三步一起提交。
- [x] 中途抛出异常后三步一起回滚。

### 7.12 本次事务验收记录（2026-08-19）

- 正常转账 `100.00` 后，付款账户余额为 `900.00`，收款账户余额为 `600.00`，`transfer_logs` 新增一条记录，证明账户更新和日志写入一起提交。
- 在日志插入成功后主动抛出 `TransferException`，两个账户恢复为 `1000.00` 和 `500.00`，日志表没有留下记录，证明已经执行的三步数据库修改一起回滚。
- 删除模拟异常并恢复正常判断后再次运行，账户余额和日志记录再次符合成功提交预期，证明故障测试后的代码和数据库状态已恢复。
- 控制台输出 CGLIB 代理类、`transfer` 执行日志、耗时日志和容器销毁日志，证明事务代理与前面完成的 AOP、生命周期练习仍然连通。

事务练习验收完成。当前不增加连接池、并发转账、事务传播嵌套和自动化测试；它们都有实际价值，但会分散本轮对“XML 事务代理如何管理同一条数据库连接”的观察。

## 8. BeanDefinition 和 BeanPostProcessor 扩展点练习

你提到的“在 BeanDefinition 和 singletonObjects 生成前后通过类似拦截器调整对象”，对应 Spring 的后置处理器体系。它们不是同一个时机：

```text
XML BeanDefinitionReader
  -> 读取 XML，注册 BeanDefinition
  -> BeanDefinitionRegistryPostProcessor（可注册或修改 BeanDefinition）
  -> BeanFactoryPostProcessor（修改 BeanDefinition）
  -> 实例化普通 Bean
  -> 属性注入
  -> BeanPostProcessor.beforeInitialization
  -> init-method / InitializingBean
  -> BeanPostProcessor.afterInitialization
  -> 把最终返回对象放入 singletonObjects
```

### 8.1 三个概念先分清

| 扩展点 | 处理对象 | 执行时机 | 练习重点 |
| --- | --- | --- | --- |
| `BeanDefinitionRegistryPostProcessor` | BeanDefinition 注册表 | Bean 实例化前，普通 BeanFactoryPostProcessor 前 | 动态注册或修改 BeanDefinition |
| `BeanFactoryPostProcessor` | 已注册的 BeanDefinition | Bean 实例化前 | 修改类名、作用域、属性值等定义元数据 |
| `BeanPostProcessor` | 已实例化并完成注入的 Bean | 初始化方法前后 | 修改 Bean 对象，或返回包装/代理对象 |

需要特别纠正：`BeanPostProcessor` 不是直接操作 `singletonObjects`。它的 `postProcessBeforeInitialization` 和 `postProcessAfterInitialization` 返回一个对象，Spring 会继续使用最终返回值；对于 singleton，容器随后才会把这个最终对象放入 singleton 缓存。因此可以理解为“影响最终进入 singletonObjects 的对象”，但不要把它写成直接修改缓存。

### 8.2 先写一个用于观察的普通 Bean

创建：

```text
src/main/java/cn/siyes/training/spring/xml/postprocessor/PostProcessorProbe.java
```

普通 Java 类只保留以下成员：

```java
private String label;

public void setLabel(String label) {
    this.label = label;
}

public String getLabel() {
    return label;
}

public void init() {
    System.out.println("init label=" + label);
}

public void print() {
    System.out.println("print label=" + label);
}
```

XML 先注册：

```xml
<bean id="postProcessorProbe"
      class="cn.siyes.training.spring.xml.postprocessor.PostProcessorProbe"
      init-method="init">
    <property name="label" value="from-xml"/>
</bean>
```

先不写任何后置处理器，运行 `getBean("postProcessorProbe")`，确认初始化日志和 `print()` 输出都是 `from-xml`。

### 8.3 手写 BeanFactoryPostProcessor：修改 BeanDefinition

创建普通 Java 类：

```text
src/main/java/cn/siyes/training/spring/xml/postprocessor/ProbeDefinitionPostProcessor.java
```

实现接口：

```java
org.springframework.beans.factory.config.BeanFactoryPostProcessor
```

在 `postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)` 中：

1. 通过 `beanFactory.getBeanDefinition("postProcessorProbe")` 取得 BeanDefinition。
2. 从 `getPropertyValues()` 中找到 `label` 属性。
3. 将它从 `from-xml` 改成 `from-bean-definition-post-processor`。
4. 打印一行日志，证明此时还没有调用 `postProcessorProbe.init()`。

关键 API 形态如下，具体类名和日志内容由你自己补全：

```java
@Override
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    BeanDefinition definition =
            beanFactory.getBeanDefinition("postProcessorProbe");

    definition.getPropertyValues().addPropertyValue(
            "label", "from-bean-definition-post-processor");
}
```

需要导入：

```java
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
```

XML 注册这个处理器：

```xml
<bean id="probeDefinitionPostProcessor"
      class="cn.siyes.training.spring.xml.postprocessor.ProbeDefinitionPostProcessor"/>
```

Spring 会识别实现了 `BeanFactoryPostProcessor` 的 Bean，并在创建普通业务 Bean 之前自动调用它。不要在 `XmlApplication` 中手动调用 `postProcessBeanFactory`，否则会绕过 Spring 的生命周期管理。

运行后观察顺序：

```text
BeanFactoryPostProcessor 执行
init label=from-bean-definition-post-processor
```

这证明它修改的是 BeanDefinition，不是已经创建好的 `PostProcessorProbe` 对象。

### 8.4 手写 BeanPostProcessor：处理 Bean 实例

创建：

```text
src/main/java/cn/siyes/training/spring/xml/postprocessor/ProbeBeanPostProcessor.java
```

实现接口：

```java
org.springframework.beans.factory.config.BeanPostProcessor
```

两个回调都必须返回一个对象。返回原对象表示继续使用当前实例；返回其他对象表示后续容器和调用方将使用替换后的实例：

```java
@Override
public Object postProcessBeforeInitialization(Object bean, String beanName) {
    // 修改初始化前的 Bean 实例
    return bean;
}

@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // 可以返回 bean，也可以返回包装对象或代理对象
    return bean;
}
```

只针对 `postProcessorProbe` 处理：

```java
public Object postProcessBeforeInitialization(Object bean, String beanName) {
    if ("postProcessorProbe".equals(beanName)) {
        PostProcessorProbe probe = (PostProcessorProbe) bean;
        probe.setLabel(probe.getLabel() + "-before-init");
    }
    return bean;
}

public Object postProcessAfterInitialization(Object bean, String beanName) {
    if ("postProcessorProbe".equals(beanName)) {
        PostProcessorProbe probe = (PostProcessorProbe) bean;
        probe.setLabel(probe.getLabel() + "-after-init");
    }
    return bean;
}
```

XML 注册：

```xml
<bean id="probeBeanPostProcessor"
      class="cn.siyes.training.spring.xml.postprocessor.ProbeBeanPostProcessor"/>
```

观察顺序：

```text
BeanFactoryPostProcessor 执行
Bean 实例化和属性注入
BeanPostProcessor.beforeInitialization
init-method
BeanPostProcessor.afterInitialization
getBean / print
```

预期 `init()` 看到 `...-before-init`，`print()` 看到 `...-before-init-after-init`。这能区分 BeanDefinition 修改和 Bean 实例修改：前者影响实例化前的配置，后者直接处理已经创建的对象。

### 8.5 练习“最终对象被替换”

这一节只练习一个问题：`postProcessAfterInitialization` 返回的对象，是否会成为后续 `getBean` 拿到的对象。先使用手写包装类，不急着引入 JDK 动态代理，这样可以把“替换对象”和“代理机制”分开观察。

#### 8.5.1 先用接口约束 Bean

新增接口：

```text
src/main/java/cn/siyes/training/spring/xml/postprocessor/ProbeView.java
```

内容保持最小：

```java
public interface ProbeView {
    void print();
    String getLabel();
}
```

让现有的 `PostProcessorProbe` 实现这个接口：

```java
public class PostProcessorProbe implements ProbeView {
    // 保留原有 label、setLabel、init 和 print 实现
}
```

这里使用接口有两个原因：

1. 原始对象和包装对象都可以通过同一个类型被调用。
2. 包装后不能再假设 `getBean` 一定返回 `PostProcessorProbe`，应该按接口获取。

#### 8.5.2 创建包装对象

新增：

```text
src/main/java/cn/siyes/training/spring/xml/postprocessor/ProbeViewWrapper.java
```

它持有原始对象，并实现相同接口：

```java
public class ProbeViewWrapper implements ProbeView {
    private final ProbeView delegate;

    public ProbeViewWrapper(ProbeView delegate) {
        this.delegate = delegate;
    }

    @Override
    public void print() {
        System.out.println("wrapper before");
        delegate.print();
        System.out.println("wrapper after");
    }

    @Override
    public String getLabel() {
        return delegate.getLabel();
    }
}
```

`delegate` 是原来的 `PostProcessorProbe` 实例。包装类不重新实现业务逻辑，只在调用前后增加观察日志，这一点和前面 `TimingInterceptor` 的目的相似，但这里是手动包装对象。

#### 8.5.3 在 BeanPostProcessor 中返回包装对象

修改 `ProbeBeanPostProcessor` 的 `postProcessAfterInitialization`。只处理目标 Bean，其他 Bean 必须原样返回：

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (!"postProcessorProbe".equals(beanName)) {
        return bean;
    }

    System.out.println("返回 ProbeViewWrapper");
    return new ProbeViewWrapper((ProbeView) bean);
}
```

这里的关键变化是：

```java
return bean;
```

变成了：

```java
return new ProbeViewWrapper(...);
```

Spring 后续会继续使用这个返回值。它不是把原始对象从 Java 内存中删除，而是把“对外暴露和后续获取的 Bean 引用”替换成包装对象。

#### 8.5.4 XML 和启动代码调整

XML 中保留原来的目标 Bean 和后置处理器注册：

```xml
<bean id="postProcessorProbe"
      class="cn.siyes.training.spring.xml.postprocessor.PostProcessorProbe"
      init-method="init">
    <property name="label" value="from-xml"/>
</bean>

<bean id="probeBeanPostProcessor"
      class="cn.siyes.training.spring.xml.postprocessor.ProbeBeanPostProcessor"/>
```

启动类不要再按实现类获取：

```java
ProbeView probe = context.getBean("postProcessorProbe", ProbeView.class);
probe.print();
System.out.println(probe.getClass().getName());
```

需要导入：

```java
import cn.siyes.training.spring.xml.postprocessor.ProbeView;
```

如果仍然写成：

```java
context.getBean("postProcessorProbe", PostProcessorProbe.class);
```

可能出现类型不匹配，因为容器对外返回的已经是 `ProbeViewWrapper`，它不是 `PostProcessorProbe` 的子类。这正是本练习要观察的现象。

#### 8.5.5 预期调用链和输出

启动时的关键顺序应类似：

```text
Bean 实例化
属性注入
BeanPostProcessor.beforeInitialization
init label=...
返回 ProbeViewWrapper
BeanPostProcessor.afterInitialization
getBean
wrapper before
原始 Bean 的 print 输出
wrapper after
```

然后比较：

```java
Object bean = context.getBean("postProcessorProbe");
System.out.println(bean.getClass().getName());
System.out.println(bean instanceof ProbeViewWrapper);
System.out.println(bean instanceof PostProcessorProbe);
```

预期：

```text
...ProbeViewWrapper
true
false
```

这证明 `BeanPostProcessor` 的返回值影响了容器最终对外提供的对象。对于 singleton，容器会把这个最终返回对象作为后续 `getBean` 的结果缓存使用。

#### 8.5.6 与 Spring AOP 的关系

这一步和 Spring AOP 的共同点是：都可以让调用方拿到代理或包装后的对象；区别是：

```text
本练习：BeanPostProcessor 手动返回 ProbeViewWrapper
Spring AOP：根据切点自动创建代理，并组合 Advice
```

Spring AOP 的自动代理创建本身也依赖 `BeanPostProcessor` 体系。现在先手写一个最小包装器，是为了看清“后置处理器返回新对象”这个底层动作。

不要直接访问或修改 Spring 的 `singletonObjects`，它是容器内部缓存，不属于业务扩展 API。你要观察的是 `getBean` 返回值、对象类型和调用日志。

#### 8.5.7 验收标准

- [x] `PostProcessorProbe` 和 `ProbeViewWrapper` 都实现 `ProbeView`。
- [x] `postProcessAfterInitialization` 只替换 `postProcessorProbe`，其他 Bean 原样返回。
- [x] `getBean("postProcessorProbe", ProbeView.class)` 成功获取包装对象。
- [x] 调用 `print()` 时先出现 `wrapper before`，再出现原始 Bean 输出，最后出现 `wrapper after`。
- [x] `getClass()` 显示包装类，`instanceof ProbeViewWrapper` 为 `true`。
- [x] 能解释为什么按 `PostProcessorProbe.class` 获取可能失败。

### 8.6 提前扩展：BeanDefinitionRegistryPostProcessor

这一节练习“BeanDefinition 已经开始注册，但普通业务 Bean 还没有实例化”的扩展点。它比普通 `BeanFactoryPostProcessor` 更早，除了可以修改已有定义，还可以向注册表新增 BeanDefinition。

先校正一个时间概念：它不是 XML 解析前的拦截器。Spring 先读取 XML 并注册 XML 中声明的 BeanDefinition，随后才调用 `BeanDefinitionRegistryPostProcessor`。因此它能看到并修改已有定义，也能在容器实例化 singleton 之前追加新的定义。

#### 8.6.1 先明确目标和时序

本次动态注册一个名为 `dynamicProbe` 的 Bean：

```text
读取 applicationContext.xml
  -> 注册 XML 中已有的 BeanDefinition
  -> 创建并调用 RegistryPostProcessor
  -> RegistryPostProcessor 注册 dynamicProbe 的 BeanDefinition
  -> BeanFactoryPostProcessor 处理 BeanDefinition
  -> Spring 实例化 dynamicProbe
  -> 属性注入、BeanPostProcessor、init-method
  -> getBean("dynamicProbe") 返回对象
```

注意：`postProcessBeanDefinitionRegistry` 阶段只有定义，还没有 `dynamicProbe` 对象，所以不能在这个方法里调用：

```java
registry.getBean("dynamicProbe"); // 错误思路，registry 不是 BeanFactory
```

也不要在该阶段手动 `new PostProcessorProbe()` 作为最终 Bean。这里的目标是注册“如何创建 Bean 的元数据”，让后续仍由 Spring 创建和管理对象。

#### 8.6.2 创建 RegistryPostProcessor

创建：

```text
src/main/java/cn/siyes/training/spring/xml/postprocessor/DynamicProbeRegistryPostProcessor.java
```

实现接口：

```java
org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
```

先写出类结构：

```java
public class DynamicProbeRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(
            BeanDefinitionRegistry registry) {
        // 在这里注册 dynamicProbe
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) {
        // 先打印日志，观察它晚于 postProcessBeanDefinitionRegistry 执行
    }
}
```

需要导入：

```java
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
```

#### 8.6.3 用 RootBeanDefinition 注册新定义

在 `postProcessBeanDefinitionRegistry` 中完成以下步骤：

1. 判断 `dynamicProbe` 是否已经存在，避免重复注册。
2. 创建 `RootBeanDefinition`，指定 Bean 的 class 为 `PostProcessorProbe.class`。
3. 给定义增加 `label` 属性，值写成 `from-registry-post-processor`。
4. 使用 `registry.registerBeanDefinition("dynamicProbe", definition)` 注册。
5. 打印 `dynamicProbe BeanDefinition 已注册`。

参考结构如下，关键代码由你手动补全：

```java
@Override
public void postProcessBeanDefinitionRegistry(
        BeanDefinitionRegistry registry) {
    if (registry.containsBeanDefinition("dynamicProbe")) {
        return;
    }

    RootBeanDefinition definition =
            new RootBeanDefinition(PostProcessorProbe.class);
    definition.getPropertyValues().addPropertyValue(
            "label", "from-registry-post-processor");

    registry.registerBeanDefinition("dynamicProbe", definition);
    System.out.println("dynamicProbe BeanDefinition 已注册");
}
```

需要额外导入：

```java
import org.springframework.beans.factory.support.RootBeanDefinition;
```

这里的 `definition` 不是 `PostProcessorProbe` 实例，而是描述“Spring 将来如何创建它”的元数据。`RootBeanDefinition(PostProcessorProbe.class)` 表示实例化时使用这个 class；`addPropertyValue` 表示实例化后进行 Setter 注入。

#### 8.6.4 在 XML 中注册处理器本身

`DynamicProbeRegistryPostProcessor` 自己也必须先成为 Spring Bean，Spring 才能发现并调用它。在 `applicationContext.xml` 中添加：

```xml
<bean id="dynamicProbeRegistryPostProcessor"
      class="cn.siyes.training.spring.xml.postprocessor.DynamicProbeRegistryPostProcessor"/>
```

这里 XML 只注册“处理器”，不注册 `dynamicProbe`：

```text
dynamicProbeRegistryPostProcessor -> XML 显式注册
dynamicProbe                       -> Java 代码动态注册
```

不要再添加：

```xml
<bean id="dynamicProbe" .../>
```

否则就无法观察动态注册；还可能因为同名定义导致覆盖或重复注册问题。

#### 8.6.5 验证动态 Bean 参与完整生命周期

在 `XmlApplication` 中，容器创建完成后获取动态 Bean。因为当前 `PostProcessorProbe` 通过 `ProbeView` 对外使用，所以按接口获取：

```java
ProbeView dynamicProbe =
        context.getBean("dynamicProbe", ProbeView.class);

dynamicProbe.print();
System.out.println(dynamicProbe.getClass().getName());
```

预期能看到：

```text
dynamicProbe BeanDefinition 已注册
DynamicProbeRegistryPostProcessor.postProcessBeanFactory
调用 setLabel=from-registry-post-processor
init初始化label=from-registry-post-processor
输出label=from-registry-post-processor
cn.siyes.training.spring.xml.postprocessor.PostProcessorProbe
```

这组结果分别证明：

- BeanDefinition 是在普通 Bean 实例化前注册的。
- 动态 Bean 的属性仍然由 Spring 按定义执行 Setter 注入。
- 动态 Bean 仍然会执行 `init-method`。
- 动态注册的 Bean 仍然会参与已经注册的 BeanPostProcessor 流程。
- `getBean("dynamicProbe")` 可以取得这个动态注册的 singleton。

如果你在 `ProbeBeanPostProcessor` 中把处理条件从精确名称改成按类型处理，要注意 `dynamicProbe` 也可能被包装成 `ProbeViewWrapper`。本次先保持精确名称 `postProcessorProbe`，避免两个练习互相干扰。

#### 8.6.6 对比三个扩展点

完成后用下面这张表复述：

| 扩展点 | 能否新增 BeanDefinition | 处理对象 | 典型用途 |
| --- | --- | --- | --- |
| `BeanDefinitionRegistryPostProcessor` | 可以 | BeanDefinition 注册表 | 动态注册 Bean、修改定义 |
| `BeanFactoryPostProcessor` | 通常不负责新增注册，重点是修改已有定义 | BeanDefinition | 统一修改作用域、属性、占位符等元数据 |
| `BeanPostProcessor` | 不负责注册定义 | 已实例化 Bean | 初始化前后修改对象、包装对象、创建代理 |

最终链路应能用自己的话说明：

```text
RegistryPostProcessor 注册“怎么创建 dynamicProbe”
  -> BeanFactory 根据定义创建 PostProcessorProbe
  -> BeanPostProcessor 处理实例
  -> init-method 执行
  -> 最终对象进入容器的 singleton 管理流程
```

不要直接访问或修改 Spring 的 `singletonObjects`。它是容器内部缓存；本练习通过注册表、生命周期日志和 `getBean` 结果观察容器行为。

#### 8.6.7 验收标准

- [x] 处理器本身通过 XML 注册，`dynamicProbe` 没有写在 XML 中。
- [x] `postProcessBeanDefinitionRegistry` 能注册 `dynamicProbe` 的 BeanDefinition。
- [x] `dynamicProbe` 能按 `ProbeView` 类型通过 `getBean` 获取。
- [x] 动态 Bean 的 `label` 来自 `RootBeanDefinition` 的属性配置。
- [x] 动态 Bean 能执行属性注入和 `init-method`。
- [x] 能解释为什么注册阶段不能直接取得 Bean 实例。
- [x] 能解释 RegistryPostProcessor 与 BeanFactoryPostProcessor、BeanPostProcessor 的时机差异。

## 9. 纯 XML 轮检查点

完成一个检查点后再继续下一个：

- [x] `ClassPathXmlApplicationContext` 能加载 XML。
- [x] XML 能创建 Repository 和 Service，并完成构造器注入。
- [x] 能用 `<property>` 完成 Setter 注入，并解释两种注入方式的取舍。
- [x] 能观察 `init-method`、`destroy-method`、singleton 和 prototype 的差异。
- [x] 能使用普通 Java 切面类和 XML 创建 AOP 代理。
- [x] 能解释 `proceed()` 与 Filter 的 `chain.doFilter()` 的相似点。
- [x] 能用 XML `<tx:advice>` 描述事务边界，并观察成功提交和异常回滚。
- [x] 能用自己的话解释 XML 中每个 Bean、引用、代理和事务配置的作用。
- [x] 能区分 `BeanDefinitionRegistryPostProcessor`、`BeanFactoryPostProcessor` 和 `BeanPostProcessor` 的处理对象与执行时机。
- [x] 能证明 BeanFactoryPostProcessor 修改的是 BeanDefinition，BeanPostProcessor 处理的是 Bean 实例。
- [x] 能解释 BeanPostProcessor 返回值如何影响最终交给容器使用的对象。

## 10. XML 轮完成后暂停

纯 XML 轮的核心检查点已完成，先不要开始注解版。复盘以下问题：

1. 哪些对象由 Spring 创建，哪些对象仍由业务代码创建。
2. `getBean` 返回目标对象还是代理对象。
3. XML 中哪个节点对应构造器注入、Setter 注入、AOP 和事务。
4. 如果删除 XML 中某个 Bean 定义，启动时会出现什么错误。
5. 如果业务类内部调用自己的另一个方法，代理和事务是否一定还能生效。

复盘完成后，把代码结构、运行结果和仍不理解的地方发我，我再给第二轮纯注解方式引导。不要提前把 XML 和注解混在一起。
