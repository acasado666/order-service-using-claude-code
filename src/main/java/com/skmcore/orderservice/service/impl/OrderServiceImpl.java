package com.skmcore.orderservice.service.impl;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.mapper.OrderMapper;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.CustomerRepository;
import com.skmcore.orderservice.repository.OrderRepository;
import com.skmcore.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderMapper = orderMapper;
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
        log.info("Order created with id: {}", saved.getId());
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public OrderResponse getOrderById(UUID id) {
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(orderMapper::toResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus) {
        log.info("Updating order {} to status {}", id, newStatus);
        Order order = findOrThrow(id);
        order.transitionTo(newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public void cancelOrder(UUID id) {
        log.info("Cancelling order: {}", id);
        Order order = findOrThrow(id);
        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id.toString()));
    }
}
