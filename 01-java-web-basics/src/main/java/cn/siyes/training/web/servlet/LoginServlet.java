package cn.siyes.training.web.servlet;

import cn.siyes.training.web.exception.InvalidCredentialsException;
import cn.siyes.training.web.model.User;
import cn.siyes.training.web.service.LoginAndLogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class LoginServlet extends BaseServlet{
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String[] userData = getUserData(req, resp);
    if (userData[0] == null) return;
    String username = userData[0];
    String password = userData[1];

    LoginAndLogoutService loginAndLogoutService = new LoginAndLogoutService();

    try {
      User loginUser = loginAndLogoutService.getUser(username, password);
//      避免登录前后的 Session ID 被固定复用

      HttpSession session = req.getSession(false);
      if (session == null) {
        session = req.getSession(true);
      } else {
        req.changeSessionId();
      }

      session.setAttribute("USER_ID", loginUser.getId());
      session.setAttribute("USERNAME", loginUser.getUsername());
      writeResponse(resp, HttpServletResponse.SC_OK, "登录成功");
    } catch (InvalidCredentialsException e) {
      writeResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "用户名或密码错误");
    } catch (Exception e) {
      writeResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未预期数据库错误");
    }
  }
}
