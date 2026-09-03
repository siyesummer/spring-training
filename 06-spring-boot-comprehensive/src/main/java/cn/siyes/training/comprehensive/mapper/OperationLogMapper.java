package cn.siyes.training.comprehensive.mapper;

import org.apache.ibatis.annotations.Param;

public interface OperationLogMapper {
  int insert(
      @Param("orderId") Long orderId,
      @Param("action") String action,
      @Param("detail") String detail
  );
}
