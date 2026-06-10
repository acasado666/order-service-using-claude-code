package com.skmcore.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        BigDecimal totalAmount,
        LocalDateTime timestamp
) {
    public static OrderCreatedEvent of(UUID orderId, String orderNumber,
                                       UUID customerId, BigDecimal totalAmount) {
        return new OrderCreatedEvent(orderId, orderNumber, customerId, totalAmount, LocalDateTime.now());
    }
}
