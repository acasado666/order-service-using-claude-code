package com.skmcore.orderservice.event;

import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;

public record OrderStatusChangedEvent(Order order, OrderStatus previousStatus) {}
