package cn.siyes.training.mvc.dto;

import cn.siyes.training.mvc.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {
  @NotNull(message = "任务状态不能为空哦")
  private TaskStatus status;

  public UpdateStatusRequest() {
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }
}
