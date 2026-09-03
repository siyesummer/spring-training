package cn.siyes.training.comprehensive.model;

import java.util.List;

public class OrderDetail extends Order {
  private List<Product> products;
  private List<OrderItem> orderItems;

  public OrderDetail() {
  }

  public List<Product> getProducts() {
    return products;
  }

  public void setProducts(List<Product> products) {
    this.products = products;
  }

  public List<OrderItem> getOrderItems() {
    return orderItems;
  }

  public void setOrderItems(List<OrderItem> orderItems) {
    this.orderItems = orderItems;
  }

  @Override
  public String toString() {
    return "OrderDetail{" +
        "products=" + products +
        ", orderItems=" + orderItems +
        '}';
  }
}
