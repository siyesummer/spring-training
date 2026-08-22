# Spring MVC 复盘

## 一、Tomcat 为什么能调用 DispatcherServlet？

Tomcat 启动 Web 应用时会读取 `WEB-INF/web.xml`。当前项目在其中完成了两项关键配置：

- 通过 `<servlet>` 注册 Spring MVC 提供的 `DispatcherServlet`。
- 通过 `<servlet-mapping>` 将 `/` 映射给它，使应用中的请求先进入 `DispatcherServlet`。

`load-on-startup=1` 让 Tomcat 在应用启动时创建并初始化 `DispatcherServlet`，而不是等第一次请求到达时才创建。初始化参数又告诉它使用 `AnnotationConfigWebApplicationContext`，并加载 `WebMvcConfig`。

因此，Tomcat 认识的是 Servlet 规范中的 `DispatcherServlet`；Controller、Service、Repository 等 Bean 则由 `DispatcherServlet` 创建的 Spring Web 容器管理。

## 二、DispatcherServlet 如何找到某个 Controller 方法？

`DispatcherServlet` 自己不遍历 Controller。应用启动时，`@EnableWebMvc` 注册 Spring MVC 基础设施，`@ComponentScan` 将 `@RestController` 注册为 Bean，`RequestMappingHandlerMapping` 再读取类和方法上的映射注解，建立“请求条件 -> HandlerMethod”的映射表。

请求到达时，会综合匹配：

- HTTP 方法，例如 GET、POST、PATCH。
- 类级和方法级路径。
- 必要时还包括 `params`、`headers`、`consumes`、`produces` 等条件。

例如：

```java
@RequestMapping("/api/tasks")
@GetMapping("/{id}")
```

最终对应 `GET /api/tasks/{id}`。匹配结果不是立刻执行方法，而是得到一个 `HandlerMethod`，其中包含目标 Controller Bean 和具体 Java 方法。

## 三、HandlerMapping 和 HandlerAdapter 为什么分开？

两者分别回答两个问题：

```text
HandlerMapping：这个请求应该交给谁？
HandlerAdapter：找到 Handler 后，应该怎样调用它？
```

`HandlerMapping` 只负责根据请求查找 Handler，不负责解析参数或执行方法。当前注解式 Controller 得到的 Handler 通常是 `HandlerMethod`。

`HandlerAdapter` 屏蔽不同 Handler 类型的调用差异。对注解式 Controller，`RequestMappingHandlerAdapter` 会继续协调：

- `HandlerMethodArgumentResolver` 解析 `@PathVariable`、`@RequestParam`、`@ModelAttribute`、`@RequestBody` 等参数。
- Bean Validation 执行参数校验。
- 反射调用 Controller 方法。
- `HandlerMethodReturnValueHandler` 处理返回值。
- 需要读写 JSON 时调用 `HttpMessageConverter`。

把“查找”和“调用”拆开后，`DispatcherServlet` 不需要针对每种 Handler 写一套固定逻辑，Spring MVC 也能扩展不同的映射策略和调用方式。

## 四、四种主要参数来源

- `@PathVariable`：来自 URL 路径模板，例如 `/api/tasks/{id}` 中的 `id`。
- `@RequestParam`：来自查询字符串或表单参数，例如 `/api/tasks?page=1&size=10` 中的 `page` 和 `size`。
- `@ModelAttribute`：从请求参数中取出多个字段，通过数据绑定组装成 Java 对象；本项目用它接收分页和筛选条件。它不是读取 JSON 请求体。
- `@RequestBody`：读取 HTTP 请求体，通过 `HttpMessageConverter` 将 JSON 等内容转换成 Java 对象。

前端类比：`@PathVariable` 类似路由参数，`@RequestParam` 类似 `location.search`，`@RequestBody` 类似请求库的 `data/body`。

## 五、JSON 与 DTO 在什么时候转换？

### JSON 转 DTO：Controller 执行之前

`HandlerAdapter` 准备调用 Controller 时，参数解析器识别到 `@RequestBody`，再让 `MappingJackson2HttpMessageConverter` 使用 Jackson 读取请求体并创建 DTO。随后 `@Valid` 触发校验。全部成功后，Controller 方法才会执行。

```text
JSON 字节
  -> @RequestBody 参数解析
  -> HttpMessageConverter + Jackson
  -> CreateTaskRequest
  -> Bean Validation
  -> Controller 方法
```

所以 JSON 转 DTO 不是 Controller 方法内部完成的。JSON 格式错误、枚举无法转换或校验失败时，Controller 方法根本不会执行。

### Java 对象转 JSON：Controller 返回之后

Controller 返回的此时仍是 `TaskResponse<Task>` 等 Java 对象。返回值处理器识别到 `@RestController` 的响应体语义，再调用消息转换器和 Jackson 将对象序列化为 JSON，最终由 Tomcat 写入 HTTP 响应。

Jackson 反序列化普通 JavaBean 时通常需要无参构造器和 setter；序列化时通常通过 getter 读取属性。本项目还关闭了 `WRITE_DATES_AS_TIMESTAMPS`，使 Java 时间类型输出为 ISO 字符串，而不是数字数组。

## 六、@Valid 为什么能阻止不合法请求进入 Controller？

`@NotBlank`、`@Size`、`@NotNull` 等注解只是声明约束；Controller 参数上的 `@Valid` 才要求 Spring 在参数解析完成后执行 Bean Validation。

校验失败时，Spring MVC 通常抛出 `MethodArgumentNotValidException`，Controller 方法不会执行。`GlobalExceptionHandler` 再将异常转换为 `400 Bad Request` JSON。

需要区分：

```text
字段缺失、为空或长度不符合约束
  -> Bean Validation 失败
  -> MethodArgumentNotValidException

JSON 语法错误或枚举值无法转换
  -> Jackson 反序列化失败
  -> HttpMessageNotReadableException
```

两者都可以返回 `400`，但失败阶段和异常类型不同。

## 七、TaskNotFoundException 如何变成 404 JSON？

Service 调用 Repository 得到 `Optional.empty()` 后，使用 `orElseThrow` 抛出 `TaskNotFoundException`。只要 Controller 没有捕获并消化这个异常，它就会沿调用栈返回到 Spring MVC。

`DispatcherServlet` 捕获处理请求时出现的异常，并委托 `HandlerExceptionResolver` 体系处理。它会找到 `@RestControllerAdvice` 中匹配的：

```java
@ExceptionHandler(TaskNotFoundException.class)
```

处理方法通过 `ResponseEntity` 同时设置真正的 HTTP `404` 和 JSON 响应体。`TaskResponse.code=404` 只是响应体字段，`ResponseEntity.status(HttpStatus.NOT_FOUND)` 才决定 HTTP 状态行。

异常链如下：

```text
Repository 返回 Optional.empty()
  -> Service 抛 TaskNotFoundException
  -> Controller 未捕获
  -> DispatcherServlet
  -> HandlerExceptionResolver
  -> @RestControllerAdvice / @ExceptionHandler
  -> HTTP 404 + JSON
```

全局 `Exception` 处理器只作为 `500` 兜底，不应把 SQL、堆栈或原始异常消息直接返回给前端；详细信息应记录在服务端日志中。

## 八、Filter 和 Interceptor 的位置及能力差异

请求位置：

```text
Tomcat
  -> Servlet Filter
  -> DispatcherServlet
  -> HandlerMapping 找到 HandlerMethod
  -> HandlerInterceptor.preHandle
  -> Controller
  -> HandlerInterceptor.postHandle / afterCompletion
```

- `Filter` 属于 Servlet 规范，在 `DispatcherServlet` 外层，可覆盖 Servlet 请求和按映射范围匹配的静态资源；它不天然知道最终会调用哪个 Controller 方法。
- `HandlerInterceptor` 属于 Spring MVC，只有请求进入 MVC Handler 链并匹配到 Handler 后才执行，可以通过 `HandlerMethod` 获得 Controller 类型和方法。
- Interceptor 不是用来拦截任意 Service 或 Repository 方法的。业务方法级横切通常使用 Spring AOP。
- `preHandle` 返回 `false` 会中止后续 Handler 调用；`afterCompletion` 可观察最终状态并做清理。

当前验收中已经观察到 `TaskController.queryById` 以及最终 HTTP `200`，证明 Interceptor 已进入正确的 MVC 请求链。

## 九、HikariDataSource、JdbcTemplate 和 Repository 的职责

- `HikariDataSource`：连接池和 `DataSource` 实现，持有 JDBC URL、用户名、密码和连接池参数，负责创建、复用、回收数据库连接。
- `JdbcTemplate`：Spring JDBC 模板，负责连接获取与释放、创建和执行 Statement、参数绑定、异常转换等重复流程；SQL 和行映射规则仍由开发者编写。
- `JdbcTaskRepository`：任务数据访问实现，负责 SQL、参数顺序、`RowMapper`、枚举与数据库字符串之间的转换，并实现 `TaskRepository` 约定。

Service 依赖 `TaskRepository` 接口，而不是 `JdbcTaskRepository` 具体类。以后切换到 MyBatis 时，主要变化应集中在：

- Maven 依赖与 MyBatis 配置。
- Mapper 接口和 SQL 映射。
- Repository/数据访问实现。

Controller、请求 DTO、响应 DTO 和大部分 Service 业务逻辑应尽量保持不变。这正是依赖接口的价值。MyBatis 也不会消除数据库建模、SQL、参数安全和事务边界这些问题，只是改变数据访问的实现方式。

## 十、Spring Boot 会自动完成哪些工作？

引入合适的 Spring Boot Starter 后，Boot 会基于依赖、配置和条件自动配置完成大量装配工作，例如：

- 使用内嵌 Tomcat 启动 Web 应用，通常不再需要手动安装外部 Tomcat 和编写 `web.xml`。
- 自动注册和配置 `DispatcherServlet`。
- 提供 Spring MVC、Jackson、Bean Validation 等常用组件的默认装配。
- 根据 `application.yml`、环境变量等配置创建 `DataSource`、HikariCP 和 `JdbcTemplate`。
- 提供统一的配置加载、日志、错误处理和可执行 JAR 打包方式。
- Starter 通过传递依赖减少逐个声明 Spring MVC 相关 JAR 的工作。

但 Boot 不是替开发者完成业务开发，也不会自动替代以下内容：

- Controller 路由和接口契约设计。
- DTO 校验规则、Service 业务规则和事务边界。
- Repository/Mapper 与 SQL。
- HTTP 状态码、异常响应、CORS 和安全策略的业务取舍。

Boot 的本质是在 Spring 与 Spring MVC 之上提供约定、自动配置和启动能力，当前练习的 Controller、参数解析、消息转换、校验和异常处理机制仍然存在，只是大量基础配置被隐藏了。

## 十一、一次完整请求链复盘

以创建任务为例，更准确的顺序是：

```text
客户端发送 POST JSON
  -> Tomcat 创建 HttpServletRequest / HttpServletResponse
  -> Servlet Filter 链
  -> DispatcherServlet
  -> HandlerMapping 找到 TaskController.insert 的 HandlerMethod
  -> Interceptor.preHandle
  -> HandlerAdapter 准备调用方法
  -> ArgumentResolver 识别 @RequestBody
  -> HttpMessageConverter + Jackson 将 JSON 转为 CreateTaskRequest
  -> Bean Validation 执行约束
  -> Controller 调用 Service
  -> Service 执行业务规则和事务
  -> Repository 通过 JdbcTemplate 访问 MySQL
  -> Controller 组装 TaskResponse，并由 ResponseEntity 指定 HTTP 201
  -> ReturnValueHandler 处理返回值
  -> HttpMessageConverter + Jackson 将 Java 对象转为 JSON
  -> Interceptor.postHandle
  -> Interceptor.afterCompletion 观察最终状态
  -> Tomcat 返回 HTTP 响应
```

`preHandle` 在 Controller 参数解析和 `@Valid` 校验之前执行，因为它由 `DispatcherServlet` 在调用 `HandlerAdapter` 前触发。如果参数解析或校验失败，Controller 不执行，但已经通过的 `preHandle` 仍然发生，最终异常进入 Spring MVC 的异常解析流程。

## 十二、当前能力评价

本次复盘说明已经形成了 Spring MVC 的基础整体认知：能够把 Servlet 容器、`DispatcherServlet`、Controller、Service、Repository、数据库和 JSON 响应串成完整链路，也理解了四种参数来源、统一异常处理和接口依赖的价值。

当前达到的是“能够独立完成并解释最小 Spring MVC REST API”的阶段，还不能据此认为已经掌握 Spring MVC 内部实现。需要继续巩固的部分主要是：

- `HandlerMapping`、`HandlerAdapter`、参数解析器和返回值处理器之间的协作。
- 参数转换、校验、Interceptor 和 Controller 的精确执行顺序。
- HTTP 状态码与 JSON 业务码的区别。
- Spring Boot 自动配置后，识别哪些机制仍来自 Spring MVC。

结合本 Module 的手写代码和 Apifox 验收，已经具备进入 MyBatis 阶段的 Spring MVC 基础。
