package cn.siyes.training.comprehensive.service;

import cn.siyes.training.comprehensive.dto.CreateOrderRequest;
import cn.siyes.training.comprehensive.exception.InsufficientStockException;
import cn.siyes.training.comprehensive.exception.OrderException;
import cn.siyes.training.comprehensive.mapper.*;
import cn.siyes.training.comprehensive.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
  private final ProductMapper productMapper;
  private final InventoryMapper inventoryMapper;
  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
  private final OperationLogMapper operationLogMapper;
  private final OrderDetailMapper orderDetailMapper;

  public OrderService(ProductMapper productMapper, InventoryMapper inventoryMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper, OperationLogMapper operationLogMapper, OrderDetailMapper orderDetailMapper) {
    this.productMapper = productMapper;
    this.inventoryMapper = inventoryMapper;
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
    this.operationLogMapper = operationLogMapper;
    this.orderDetailMapper = orderDetailMapper;
  }

  @Transactional
  public Order createOrder(CreateOrderRequest createOrderRequest) {
    final Long buyerId = createOrderRequest.getBuyerId();
    final List<OrderItem> items = createOrderRequest.getItems();
    final BigDecimal[] totalAmount = {BigDecimal.ZERO};

    final ArrayList<OrderItem> orderItems = new ArrayList<>();

    items.forEach(item -> {
      final Product product = productMapper.findById(item.getProductId());
      System.out.println(product);
      if (product.getEnabled() != 1) {
        throw new OrderException("商品(" + item.getProductId() + ")已下架");
      }
      final Inventory inventory = inventoryMapper.findByProductId(item.getProductId());
      System.out.println(inventory);
      if (item.getQuantity() > inventory.getAvailableQuantity()) {
        throw new OrderException("商品(" + item.getProductId() + ")库存不足");
      }
//      扣减库存
      final int affectedRows = inventoryMapper.reduceInventory(item.getProductId(), item.getQuantity(), inventory.getVersion());
      if (affectedRows == 0) {
        throw new InsufficientStockException("库存不足或库存已发生变化");
      }

      BigDecimal itemAmount =
          product.getPrice()
              .multiply(BigDecimal.valueOf(item.getQuantity()));
      totalAmount[0] = totalAmount[0].add(itemAmount);

      final OrderItem orderItem = new OrderItem();
      orderItem.setProductId(item.getProductId());
      orderItem.setQuantity(item.getQuantity());
      orderItem.setUnitPrice(product.getPrice());
      orderItems.add(orderItem);
    });

    final Order order = new Order();
    order.setBuyerId(buyerId);
    order.setTotalAmount(totalAmount[0]);
    order.setStatus(OrderStatus.CREATED);
//    插入 orders
    final int insert = orderMapper.insert(order);
    if (insert != 1) {
      throw new OrderException("订单创建失败");
    }

    orderItems.forEach(orderItem -> {
//      给order_items设置订单ID
      orderItem.setOrderId(order.getId());
    });
//    批量插入 order_items
    final int i = orderItemMapper.batchInsert(orderItems);
    if (i != items.size()) {
      throw new OrderException("插入 order_items失败");
    }

//    插入 operation_logs
    final int logCount = operationLogMapper.insert(
        order.getId(),
        OrderStatus.CREATED,
        "创建订单的日志"
    );
    if (logCount != 1) {
      throw new OrderException("插入 operation_logs失败");
    }

    final Order orderRes = orderMapper.findById(order.getId());
    if (orderRes == null) {
      throw new OrderException("创建订单后查询order失败");
    }
    System.out.println(orderRes);


//    throw new OrderException("先让他不要生效");

    return orderRes;
  }

  @Transactional(readOnly = true)
  public OrderDetail findDetailById(Long orderId) {

    return orderDetailMapper.findDetailById(orderId);
  }

  @Transactional
  public OrderDetail cancelOrder(Long orderId) {
    final Order order = orderMapper.findById(orderId);

    if (order == null) {
      throw new OrderException("订单不存在");
    }

    if (!order.getStatus().equals(OrderStatus.CREATED)) {
      throw new OrderException("只允许 CREATED 状态取消");
    }

    final List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);

    orderItems.forEach(orderItem->{
      System.out.println(orderItem);
      final Inventory inventory = inventoryMapper.findByProductId(orderItem.getProductId());
//      恢复每个明细的库存
      final int count = inventoryMapper.increaseInventory(
          orderItem.getProductId(),
          orderItem.getQuantity(),
          inventory.getVersion()
      );
      if (count != 1) {
        throw new OrderException("库存恢复失败");
      }
    });

//    修改订单状态为 CANCELLED
    final int statusCount = orderMapper.updateStatusById(orderId, OrderStatus.CANCELLED, LocalDateTime.now());
    if (statusCount != 1) {
      throw new OrderException("修改订单状态为 CANCELLED失败");
    }

//写入取消操作日志
    final int logCount = operationLogMapper.insert(
        orderId,
        OrderStatus.CANCELLED,
        "订单取消"
    );
    if (logCount != 1) {
      throw new OrderException("写入取消操作日志失败");
    }

    return orderDetailMapper.findDetailById(orderId);
  }
}
