package cn.siyes.training.comprehensive.mapper;

import cn.siyes.training.comprehensive.model.Order;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface OrderMapper {
  int insert(Order order);

  Order findById(Long id);

  int updateStatusById(
      @Param("orderId") Long id,
      @Param("status") String status,
      @Param("cancelledAt")LocalDateTime cancelledAt);
}
