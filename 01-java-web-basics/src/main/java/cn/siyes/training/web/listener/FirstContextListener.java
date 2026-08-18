package cn.siyes.training.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class FirstContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.out.println("javaweb容器创建");
    sce.getServletContext().setAttribute("season", "四叶");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    System.out.println("javaweb容器快要销毁了");
  }
}
