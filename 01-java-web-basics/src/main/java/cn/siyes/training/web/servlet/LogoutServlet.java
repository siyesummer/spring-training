package cn.siyes.training.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class LogoutServlet extends BaseServlet {
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(false);

    if (session == null || session.getAttribute("USER_ID") ==null) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
      return;
    }

//    销毁session
    session.invalidate();

    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
