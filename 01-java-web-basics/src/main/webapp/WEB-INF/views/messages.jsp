<%--
  Created by IntelliJ IDEA.
  User: siyesummer
  Date: 2026/8/18
  Time: 16:19
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>发送信息</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/api/messages" method="post">
    <div><label>信息:</label> <input name="content" placeholder="请输入信息" /></div>
    <button type="submit">发送</button>
</form>
</body>
</html>
