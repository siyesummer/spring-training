package cn.siyes.training.mybatis.model;

import java.util.List;

public class TaskDetail extends Task {
  private List<TaskComment> comments;

  public TaskDetail() {
  }

  public List<TaskComment> getComments() {
    return comments;
  }

  public void setComments(List<TaskComment> comments) {
    this.comments = comments;
  }

  public String commentToString(List<TaskComment> comments) {
    final StringBuilder stringBuilder = new StringBuilder();
    comments.forEach(comment -> {
      stringBuilder.append(comment.toString()).append("\n");
    });

    return stringBuilder.toString();
  }

  @Override
  public String toString() {
    return "TaskDetail{" +
        "tasks=" + super.toString() +
        "\ncomments=" + commentToString(comments) +
        '}';
  }
}
