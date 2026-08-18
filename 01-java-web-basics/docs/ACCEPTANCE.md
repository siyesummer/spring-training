# javaweb总结

## 总体链路

### JSP页面、接口映射
- 通过webapp目录下的`web.xml`配置`servlet`
- `javaweb`的页面和接口都通过`web.xml`配置映射路径
- `JSP`页面，也是通过配置对应`Servlet.java`，在通过`getRequestDispatcher`去转发

### Listener
- `Listener`不处理某一个 HTTP 请求，而是由 Tomcat 在 Web 应用、Session 或请求生命周期事件发生时回调
- `ServletContextListener`可监听 Web 应用启动和停止，`HttpSessionListener`可监听 Session 创建和销毁
- Listener 需要在 `web.xml` 注册；仅有 Java 类但没有注册时，容器不会自动调用

### Filter
- 也是通过webapp目录下的`web.xml`配置
- 通过实现`jakarta.servlet.Filter`实现`doFilter`实现自己业务需求

### 执行顺序
> 用户访问页面、请求数据、发送请求时，通过`tomcat`启动的`webapp`服务会先判断该路径有没有命中`Filter`的匹配。
> 如果命中了先执行`Filter`内逻辑，这个类似于前端的路由守卫，可以在这里做一些统一拦截或者设置。
> 只有`Filter`执行了`chain.doFilter(request, response);`才能接续往下走，否则不会执行对应的`Servlet`逻辑。
> 有多个`Filter`匹配时，`chain.doFilter(request, response)`前后代码执行顺序遵循先进后出规则。

## Listener

- `ServletContextListener`、`HttpSessionListener`、`ServletRequestListener`
- Listener 则是 Tomcat 在生命周期事件发生时主动调用的回调接口
- 也是在`web.xml`配置
```xml
<listener>
    <listener-class>MyServletContextListener</listener-class>
</listener>
```

## Session

- 在`Servlet`内的`doGet`、`doPost`内可以通过`HttpServletRequest`获取到
- `req.getSession(false)`获取`session`，参数控制是否生成新的`session`,在`session`上可以设置和获取属性

## JDBC

- 通过`DriverManager.getConnection(
        requireEnv("TRAINING_DB_URL"),
        requireEnv("TRAINING_DB_USER"),
        requireEnv("TRAINING_DB_PASSWORD")
    )`获取到数据库的connect非常方便，参数在`tomcat`的环境变量中获取
- 通过`try-with-resource`实现`Connect`、`PreparedStatement`、`ResultSet`的自动关闭
- 通过`preparedStatement.setString`等方法可以便捷设置`SQL`语句中的参数，也可以避免`SQL`注入的问题。

## 事务

- 有些时候我们需要多次操作数据库，且需要要求多次操作同时成功，比如转账。
- JDBC 默认`autoCommit=true`，每条成功执行的更新语句会自动提交。
- 调用`connect.setAutoCommit(false)`后，后续更新属于当前事务；`commit()`确认并持久化当前事务中尚未提交的修改，`rollback()`撤销当前事务中尚未提交的修改。
- `commit()`不是让 SQL “生效”的唯一时刻：SQL 可能已经执行并影响当前事务可见的数据，`commit()`决定这些修改是否最终提交。

## 测试
- 开发者可以手写测试代码，也可以借助 AI 生成测试代码
- 当前练习重点是理解技术机制和手动写出最小实现，测试与 Maven 命令作为辅助能力，不需要为了记录数量而展开复杂测试矩阵

## 故障排查

- 通过`debugger`模式打断点，进行调试
- 在IDEA内打断点，请求接口，然后按需要可以一步一步执行查看，和前端在浏览器上的调试方式大同小异
