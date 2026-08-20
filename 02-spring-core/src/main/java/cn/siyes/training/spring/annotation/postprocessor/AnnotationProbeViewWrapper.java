package cn.siyes.training.spring.annotation.postprocessor;

public class AnnotationProbeViewWrapper
    implements AnnotationProbeView{
  private final AnnotationProbeView target;

  public AnnotationProbeViewWrapper(AnnotationProbeView target) {
    this.target = target;
  }

  @Override
  public void print() {
    System.out.println("5. Wrapper before");
    target.print();
  }
}
