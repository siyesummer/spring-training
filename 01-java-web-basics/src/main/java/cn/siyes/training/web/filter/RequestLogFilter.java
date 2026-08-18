package cn.siyes.training.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RequestLogFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    String method = req.getMethod();
    String requestURI = req.getRequestURI();
    String queryString = req.getQueryString();
    long start = System.nanoTime();

    try {
      chain.doFilter(request, response);
    } finally {
      long end = System.nanoTime();
      long millis = (end -start) / 1000000;
      System.out.printf("%s %s query=%s status=%d cost=%dms%n",
          method, requestURI, queryString, resp.getStatus(), millis);
    }
  }
}
