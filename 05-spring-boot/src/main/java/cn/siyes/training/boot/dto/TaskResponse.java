package cn.siyes.training.boot.dto;

public class TaskResponse<T> {
  private Integer code;
  private String message;
  private T data;

  public TaskResponse() {
  }

  public TaskResponse(Integer code, T data) {
    this.code = code;
    this.data = data;
  }

  public TaskResponse(Integer code, T data, String message) {
    this.code = code;
    this.data = data;
    this.message = message;
  }

  public TaskResponse(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setData(T data) {
    this.data = data;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
