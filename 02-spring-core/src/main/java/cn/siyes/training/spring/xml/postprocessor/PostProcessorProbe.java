package cn.siyes.training.spring.xml.postprocessor;

public class PostProcessorProbe implements ProbeView {
  private String label;
  public String season;

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    System.out.println("调用了setLabel="+ label);
    this.label = label;
  }

  public void init() {
    System.out.println("init初始化label=" + label);
  }

  public void print() {
    System.out.println("输出label="+label);
  }

  public String getSeason() {
    return season;
  }

  public void setSeason(String season) {
    System.out.println("调用了setSeason");
    this.season = season;
  }
}
