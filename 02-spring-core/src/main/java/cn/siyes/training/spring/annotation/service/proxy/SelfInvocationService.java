package cn.siyes.training.spring.annotation.service.proxy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SelfInvocationService {
  public void outer() {
    System.out.println("outer body");
    this.inner();
  }

  @Transactional
  public void inner() {

    System.out.println("inner body");
    System.out.println(
        TransactionSynchronizationManager
            .isActualTransactionActive()
    );
  }
}
