package cn.siyes.training.comprehensive.model;

//库存
public class Inventory {
  private Long productId;
  private Integer availableQuantity;
  private Integer version;

  public Inventory() {
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Integer getAvailableQuantity() {
    return availableQuantity;
  }

  public void setAvailableQuantity(Integer availableQuantity) {
    this.availableQuantity = availableQuantity;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "Inventory{" +
        "productId=" + productId +
        ", availableQuantity=" + availableQuantity +
        ", version=" + version +
        '}';
  }
}
