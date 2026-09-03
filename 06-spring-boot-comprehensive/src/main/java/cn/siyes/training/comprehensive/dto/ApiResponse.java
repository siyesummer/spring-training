package cn.siyes.training.comprehensive.dto;

public class ApiResponse<T> {
  private Integer code;
  private String message;
  private T data;

  public ApiResponse() {
  }

  public ApiResponse(Integer code, T data) {
    this.code = code;
    this.data = data;
  }

  public ApiResponse(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public ApiResponse(Integer code, T data, String message) {
    this.code = code;
    this.data = data;
    this.message = message;
  }

  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(ResponseCode.OK, data, message);
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
