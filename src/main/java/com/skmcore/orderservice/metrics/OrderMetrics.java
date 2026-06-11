package com.skmcore.orderservice.metrics;

import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderMetrics {

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

    private final MeterRegistry registry;
    private final Timer creationTimer;

    public OrderMetrics(MeterRegistry registry, OrderRepository orderRepository) {
        this.registry = registry;

        this.creationTimer = Timer.builder("orders.creation.duration")
                .description("Time taken to create an order end-to-end")
                .register(registry);

        Gauge.builder("orders.active.count", orderRepository,
                        repo -> repo.countByStatusIn(ACTIVE_STATUSES))
                .description("Number of orders in a non-terminal status (CREATED, CONFIRMED, SHIPPED)")
                .register(registry);
    }

    public void recordOrderCreated(UUID customerId) {
        Counter.builder("orders.created.total")
                .tag("customer_id", customerId.toString())
                .description("Total orders created, tagged by customer")
                .register(registry)
                .increment();
    }

    public void recordStatusChanged(OrderStatus from, OrderStatus to) {
        Counter.builder("orders.status.changed.total")
                .tag("from_status", from.name())
                .tag("to_status", to.name())
                .description("Total order status transitions")
                .register(registry)
                .increment();
    }

    public Timer.Sample startCreationTimer() {
        return Timer.start(registry);
    }

    public void stopCreationTimer(Timer.Sample sample) {
        sample.stop(creationTimer);
    }
}
