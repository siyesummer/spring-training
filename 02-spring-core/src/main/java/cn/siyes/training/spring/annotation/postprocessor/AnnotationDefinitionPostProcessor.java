package cn.siyes.training.spring.annotation.postprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
// 处理已注册的 BeanDefinition
public class AnnotationDefinitionPostProcessor
    implements BeanFactoryPostProcessor {
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    final BeanDefinition postProcessorProbe = beanFactory.getBeanDefinition("annotationProbe");
    final MutablePropertyValues propertyValues = postProcessorProbe.getPropertyValues();
    propertyValues.
        addPropertyValue("label",
            "from-BeanFactoryPostProcessor");
    System.out.println(
        "0. BeanFactoryPostProcessor 修改 BeanDefinition");
  }
}
