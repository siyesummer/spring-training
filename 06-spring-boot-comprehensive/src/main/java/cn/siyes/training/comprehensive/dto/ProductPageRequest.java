package cn.siyes.training.comprehensive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProductPageRequest {
  private String name;
  private Byte enabled;
  @NotNull(message = "页数不能为空")
  @Min(value = 1, message = "页数最小为1")
  private int page;
  @NotNull(message = "每页数目不能为空")
  @Min(value = 1, message = "每页数目最小为1")
  private int pageSize;

  public ProductPageRequest() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Byte getEnabled() {
    return enabled;
  }

  public void setEnabled(Byte enabled) {
    this.enabled = enabled;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }
}
