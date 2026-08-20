package cn.siyes.training.spring.annotation.service.proxy;

import org.springframework.stereotype.Service;

@Service
public class ExternalInvocationService {
  private final SelfInvocationService target;

  public ExternalInvocationService(SelfInvocationService target) {
    this.target = target;
  }

  public void callInner() {
    target.inner();
  }
}
