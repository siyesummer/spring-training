package cn.siyes.training.comprehensive.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
  private Long id;
  private Long buyerId;
  private BigDecimal totalAmount;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime cancelledAt;

  public Order() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(Long buyerId) {
    this.buyerId = buyerId;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  @Override
  public String toString() {
    return "Order{" +
        "id=" + id +
        ", buyerId=" + buyerId +
        ", totalAmount=" + totalAmount +
        ", status='" + status + '\'' +
        ", createdAt=" + createdAt +
        ", cancelledAt=" + cancelledAt +
        '}';
  }
}
