package com.skmcore.orderservice.event;

import com.skmcore.orderservice.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        LocalDateTime timestamp
) {
    public static OrderStatusChangedEvent of(UUID orderId, String orderNumber,
                                             OrderStatus previousStatus, OrderStatus newStatus) {
        return new OrderStatusChangedEvent(orderId, orderNumber, previousStatus, newStatus, LocalDateTime.now());
    }
}
