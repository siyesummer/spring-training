<%--
  Created by IntelliJ IDEA.
  User: siyesummer
  Date: 2026/8/17
  Time: 23:29
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>登录页</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/api/login" method="post">
    <div><label>用户名:</label> <input name="username" placeholder="请输入用户名" /></div>
    <div><label>密码:</label> <input name="password" type="password" placeholder="请输入密码" /></div>
    <button type="submit">登录</button>
</form>
</body>
</html>
