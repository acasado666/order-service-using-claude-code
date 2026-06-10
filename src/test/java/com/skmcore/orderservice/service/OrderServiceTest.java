package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.mapper.OrderMapper;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.repository.OrderRepository;
import com.skmcore.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_validRequest_savesAndReturnsResponse() {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))
        ));
        Order order = buildOrder(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());
        OrderResponse expected = buildResponse(order);

        when(orderMapper.toEntity(request)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isEqualTo(expected);
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        Order order = buildOrder(OrderStatus.PENDING);
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.getOrderById(id);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getOrderById_missingId_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void cancelOrder_pendingOrder_setsStatusCancelled() {
        UUID id = UUID.randomUUID();
        Order order = buildOrder(OrderStatus.PENDING);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.cancelOrder(id);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_missingId_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateOrderStatus_pendingToConfirmed_returnsUpdatedResponse() {
        UUID id = UUID.randomUUID();
        Order order = buildOrder(OrderStatus.PENDING);
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.updateOrderStatus(id, OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result).isEqualTo(expected);
    }

    private Order buildOrder(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .customerId("cust-1")
                .status(status)
                .totalAmount(new BigDecimal("19.98"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private OrderResponse buildResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                List.of(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
