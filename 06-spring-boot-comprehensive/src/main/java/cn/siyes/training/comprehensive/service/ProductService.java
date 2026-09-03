package cn.siyes.training.comprehensive.service;

import cn.siyes.training.comprehensive.dto.ProductPageRequest;
import cn.siyes.training.comprehensive.mapper.ProductMapper;
import cn.siyes.training.comprehensive.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
  private final ProductMapper productMapper;

  public ProductService(ProductMapper productMapper) {
    this.productMapper = productMapper;
  }

  public List<Product> pageQuery(ProductPageRequest productPageRequest) {
    int offset = (productPageRequest.getPage() - 1) * productPageRequest.getPageSize();

    return productMapper.findPage(
        productPageRequest.getName(),
        productPageRequest.getEnabled(),
        offset,
        productPageRequest.getPageSize()
    );
  }
}
