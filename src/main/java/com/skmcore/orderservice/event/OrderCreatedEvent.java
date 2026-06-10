package com.skmcore.orderservice.event;

import com.skmcore.orderservice.model.Order;

public record OrderCreatedEvent(Order order) {}
