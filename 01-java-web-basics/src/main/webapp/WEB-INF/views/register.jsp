<%--
  Created by IntelliJ IDEA.
  User: siyesummer
  Date: 2026/8/17
  Time: 15:53
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>这册页</title>
</head>
<body>
<div>
    <form action="${pageContext.request.contextPath}/api/register" method="post">
        <div><label>用户名:</label> <input name="username" placeholder="请输入用户名" /></div>
        <div><label>密码:</label> <input name="password" placeholder="请输入密码" /></div>
        <button type="submit">注册</button>
    </form>
</div>
</body>
</html>
