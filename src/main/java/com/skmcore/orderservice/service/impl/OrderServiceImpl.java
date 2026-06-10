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
        log.info("Order created: {}", saved.getOrderNumber());
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, UUID customerId, Pageable pageable) {
        Page<Order> page;
        if (status != null && customerId != null) {
            page = orderRepository.findByStatusAndCustomer_Id(status, customerId, pageable);
        } else if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        } else if (customerId != null) {
            page = orderRepository.findByCustomer_Id(customerId, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return page.map(orderMapper::toResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        log.info("Updating order {} to status {}", orderNumber, newStatus);
        Order order = findByOrderNumberOrThrow(orderNumber);
        order.transitionTo(newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public void cancelOrder(String orderNumber) {
        log.info("Cancelling order: {}", orderNumber);
        Order order = findByOrderNumberOrThrow(orderNumber);
        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private Order findByOrderNumberOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
    }
}
