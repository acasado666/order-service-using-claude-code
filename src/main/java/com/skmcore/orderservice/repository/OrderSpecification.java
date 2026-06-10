package com.skmcore.orderservice.repository;

import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> hasCustomerId(UUID customerId) {
        return (root, query, cb) ->
                customerId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }
}
