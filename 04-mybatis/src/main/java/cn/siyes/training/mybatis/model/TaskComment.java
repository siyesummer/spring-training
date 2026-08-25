package cn.siyes.training.mybatis.model;

import java.time.LocalDateTime;

public class TaskComment {
  private Long id;
  private Long taskId;
  private String content;
  private LocalDateTime createdAt;

  public TaskComment() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "TaskComment{" +
        "id=" + id +
        ", taskId=" + taskId +
        ", content='" + content + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}
