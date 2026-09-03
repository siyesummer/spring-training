package cn.siyes.training.comprehensive.dto;

import cn.siyes.training.comprehensive.model.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {
  @NotNull(message = "购买者ID不能为空")
  private Long buyerId;
  @NotEmpty(message = "产品明细不能为空")
  @Valid
  private List<OrderItem> items;

  public CreateOrderRequest() {
  }

  public Long getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(Long buyerId) {
    this.buyerId = buyerId;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public void setItems(List<OrderItem> items) {
    this.items = items;
  }
}
