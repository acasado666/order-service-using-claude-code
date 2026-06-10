package com.skmcore.orderservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OrderEventListener {

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("OrderCreatedEvent: orderId={} orderNumber={} customerId={} totalAmount={} timestamp={}",
                event.orderId(), event.orderNumber(), event.customerId(),
                event.totalAmount(), event.timestamp());

        // TODO: send order confirmation email to customer
        // TODO: notify inventory service to reserve items
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("OrderStatusChangedEvent: orderId={} orderNumber={} transition={}->{} timestamp={}",
                event.orderId(), event.orderNumber(),
                event.previousStatus(), event.newStatus(), event.timestamp());

        // TODO: push notification to customer about their order status update
    }
}
