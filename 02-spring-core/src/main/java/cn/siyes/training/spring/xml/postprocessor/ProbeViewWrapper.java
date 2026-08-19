package cn.siyes.training.spring.xml.postprocessor;

public class ProbeViewWrapper implements ProbeView {
  private final ProbeView delegate;

  public ProbeViewWrapper(ProbeView delegate) {
    this.delegate = delegate;
  }

  @Override
  public void print() {
    System.out.println("wrapper before");
    delegate.print();
    System.out.println("wrapper after");
  }

  @Override
  public String getLabel() {
    return delegate.getLabel();
  }
}
