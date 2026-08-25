package cn.siyes.training.mybatis.dto;

import cn.siyes.training.mybatis.model.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class QueryPageRequest {
  private String keyword;
  private  TaskStatus status;
  @Min(value = 1, message = "page最小为1")
  private int page;

  private String sortBy;
  private String direction;

  @Min(value = 1, message = "每页size最少1")
  @Max(value = 50, message = "每页size最多50")
  private int size;

  public QueryPageRequest() {

  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }
}
