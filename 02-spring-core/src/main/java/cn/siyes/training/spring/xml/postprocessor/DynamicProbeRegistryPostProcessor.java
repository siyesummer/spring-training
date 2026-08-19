package cn.siyes.training.spring.xml.postprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

// 除了可以修改已有定义，还可以向注册表新增 BeanDefinition
public class DynamicProbeRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
    if (registry.containsBeanDefinition("dynamicProbe")) {
      return;
    }

    final RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(PostProcessorProbe.class);
    rootBeanDefinition.getPropertyValues().addPropertyValue("label", "DynamicProbeRegistryPostProcessor里注册beanDefinition");

    registry.registerBeanDefinition("dynamicProbe", rootBeanDefinition);
    System.out.println("dynamicProbe BeanDefinition已注册");
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    BeanDefinitionRegistryPostProcessor.super.postProcessBeanFactory(beanFactory);
  }
}
