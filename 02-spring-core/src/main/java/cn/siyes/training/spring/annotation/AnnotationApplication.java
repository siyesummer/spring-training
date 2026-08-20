package cn.siyes.training.spring.annotation;

import cn.siyes.training.spring.annotation.config.AnnotationConfig;
import cn.siyes.training.spring.annotation.exception.TransferException;
import cn.siyes.training.spring.annotation.postprocessor.AnnotationProbeView;
import cn.siyes.training.spring.annotation.repository.AccountRepository;
import cn.siyes.training.spring.annotation.service.AccountService;
import cn.siyes.training.spring.annotation.service.proxy.ExternalInvocationService;
import cn.siyes.training.spring.annotation.service.proxy.SelfInvocationService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;

public class AnnotationApplication {
  public static void main(String[] args) throws TransferException {

    try(final AnnotationConfigApplicationContext annotationContext = new AnnotationConfigApplicationContext(AnnotationConfig.class);) {

//      final AccountService accountService = (AccountService)annotationContext.getBean(AccountService.class);
//
//      accountService.transfer(1 , 2, BigDecimal.valueOf(-100));

//      final AnnotationProbeView annotationProbe = annotationContext.getBean("annotationProbe",
//          AnnotationProbeView.class);
//      System.out.println("geBean 类型=" + annotationProbe.getClass().getName());
//      annotationProbe.print();
//
//      AnnotationProbeView first = annotationContext.getBean(
//          "annotationProbe", AnnotationProbeView.class
//      );
//      AnnotationProbeView second = annotationContext.getBean(
//          "annotationProbe", AnnotationProbeView.class
//      );
//      System.out.println(first == second);

//      final AnnotationProbeView annotationDynamicProbe =
//          annotationContext.getBean(
//              "annotationDynamicProbe",
//              AnnotationProbeView.class);
//
//      annotationDynamicProbe.print();

      final SelfInvocationService bean =
          annotationContext.getBean(SelfInvocationService.class);
      System.out.println("--- outer 调用 this.inner ---");
      bean.outer();

      bean.inner();

      final ExternalInvocationService external =
          annotationContext.getBean(ExternalInvocationService.class);
      System.out.println("--- 另一个 Service 调用 inner ---");
      external.callInner();
    }
  }
}
