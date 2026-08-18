package cn.siyes.training.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class CurrentUserServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(false);

    if (session == null || session.getAttribute("USER_ID") ==null) {
      writeResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "无登录用户");
      return;
    }

    Object username = session.getAttribute("USERNAME");
    Object userId = session.getAttribute("USER_ID");

    writeResponse(resp, HttpServletResponse.SC_OK, "登录用户ID:" + userId + ",登录用户名称:" + username);
  }
}
