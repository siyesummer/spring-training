package cn.siyes.training.web.servlet;

import cn.siyes.training.web.exception.UsernameAlreadyExistsException;
import cn.siyes.training.web.service.RegisterService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RegisterServlet extends BaseServlet {
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String[] userData = getUserData(req, resp);
    if (userData[0] == null) return;
    String username = userData[0];
    String password = userData[1];
    RegisterService registerService = new RegisterService();

    try {
      registerService.register(username, password);
      writeResponse(resp, HttpServletResponse.SC_CREATED, "注册成功");
    } catch (UsernameAlreadyExistsException e) {
      writeResponse(resp, HttpServletResponse.SC_CONFLICT, "用户名已存在");
    } catch (Exception e) {
      writeResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未预期数据库错误");
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    System.out.println("来啦");
  }
}
