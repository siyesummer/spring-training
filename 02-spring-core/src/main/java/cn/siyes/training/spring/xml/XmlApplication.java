package cn.siyes.training.spring.xml;

import cn.siyes.training.spring.xml.postprocessor.PostProcessorProbe;
import cn.siyes.training.spring.xml.postprocessor.ProbeView;
import cn.siyes.training.spring.xml.service.AccountService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.math.BigDecimal;

public class XmlApplication {
  public static void main(String[] args) {
    try (final ClassPathXmlApplicationContext context =
             new ClassPathXmlApplicationContext("spring/xml/applicationContext.xml");) {
//      final AccountService accountService = (AccountService) context.getBean("accountService");
//
//      System.out.println(accountService.getClass());
//      accountService.sayHi();

//      accountService.transfer(1, 2 , BigDecimal.valueOf(100));


//    bean对象替换
      final ProbeView postProcessorProbe = context.getBean("postProcessorProbe", ProbeView.class);
      postProcessorProbe.print();
      System.out.println(postProcessorProbe.getClass().getName());

//      代码内注册BeanDefinition
      final ProbeView dynamicProbe = context.getBean("dynamicProbe", ProbeView.class);

      dynamicProbe.print();
      System.out.println(dynamicProbe.getClass().getName());

    }
  }
}
