package cn.siyes.training.mybatis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateCommentsRequest {
  @NotEmpty(message = "评论列表不能为空")
  private List<
          @NotBlank(message = "评论内容不能为空")
          @Size(max = 500, message = "评论不能超过500个字符")
          String
      > comments;

  public CreateCommentsRequest() {
  }

  public List<String> getComments() {
    return comments;
  }

  public void setComments(List<String> comments) {
    this.comments = comments;
  }
}
