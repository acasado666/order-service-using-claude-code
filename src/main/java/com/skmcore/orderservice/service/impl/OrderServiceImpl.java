package com.skmcore.orderservice.service.impl;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.event.OrderCreatedEvent;
import com.skmcore.orderservice.event.OrderStatusChangedEvent;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.mapper.OrderMapper;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.CustomerRepository;
import com.skmcore.orderservice.repository.OrderRepository;
import com.skmcore.orderservice.repository.OrderSpecification;
import com.skmcore.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            OrderMapper orderMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", request.customerId().toString()));

        Order order = orderMapper.toEntity(request);
        order.setCustomer(customer);
        order.getItems().forEach(item -> item.setOrder(order));
        order.recalculateTotalAmount();

        Order saved = orderRepository.save(order);
        log.info("Order created: {}", saved.getOrderNumber());

        eventPublisher.publishEvent(OrderCreatedEvent.of(
                saved.getId(), saved.getOrderNumber(),
                saved.getCustomer().getId(), saved.getTotalAmount()));
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public OrderResponse getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumberWithItems(orderNumber)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrders(UUID customerId, OrderStatus status, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.hasCustomerId(customerId))
                .and(OrderSpecification.hasStatus(status));

        Page<OrderResponse> page = orderRepository.findAll(spec, pageable).map(orderMapper::toResponse);
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        log.info("Updating order {} to status {}", orderNumber, newStatus);
        Order order = findByOrderNumberOrThrow(orderNumber);

        OrderStatus previousStatus = order.getStatus();
        order.transitionTo(newStatus);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(OrderStatusChangedEvent.of(
                saved.getId(), saved.getOrderNumber(), previousStatus, newStatus));
        return orderMapper.toResponse(saved);
    }

    @Override
    public void cancelOrder(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.CANCELLED);
    }

    private Order findByOrderNumberOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
    }
}
