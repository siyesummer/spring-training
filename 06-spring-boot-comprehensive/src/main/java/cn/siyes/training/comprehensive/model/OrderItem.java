package cn.siyes.training.comprehensive.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OrderItem {
  private Long id;
  private Long orderId;
  @NotNull(message = "产品ID不能为空")
  private Long productId;
  @NotNull(message = "数量不能为空")
  @Min(value = 1, message = "数量必须大于 0")
  private Integer quantity;
  private BigDecimal unitPrice;

  public OrderItem() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  @Override
  public String toString() {
    return "OrderItem{" +
        "id=" + id +
        ", orderId=" + orderId +
        ", productId=" + productId +
        ", quantity=" + quantity +
        ", unitPrice=" + unitPrice +
        '}';
  }
}
