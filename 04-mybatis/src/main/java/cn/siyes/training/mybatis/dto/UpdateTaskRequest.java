package cn.siyes.training.mybatis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateTaskRequest {
  @NotBlank(message = "任务标题不能为空")
  private String title;
  @Size(max = 500, message = "任务描述不能超过500个字符")
  private String description;
  private LocalDate dueDate;

  public UpdateTaskRequest() {
  }

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

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }
}
