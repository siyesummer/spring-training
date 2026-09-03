package cn.siyes.training.comprehensive.controller;

import cn.siyes.training.comprehensive.dto.ApiResponse;
import cn.siyes.training.comprehensive.dto.ProductPageRequest;
import cn.siyes.training.comprehensive.model.Product;
import cn.siyes.training.comprehensive.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public ApiResponse<List<Product>> queryProducts(
      @Valid  @ModelAttribute  ProductPageRequest productPageRequest) {
    final List<Product> products = productService.pageQuery(productPageRequest);
    return ApiResponse.ok(products, "查询产品成功");
  }
}
