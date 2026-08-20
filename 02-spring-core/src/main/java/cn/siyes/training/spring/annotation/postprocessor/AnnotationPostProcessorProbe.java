package cn.siyes.training.spring.annotation.postprocessor;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component("annotationProbe")
public class AnnotationPostProcessorProbe
    implements AnnotationProbeView {
  private String label = "from components";

  public AnnotationPostProcessorProbe() {
    System.out.println("1. AnnotationProbe 构造器");
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @PostConstruct
  public void init() {
    System.out.println("3. @PostConstruct, label=" + label);
  }

  @Override
  public void print() {
    System.out.println("6. Probe.print, label=" + label);
  }
}
