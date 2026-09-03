package cn.siyes.training.comprehensive.mapper;

import cn.siyes.training.comprehensive.model.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {
  List<Product> findPage(
      @Param("name") String name,
      @Param("enabled") Byte enabled,
      @Param("offset") int offset,
      @Param("size") int size
  );

  Product findById(Long id);
}
