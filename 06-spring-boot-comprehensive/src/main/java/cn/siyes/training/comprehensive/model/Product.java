package cn.siyes.training.comprehensive.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
  private Long id;
  private String name;
  private BigDecimal price;
  private Byte enabled;
  private LocalDateTime createdAt;

  public Product() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public Byte getEnabled() {
    return enabled;
  }

  public void setEnabled(Byte enabled) {
    this.enabled = enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "Product{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", price=" + price +
        ", enabled=" + enabled +
        ", createdAt=" + createdAt +
        '}';
  }
}
