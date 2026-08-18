package cn.siyes.training.web.listener;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

public class FirstServletRequestListener implements ServletRequestListener {
  @Override
  public void requestInitialized(ServletRequestEvent sre) {
    HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
    System.out.println("request创建" + request.getMethod() + request.getRequestURI());
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {
    HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
    System.out.println("request销毁" + request.getMethod() + request.getRequestURI());
  }
}
