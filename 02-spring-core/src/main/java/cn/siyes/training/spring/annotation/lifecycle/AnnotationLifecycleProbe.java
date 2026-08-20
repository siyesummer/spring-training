package cn.siyes.training.spring.annotation.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class AnnotationLifecycleProbe {
  @PostConstruct
  public void init() {
//    System.out.println("注解probe init");
  }

  @PreDestroy
  public void destroy() {
//    System.out.println("注解probe destroy");
  }
}
