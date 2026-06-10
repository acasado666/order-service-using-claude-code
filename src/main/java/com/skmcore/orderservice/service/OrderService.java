package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    Page<OrderResponse> getOrders(OrderStatus status, UUID customerId, Pageable pageable);

    OrderResponse updateOrderStatus(String orderNumber, OrderStatus newStatus);

    void cancelOrder(String orderNumber);
}
