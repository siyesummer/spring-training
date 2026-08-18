package cn.siyes.training.web.filter;

import jakarta.servlet.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ApiFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/json;charset=UTF-8");
    System.out.println("经过了过滤器" + "filter-call");
    chain.doFilter(request, response);
  }
}
