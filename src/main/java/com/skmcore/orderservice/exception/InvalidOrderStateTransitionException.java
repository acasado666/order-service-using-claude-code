package com.skmcore.orderservice.exception;

import com.skmcore.orderservice.model.OrderStatus;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(OrderStatus current, OrderStatus target) {
        super("Cannot transition order from " + current + " to " + target);
    }
}
