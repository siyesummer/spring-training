package cn.siyes.training.web.listener;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class FirstSessionListener implements HttpSessionListener {

  private int count;

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    count++;
    System.out.println("session创建了" + count);
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    count--;
    System.out.println("session销毁了" + count);
  }
}
