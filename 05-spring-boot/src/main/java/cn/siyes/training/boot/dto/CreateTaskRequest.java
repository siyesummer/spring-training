package cn.siyes.training.boot.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
// 必须有无参构造器和Setter Getter方法
// 参数转Java对象时，通过无参构造器创建器生成独享，再Setter属性
// 返回的Java对象反转为Json时则依賴Getter获取属性
public class CreateTaskRequest {
  @NotBlank(message = "任务标题不能为空哦")
  @Size(max = 20, message = "标题不能超过20字符")
  private String title;
  @Size(max = 500, message = "任务描述不能超过500个字符")
  private String description;
  @FutureOrPresent(message = "截止日期不能早于今天")
  private LocalDate dueDate;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }
}
