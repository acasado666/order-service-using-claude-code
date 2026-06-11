package com.skmcore.orderservice.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static com.skmcore.orderservice.model.OrderStatus.CANCELLED;
import static com.skmcore.orderservice.model.OrderStatus.CONFIRMED;
import static com.skmcore.orderservice.model.OrderStatus.CREATED;
import static com.skmcore.orderservice.model.OrderStatus.DELIVERED;
import static com.skmcore.orderservice.model.OrderStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OrderStatusTransitionTest {

    // ── valid transitions ────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1} is allowed")
    @MethodSource("validTransitions")
    void validTransition_updatesStatus(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);

        order.transitionTo(to);

        assertThat(order.getStatus()).isEqualTo(to);
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                arguments(CREATED,   CONFIRMED),
                arguments(CREATED,   CANCELLED),
                arguments(CONFIRMED, SHIPPED),
                arguments(CONFIRMED, CANCELLED),
                arguments(SHIPPED,   DELIVERED)
        );
    }

    // ── invalid transitions ──────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1} throws IllegalStateException")
    @MethodSource("invalidTransitions")
    void invalidTransition_throwsIllegalStateException(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);

        assertThatThrownBy(() -> order.transitionTo(to))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                // CREATED skips
                arguments(CREATED,   SHIPPED),
                arguments(CREATED,   DELIVERED),
                // CONFIRMED skips / reverse
                arguments(CONFIRMED, CREATED),
                arguments(CONFIRMED, DELIVERED),
                // SHIPPED — only DELIVERED is valid
                arguments(SHIPPED,   CREATED),
                arguments(SHIPPED,   CONFIRMED),
                arguments(SHIPPED,   CANCELLED),
                // DELIVERED is terminal
                arguments(DELIVERED, CREATED),
                arguments(DELIVERED, CONFIRMED),
                arguments(DELIVERED, SHIPPED),
                arguments(DELIVERED, CANCELLED),
                // CANCELLED is terminal
                arguments(CANCELLED, CREATED),
                arguments(CANCELLED, CONFIRMED),
                arguments(CANCELLED, SHIPPED),
                arguments(CANCELLED, DELIVERED)
        );
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-TEST0001")
                .status(status)
                .customer(Customer.builder()
                        .id(UUID.randomUUID())
                        .email("test@example.com")
                        .fullName("Test User")
                        .build())
                .build();
    }
}
