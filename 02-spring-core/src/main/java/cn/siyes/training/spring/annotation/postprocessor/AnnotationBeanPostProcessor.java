package cn.siyes.training.spring.annotation.postprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
//处理 Bean 实例
public class AnnotationBeanPostProcessor
    implements BeanPostProcessor {
  @Nullable
  @Override
  public Object postProcessBeforeInitialization(
      Object bean,
      String beanName)
      throws BeansException {
    if (bean instanceof AnnotationProbeView) {
      System.out.println("观察到 Probe Bean: " + beanName);
    }
    if ("annotationProbe".equals(beanName)) {
      AnnotationPostProcessorProbe probe = (AnnotationPostProcessorProbe) bean;
      probe.setLabel("2. BeanPostProcessor.before");
      System.out.println("2. BeanPostProcessor.before");
    }
    return bean;
  }

  @Nullable
  @Override
  public Object postProcessAfterInitialization(
      Object bean,
      String beanName)
      throws BeansException {
    if (!"annotationProbe".equals(beanName)) {
      return bean;
    }

    System.out.println("4. BeanPostProcessor.after 返回包装对象");
    return new AnnotationProbeViewWrapper(
        (AnnotationProbeView) bean);
  }
}
