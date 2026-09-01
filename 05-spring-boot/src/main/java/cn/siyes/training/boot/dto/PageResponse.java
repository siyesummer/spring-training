package cn.siyes.training.boot.dto;

public class PageResponse<T> {
  private Integer code;
  private String message;
  private PageData<T> data;

  public PageResponse() {
  }

  public PageResponse(Integer code, PageData<T> data, String message) {
    this.code = code;
    this.data = data;
    this.message = message;
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

  public PageData<T> getData() {
    return data;
  }

  public void setData(PageData<T> data) {
    this.data = data;
  }
}
