package cn.siyes.training.web.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {
  public String[] getUserData(HttpServletRequest req, HttpServletResponse resp) {
    String username = req.getParameter("username");
    String password = req.getParameter("password");
    String[] result = new String[2];
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      System.out.println("用户名为空");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      try {
        resp.getWriter().print(createResponse(HttpServletResponse.SC_BAD_REQUEST, "用户、密码不能为空"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return result;
    }

    result[0] = username;
    result[1] = password;
    return result;
  }

  public String createResponse(int code, String message) {
    return """
        {
          "code": %d,
          "message": "%s"
        }
        """.formatted(code, message);
  }

  public void writeResponse(HttpServletResponse resp, int code, String message) throws IOException {
    String result = createResponse(code, message);
    resp.setStatus(code);
    resp.getWriter().print(result);
  }
}
