package com.skmcore.orderservice.event;

import com.skmcore.orderservice.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderEvent(
        UUID orderId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        LocalDateTime occurredAt
) {
    public static OrderEvent of(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus) {
        return new OrderEvent(orderId, previousStatus, newStatus, LocalDateTime.now());
    }
}
