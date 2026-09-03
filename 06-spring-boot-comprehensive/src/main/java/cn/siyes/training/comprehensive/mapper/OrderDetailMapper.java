package cn.siyes.training.comprehensive.mapper;

import cn.siyes.training.comprehensive.model.OrderDetail;

public interface OrderDetailMapper {
  OrderDetail findDetailById(Long orderId);
}
