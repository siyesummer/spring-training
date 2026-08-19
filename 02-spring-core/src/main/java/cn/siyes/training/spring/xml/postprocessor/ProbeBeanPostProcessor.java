package cn.siyes.training.spring.xml.postprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;

//处理 Bean 实例
public class ProbeBeanPostProcessor implements BeanPostProcessor {
  @Nullable
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if ("postProcessorProbe".equals(beanName)) {
      PostProcessorProbe probe = (PostProcessorProbe) bean;
      probe.setLabel("postProcessBeforeInitialization");
    }
    return bean;
//    return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
  }

  @Nullable
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (!"postProcessorProbe".equals(beanName)) {
      return bean;
    }

    System.out.println("返回 ProbeViewWrapper");
    return new ProbeViewWrapper((ProbeView) bean);
//    return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
  }
}
