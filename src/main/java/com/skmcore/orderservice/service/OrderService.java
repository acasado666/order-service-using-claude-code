package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.model.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderByNumber(String orderNumber);

    PagedResponse<OrderResponse> listOrders(UUID customerId, OrderStatus status, Pageable pageable);

    OrderResponse updateOrderStatus(String orderNumber, OrderStatus newStatus);

    void cancelOrder(String orderNumber);
}
