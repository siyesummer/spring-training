package cn.siyes.training.spring.annotation.postprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class AnnotationDynamicRegistryPostProcessor
  implements BeanDefinitionRegistryPostProcessor {
  @Override
  public void postProcessBeanDefinitionRegistry(
      BeanDefinitionRegistry registry)
      throws BeansException {

    if (registry.containsBeanDefinition(
        "annotationDynamicProbe")) {
      return;
    }

    final RootBeanDefinition rootBeanDefinition =
        new RootBeanDefinition(
            AnnotationPostProcessorProbe.class);

    rootBeanDefinition.getPropertyValues().add(
        "label",
        "from-registry-post-processor"
    );
//    注册bean
    registry.registerBeanDefinition(
        "annotationDynamicProbe",
        rootBeanDefinition
    );

    System.out.println(
        "RegistryPostProcessor 动态注册 BeanDefinition");

  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//    不修改 BeanDefinition
  }
}
