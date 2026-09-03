package cn.siyes.training.comprehensive.mapper;

import cn.siyes.training.comprehensive.model.Order;
import cn.siyes.training.comprehensive.model.OrderItem;

import java.util.List;

public interface OrderItemMapper {
  int batchInsert(List<OrderItem> orderItems);

  List<OrderItem> findByOrderId(Long orderId);
}
