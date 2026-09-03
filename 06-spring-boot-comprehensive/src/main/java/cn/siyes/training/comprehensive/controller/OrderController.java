package cn.siyes.training.comprehensive.controller;

import cn.siyes.training.comprehensive.dto.ApiResponse;
import cn.siyes.training.comprehensive.dto.CreateOrderRequest;
import cn.siyes.training.comprehensive.model.Order;
import cn.siyes.training.comprehensive.model.OrderDetail;
import cn.siyes.training.comprehensive.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  public ApiResponse<Order> createOrder(
     @Valid @RequestBody CreateOrderRequest createOrderRequest) {

    final Order order = orderService.createOrder(createOrderRequest);

    return ApiResponse.ok(order, "创建订单成功");
  }

  @GetMapping("/{id}/detail")
  public ApiResponse<OrderDetail> createOrder(@PathVariable Long id) {

    final OrderDetail detailById = orderService.findDetailById(id);

    return ApiResponse.ok(detailById, "查询订单详情成功");
  }

  @PatchMapping("/{id}")
  public ApiResponse<OrderDetail> cancelOrder(@PathVariable Long id) {
    final OrderDetail orderDetail = orderService.cancelOrder(id);

    return ApiResponse.ok(orderDetail, "取消订单成功");
  }
}
