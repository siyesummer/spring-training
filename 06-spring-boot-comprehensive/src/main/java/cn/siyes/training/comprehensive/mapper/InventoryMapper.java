package cn.siyes.training.comprehensive.mapper;

import cn.siyes.training.comprehensive.model.Inventory;
import org.apache.ibatis.annotations.Param;

public interface InventoryMapper {
  Inventory findByProductId(Long id);

  int reduceInventory(
      @Param("productId") Long productId,
      @Param("quantity") int quantity,
      @Param("version") int version
  );

  int increaseInventory(
      @Param("productId") Long productId,
      @Param("quantity") int quantity,
      @Param("version") int version
  );
}
