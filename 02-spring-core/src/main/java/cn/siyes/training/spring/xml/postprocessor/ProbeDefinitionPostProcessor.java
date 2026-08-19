package cn.siyes.training.spring.xml.postprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

// 处理已注册的 BeanDefinition
public class ProbeDefinitionPostProcessor implements BeanFactoryPostProcessor {
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    System.out.println("BeanFactoryPostProcessor执行");
    final BeanDefinition postProcessorProbe = beanFactory.getBeanDefinition("postProcessorProbe");
    final MutablePropertyValues propertyValues = postProcessorProbe.getPropertyValues();
//    propertyValues.addPropertyValue("season", "四叶");
    propertyValues.addPropertyValue("label", "BeanFactoryPostProcessor里修改");
    System.out.println("propertyValues="+propertyValues);


  }
}
