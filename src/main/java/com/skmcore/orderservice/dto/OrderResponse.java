package com.skmcore.orderservice.dto;

import com.skmcore.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        ShippingAddressDto shippingAddress,
        List<OrderItemResponse> items,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
