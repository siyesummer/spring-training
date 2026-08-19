package cn.siyes.training.spring.xml.lifecycle;

public class LifecycleProbe {

  public void init() {
    System.out.println("初始化");
  }

  public void destroy() {
    System.out.println("要销毁了");
  }
}
