package cn.siyes.training.web.servlet;

import cn.siyes.training.web.model.Message;
import cn.siyes.training.web.service.MessageService;
import cn.siyes.training.web.validation.MessageValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class MessageServlet extends BaseServlet{
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(false);
    long userId = (long) session.getAttribute("USER_ID");
    String content = req.getParameter("content");

    if (!MessageValidator.isValidContent(content)) {
      writeResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "信息为空或超过500个字符");
      return;
    }

    MessageService messageService = new MessageService();

    try {
      messageService.publish(userId, content);
      writeResponse(resp, HttpServletResponse.SC_CREATED, "发送成功");
    } catch (SQLException e) {
      writeResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未预期数据库错误");
    }

  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    MessageService messageService = new MessageService();

    try {
      ArrayList<Message> messageArrayList = messageService.finAll();

      writeMsgResponse(resp, HttpServletResponse.SC_OK, messageArrayList);
    } catch (SQLException e) {
      writeResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未预期数据库错误");
    }

  }

  public String createMessages(int code, ArrayList<Message> messages) {
    StringBuilder stringBuffer = new StringBuilder();

    for (Message msg : messages) {

      stringBuffer.append("发送人:").append(msg.getUserId()).append(" 信息:").append(msg.getContent()).append(";\n");
    }

    return """
        {
          "code": %d,
          "message": "%s"
        }
        """.formatted(code, stringBuffer);
  }

  public void writeMsgResponse(HttpServletResponse resp, int code, ArrayList<Message> messages) throws IOException {
    String result = createMessages(code, messages);
    resp.setStatus(code);
    resp.getWriter().print(result);
  }
}
