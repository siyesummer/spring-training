# Spring MVC 单容器与父子容器专题

> 适用 Module：`03-spring-mvc`
> 当前项目实际方案：单个 `WebApplicationContext`
> 父子容器配置：作为机制学习和可选实验，不要求现在修改主线代码
> 核心问题：谁创建容器、Bean 放在哪里、依赖如何查找、怎样避免重复扫描

## 1. 先明确这里的“容器”是什么

当前项目中同时存在几种容易都被叫作“容器”的对象：

```text
Tomcat
  -> Servlet 容器，管理 Web 应用、Servlet、Filter、Listener

WebApplicationContext
  -> Spring Bean 容器，管理 Controller、Service、Repository 等 Bean

HikariDataSource
  -> 数据库连接池，管理可复用的数据库连接
```

本文讨论“一个容器还是父子容器”时，特指 Spring 的 `ApplicationContext` 数量，不是 Tomcat 数量，也不是数据库连接池数量。

## 2. 当前项目为什么只有一个 Spring 容器

当前 `web.xml` 只注册了 `DispatcherServlet`：

```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>

    <init-param>
        <param-name>contextClass</param-name>
        <param-value>org.springframework.web.context.support.AnnotationConfigWebApplicationContext</param-value>
    </init-param>

    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>cn.siyes.training.mvc.config.WebMvcConfig</param-value>
    </init-param>

    <load-on-startup>1</load-on-startup>
</servlet>
```

启动链是：

```text
Tomcat 读取 web.xml
  -> 创建 DispatcherServlet
  -> 调用 DispatcherServlet.init()
  -> DispatcherServlet 创建 AnnotationConfigWebApplicationContext
  -> 加载 WebMvcConfig
  -> 扫描并创建项目 Bean
```

当前 `WebMvcConfig` 扫描整个业务根包：

```java
@ComponentScan("cn.siyes.training.mvc")
```

因此同一个 Spring 容器会持有：

```text
DispatcherServlet WebApplicationContext
├─ WebMvcConfig
├─ DatabaseConfig
├─ HandlerMapping
├─ HandlerAdapter
├─ HttpMessageConverter
├─ Controller
├─ RestControllerAdvice
├─ Interceptor
├─ Service
├─ Repository
├─ HikariDataSource
├─ JdbcTemplate
└─ TransactionManager
```

这不是“没有分层”。Controller、Service 和 Repository 的代码职责仍然分层，只是它们的 Bean 都保存在同一个 Spring 容器中。

## 3. WebApplicationContext 比普通 Spring 容器多什么

Spring Core 阶段使用过：

```java
AnnotationConfigApplicationContext
```

Spring MVC 中常见的是：

```java
AnnotationConfigWebApplicationContext
```

两者都能完成：

- 读取 BeanDefinition。
- 创建和注入 Bean。
- 执行生命周期回调和后置处理器。
- 创建 AOP 和事务代理。

`WebApplicationContext` 还与 Servlet Web 环境关联，可以访问：

- `ServletContext` 和 `ServletConfig`。
- request、session、application 等 Web Scope。
- Web 资源和 Servlet 环境属性。

类型关系可以简化为：

```text
ApplicationContext
  └─ WebApplicationContext
       └─ ConfigurableWebApplicationContext
            └─ AnnotationConfigWebApplicationContext
```

它仍然是 Spring 容器，只是增加了 Web 环境能力。

## 4. 传统父子容器的总体结构

父子容器版本会把 Bean 分成两组：

```text
Tomcat
│
├─ ContextLoaderListener
│    └─ Root WebApplicationContext
│         ├─ Service
│         ├─ Repository
│         ├─ HikariDataSource
│         ├─ JdbcTemplate
│         └─ TransactionManager
│
└─ DispatcherServlet
     └─ Child WebApplicationContext
          ├─ Controller
          ├─ RestControllerAdvice
          ├─ Interceptor
          ├─ HandlerMapping
          ├─ HandlerAdapter
          └─ HttpMessageConverter
```

两者都是 Spring 容器：

- Root 容器表示整个 Web 应用共享的业务基础设施。
- Child 容器表示某一个 `DispatcherServlet` 专属的 MVC 基础设施和 Web Bean。

“Root”和“Child”是容器层级，不是 Java 类继承，也不是包目录天然形成的关系。

## 5. 两个容器分别由谁创建

### 5.1 ContextLoaderListener 创建 Root 容器

`ContextLoaderListener` 实现 Servlet Listener。Tomcat 启动 Web 应用时回调：

```text
ServletContextListener.contextInitialized(...)
  -> ContextLoaderListener 初始化
  -> 创建 Root WebApplicationContext
  -> 将 Root 容器保存到 ServletContext
```

Root 容器的生命周期大体与整个 Web 应用一致。

### 5.2 DispatcherServlet 创建 Child 容器

Tomcat 随后根据 `load-on-startup` 初始化 `DispatcherServlet`：

```text
DispatcherServlet.init()
  -> 从 ServletContext 找到 Root WebApplicationContext
  -> 创建自己的 WebApplicationContext
  -> 把 Root 容器设为 parent
  -> 加载 WebMvcConfig
```

因此不是 Listener 一次性创建两个容器：

```text
ContextLoaderListener -> Root 容器
DispatcherServlet     -> Child 容器
```

应用关闭时顺序大体相反：Servlet 销毁并关闭 Child 容器，然后 Listener 关闭 Root 容器。容器关闭会触发其中 Bean 的销毁回调，例如关闭 Hikari 连接池。

## 6. Bean 查找方向为什么是 Child -> Root

假设 `TaskController` 位于 Child 容器，`TaskService` 位于 Root 容器：

```java
public TaskController(TaskService taskService) {
    this.taskService = taskService;
}
```

创建 Controller 时，Child 容器按下面的方向查找依赖：

```text
在 Child 容器本地查找 TaskService
  -> 没找到
  -> 向 parent，也就是 Root 容器查找
  -> 找到 TaskService
  -> 注入 TaskController
```

因此：

```text
Child 可以使用 Root Bean
Root 不能反向查找 Child Bean
```

这与代码依赖方向一致：

```text
Controller -> Service -> Repository
```

Root 中的 Service 不应该反向依赖 Child 中的 Controller。否则业务层将依赖 HTTP Web 层，破坏分层，也容易形成循环依赖。

如果 Root 和 Child 都有同名 Bean，Child 本地 Bean 会优先，Root Bean 会被遮蔽。这通常不是想要的结果，而是扫描范围重叠的信号。

## 7. 父子容器解决什么问题

父子容器的主要价值不是“项目看起来更高级”，而是隔离多个 Web 入口并复用业务层。

例如一个 WAR 注册两个 `DispatcherServlet`：

```text
/api/*
  -> apiDispatcherServlet
  -> API Child Context

/admin/*
  -> adminDispatcherServlet
  -> Admin Child Context
```

两个 Child 容器可以拥有各自的 Controller、Interceptor、ViewResolver 和路由规则，同时共享 Root 中的：

```text
Service
Repository
DataSource
TransactionManager
```

这样可以得到：

```text
Root WebApplicationContext
├─ TaskService
├─ JdbcTaskRepository
└─ HikariDataSource

API Child Context
└─ TaskApiController

Admin Child Context
└─ TaskAdminController
```

对于当前只有一个 REST `DispatcherServlet` 的项目，单容器已经足够，父子容器不会自动提高代码质量。

## 8. 当前项目如何改为父子容器

下面是基于当前 `03-spring-mvc` 包结构的完整可选方案。它用于理解配置方式；不要在当前主线请求链跑通前直接切换。

### 8.1 调整后的包与配置职责

```text
cn.siyes.training.mvc
├─ config/
│  ├─ RootConfig.java       # Root 入口：Service、Repository、数据库
│  ├─ DatabaseConfig.java   # DataSource、JdbcTemplate、事务管理器
│  └─ WebMvcConfig.java     # Child 入口：Controller 和 MVC
├─ controller/              # Child 扫描
├─ exception/               # Child 扫描，包含 @RestControllerAdvice
├─ interceptor/             # Child 扫描
├─ service/                 # Root 扫描
├─ repository/              # Root 扫描
├─ dto/                     # 普通对象，不需要扫描
└─ model/                   # 普通对象，不需要扫描
```

划分原则：

```text
Root：不扫描 Controller
Child：不扫描 Service、Repository、DatabaseConfig
```

### 8.2 新增 RootConfig

创建：

```text
src/main/java/cn/siyes/training/mvc/config/RootConfig.java
```

```java
package cn.siyes.training.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DatabaseConfig.class)
@ComponentScan(basePackages = {
        "cn.siyes.training.mvc.service",
        "cn.siyes.training.mvc.repository"
})
public class RootConfig {
}
```

这里的职责是：

- `@ComponentScan` 注册 Service 和 Repository。
- `@Import(DatabaseConfig.class)` 显式引入 DataSource、JdbcTemplate、事务管理器。
- 不添加 `@EnableWebMvc`，因为 Root 容器不处理 Controller 路由。
- 不扫描整个 `cn.siyes.training.mvc`，否则会把 Controller 和 WebMvcConfig 一起注册到 Root。

`DatabaseConfig` 可以保留当前的 `@Configuration` 和 `@EnableTransactionManagement`。它只被 Root 导入，因此 HikariDataSource 和事务基础设施只创建一套。

### 8.3 收窄 WebMvcConfig 的扫描范围

当前写法：

```java
@ComponentScan("cn.siyes.training.mvc")
```

在父子容器方案中必须改成：

```java
package cn.siyes.training.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "cn.siyes.training.mvc.controller",
        "cn.siyes.training.mvc.exception",
        "cn.siyes.training.mvc.interceptor"
})
public class WebMvcConfig implements WebMvcConfigurer {
    // Interceptor、CORS 等 MVC 配置继续放在这里
}
```

Child 容器会创建：

- Controller。
- `@RestControllerAdvice`。
- Interceptor。
- `@EnableWebMvc` 导入的 MVC 基础设施。

Controller 构造器需要 `TaskService` 时，Child 容器会到 Root 容器中查找。

### 8.4 完整 web.xml

父子容器版本可以使用：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
         https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <display-name>03-spring-mvc</display-name>

    <!-- ContextLoaderListener 创建 Root 容器时使用的容器类型。 -->
    <context-param>
        <param-name>contextClass</param-name>
        <param-value>org.springframework.web.context.support.AnnotationConfigWebApplicationContext</param-value>
    </context-param>

    <!-- Root 容器入口，只加载业务层和数据库配置。 -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>cn.siyes.training.mvc.config.RootConfig</param-value>
    </context-param>

    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>

        <!-- DispatcherServlet 创建 Child 容器时使用的容器类型。 -->
        <init-param>
            <param-name>contextClass</param-name>
            <param-value>org.springframework.web.context.support.AnnotationConfigWebApplicationContext</param-value>
        </init-param>

        <!-- Child 容器只加载 MVC 配置。 -->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>cn.siyes.training.mvc.config.WebMvcConfig</param-value>
        </init-param>

        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

这里有两组名称相同的配置项，但读取者不同：

| 配置位置 | 读取者 | 创建的容器 |
| --- | --- | --- |
| Web 应用级 `<context-param>` | `ContextLoaderListener` | Root WebApplicationContext |
| `<servlet>` 内 `<init-param>` | `DispatcherServlet` | Child WebApplicationContext |

如果忘记为 Listener 配置 `contextClass`，它的默认实现通常会按 XML WebApplicationContext 处理配置，并可能尝试把 `RootConfig` 当成 XML 位置，导致启动错误。

### 8.5 启动时实际发生什么

```text
1. Tomcat 创建 ServletContext

2. Tomcat 调用 ContextLoaderListener.contextInitialized()
   -> 读取 Web 应用级 contextClass
   -> 创建 AnnotationConfigWebApplicationContext，作为 Root
   -> 加载 RootConfig
   -> 创建 DatabaseConfig、DataSource、JdbcTemplate
   -> 创建 Repository、Service 和事务代理
   -> 把 Root Context 保存到 ServletContext

3. Tomcat 根据 load-on-startup 创建 DispatcherServlet
   -> DispatcherServlet 从 ServletContext 找到 Root
   -> 创建自己的 AnnotationConfigWebApplicationContext，作为 Child
   -> 设置 child.parent = root
   -> 加载 WebMvcConfig
   -> 创建 MVC 基础设施、Interceptor、Advice、Controller

4. 应用可以接收请求
```

一次请求的调用关系变为：

```text
Tomcat
  -> DispatcherServlet
  -> Child HandlerMapping 找到 TaskController
  -> Child 创建的 TaskController
  -> 注入并调用 Root 中的 TaskService 代理
  -> Root 中的 JdbcTaskRepository
  -> Root 中的 JdbcTemplate / HikariDataSource
  -> MySQL
```

父子容器只改变 Bean 的存放和查找位置，不改变 HTTP 请求最终仍由 `DispatcherServlet` 处理这一事实。

## 9. 如何证明父子关系真的生效

### 9.1 先观察启动日志

启动时应能区分两次 Context 初始化：

```text
Root WebApplicationContext 初始化
  -> HikariPool 启动一次

dispatcher 的 WebApplicationContext 初始化
  -> Controller 路由映射注册
```

如果看到 HikariPool 启动两次，应优先检查 Root 和 Child 是否都扫描了 `DatabaseConfig`。

### 9.2 使用调试器检查 Controller 所在容器

可以临时在 Controller 中注入 WebApplicationContext：

```java
private final WebApplicationContext webApplicationContext;

public TaskController(
        TaskService taskService,
        WebApplicationContext webApplicationContext) {
    this.taskService = taskService;
    this.webApplicationContext = webApplicationContext;
}
```

在 Controller 方法断点中观察：

```java
webApplicationContext.getParent()
webApplicationContext.containsLocalBean("taskController")
webApplicationContext.containsLocalBean("taskService")
webApplicationContext.containsBean("taskService")
webApplicationContext.getParent().containsLocalBean("taskService")
```

预期：

| 表达式 | 预期 | 原因 |
| --- | --- | --- |
| `getParent() != null` | `true` | Child 已关联 Root |
| Child `containsLocalBean("taskController")` | `true` | Controller 在 Child |
| Child `containsLocalBean("taskService")` | `false` | Service 不在 Child 本地 |
| Child `containsBean("taskService")` | `true` | Child 能沿父链找到 Service |
| Root `containsLocalBean("taskService")` | `true` | Service 在 Root 本地 |

这里的默认 Bean 名成立需要类名为 `TaskController`、`TaskService` 且没有显式改名。验证完成后可以删除临时注入，避免为了实验长期污染 Controller。

### 9.3 验证 Root 不能读取 Child

在调试器中观察：

```java
webApplicationContext.getParent().containsBean("taskController")
```

预期为 `false`。Root 的 BeanFactory 不会向 Child 反向查找。

## 10. 最容易出现的配置错误

### 10.1 Root 和 Child 都扫描根包

错误示例：

```java
// RootConfig
@ComponentScan("cn.siyes.training.mvc")

// WebMvcConfig
@ComponentScan("cn.siyes.training.mvc")
```

可能形成：

```text
Root TaskService  != Child TaskService
Root Repository   != Child Repository
Root DataSource   != Child DataSource
```

结果包括：

- Hikari 连接池启动两次。
- Service、Repository、事务代理和生命周期回调出现两套。
- Child Controller 优先注入 Child 中重复的 Service，失去共享 Root 的意义。
- Root 还可能错误创建 Controller，但 Root 没有完整 MVC 基础设施。

修复原则不是添加 `@Primary`，而是先正确隔离扫描范围。

### 10.2 配置了 RootConfig，但没有注册 Listener

只有 `<context-param>` 不会自动创建 Root 容器。必须存在：

```xml
<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>
```

否则参数没有读取者，项目仍只有 DispatcherServlet 创建的容器。

### 10.3 RootConfig 也添加 `@EnableWebMvc`

`@EnableWebMvc` 应位于 Child 的 WebMvcConfig。放到 Root 会把 HandlerMapping、HandlerAdapter 等 MVC Bean 创建到不负责请求分发的 Root 容器中，使职责混乱。

### 10.4 Child 扫描不到 Controller

如果 WebMvcConfig 只扫描 `service` 和 `repository`，`DispatcherServlet` 的 Child 容器中没有 Controller，HandlerMapping 就不会注册业务路由，最终表现为 404。

### 10.5 Root 扫描不到 Service

Controller Bean 位于 Child，但 Child 向 Root 查找 `TaskService` 仍找不到，启动时会出现 `NoSuchBeanDefinitionException` 或构造器依赖无法满足。

### 10.6 把 ContextLoaderListener 当成请求过滤器

Listener 只在 Web 应用生命周期事件发生时创建和关闭 Root 容器。每次 HTTP 请求不会重新调用它，也不会经过 `chain.doFilter()`。

## 11. 单容器与父子容器对照

| 对比项 | 单容器 | 父子容器 |
| --- | --- | --- |
| 创建入口 | `DispatcherServlet` | Listener 创建 Root，Servlet 创建 Child |
| Bean 存放 | Web 和业务 Bean 放在同一 Context | 业务 Bean 放 Root，Web Bean 放 Child |
| 配置数量 | 少 | 多，需要严格隔离扫描 |
| 依赖查找 | 同一容器本地查找 | Child 本地找不到时向 Root 查找 |
| 多 DispatcherServlet 共享业务 Bean | 不适合表达 | Root 可以被多个 Child 共享 |
| 重复 Bean 风险 | 较低 | 扫描重叠时较高 |
| 当前项目适用性 | 最合适 | 可选机制实验，不是必要架构 |

## 12. 与 Spring Boot 的关系

多数现代 Spring Boot 单体 Web 服务通常使用统一的应用上下文管理：

```text
一个 ApplicationContext
├─ Controller
├─ Service
├─ Repository
├─ DataSource
└─ Spring MVC 基础设施
```

Boot 自动配置并注册 `DispatcherServlet`，但不代表现代项目必须使用传统 `ContextLoaderListener + DispatcherServlet` 父子容器。

因此当前单容器方案不是“不规范的临时方案”。父子容器是特定传统 Web 架构和多 Servlet 隔离需求下的工具，不是 Spring MVC 项目的固定完成标准。

## 13. 当前阶段建议

当前应保持单容器，先完成：

```text
DispatcherServlet
  -> Controller
  -> 参数解析 / JSON 转换 / Validation
  -> Service
  -> JdbcTaskRepository
  -> MySQL
  -> 统一异常响应
```

主链路稳定后，可以在独立 Git 提交之后做一次父子容器实验：

1. 新增 `RootConfig`。
2. 收窄 `WebMvcConfig` 扫描范围。
3. 在 `web.xml` 注册 Root 的 context-param 和 `ContextLoaderListener`。
4. 观察 Root、Child 和 Bean 查找方向。
5. 确认 HikariPool 只创建一次。
6. 完成验证后决定保留父子结构，还是恢复更适合本项目的单容器结构。

最终需要掌握的不是配置数量，而是：

> 单容器把 Web Bean 和业务 Bean 放在同一个 Spring 上下文中；父子容器把共享业务 Bean 放在 Root，把某个 DispatcherServlet 专属的 MVC Bean 放在 Child。Child 可以沿父链使用 Root Bean，Root 不知道 Child Bean。扫描范围必须隔离，否则会创建重复 Bean 和重复基础设施。
