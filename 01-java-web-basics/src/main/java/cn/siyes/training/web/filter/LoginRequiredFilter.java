package cn.siyes.training.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class LoginRequiredFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("USER_ID") == null) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.getWriter().print(createResponse(HttpServletResponse.SC_UNAUTHORIZED, "无登录用户"));
      return;
    }
    chain.doFilter(request, response);
  }

  public String createResponse(int code, String message) {
    return """
        {
          "code": %d,
          "message": "%s"
        }
        """.formatted(code, message);
  }
}
