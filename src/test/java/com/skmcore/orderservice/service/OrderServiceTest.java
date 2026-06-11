package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
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
import com.skmcore.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String ORDER_NUMBER = "ORD-ABC12345";

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ── createOrder ──────────────────────────────────────────────────────────

    @Test
    void createOrder_Success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId);
        CreateOrderRequest request = buildRequest(customerId);
        Order order = buildOrder(OrderStatus.CREATED, customer);
        order.setItems(new ArrayList<>());
        OrderResponse expected = buildResponse(order);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isEqualTo(expected);
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderMapper, times(1)).toEntity(request);
        verify(orderRepository, times(1)).save(order);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(order.getId());
        assertThat(captor.getValue().orderNumber()).isEqualTo(ORDER_NUMBER);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
    }

    @Test
    void createOrder_CustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(buildRequest(customerId)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(customerId.toString());

        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── updateOrderStatus ────────────────────────────────────────────────────

    @Test
    void updateOrderStatus_ValidTransition() {
        Customer customer = buildCustomer(UUID.randomUUID());
        Order order = buildOrder(OrderStatus.CREATED, customer);
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.updateOrderStatus(ORDER_NUMBER, OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result).isEqualTo(expected);
        verify(orderRepository, times(1)).findByOrderNumber(ORDER_NUMBER);
        verify(orderRepository, times(1)).save(order);

        ArgumentCaptor<OrderStatusChangedEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().previousStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(captor.getValue().newStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(captor.getValue().orderNumber()).isEqualTo(ORDER_NUMBER);
    }

    @Test
    void updateOrderStatus_InvalidTransition() {
        Customer customer = buildCustomer(UUID.randomUUID());
        Order order = buildOrder(OrderStatus.DELIVERED, customer);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_NUMBER, OrderStatus.CREATED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("CREATED");

        verify(orderRepository, times(1)).findByOrderNumber(ORDER_NUMBER);
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── cancelOrder ──────────────────────────────────────────────────────────

    @Test
    void cancelOrder_Success() {
        Customer customer = buildCustomer(UUID.randomUUID());
        Order order = buildOrder(OrderStatus.CREATED, customer);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(buildResponse(order));

        orderService.cancelOrder(ORDER_NUMBER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, times(1)).findByOrderNumber(ORDER_NUMBER);
        verify(orderRepository, times(1)).save(order);
        verify(eventPublisher, times(1)).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void cancelOrder_AlreadyDelivered() {
        Customer customer = buildCustomer(UUID.randomUUID());
        Order order = buildOrder(OrderStatus.DELIVERED, customer);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_NUMBER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("CANCELLED");

        verify(orderRepository, times(1)).findByOrderNumber(ORDER_NUMBER);
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── listOrders ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void listOrders_WithFilters() {
        UUID customerId = UUID.randomUUID();
        OrderStatus status = OrderStatus.CONFIRMED;
        Customer customer = buildCustomer(customerId);
        Order order = buildOrder(status, customer);
        OrderResponse orderResponse = buildResponse(order);
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        PagedResponse<OrderResponse> result = orderService.listOrders(customerId, status, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0)).isEqualTo(orderResponse);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    // ── getOrderByNumber ─────────────────────────────────────────────────────

    @Test
    void getOrderByNumber_existingOrder_returnsResponse() {
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findByOrderNumberWithItems(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expected);

        assertThat(orderService.getOrderByNumber(ORDER_NUMBER)).isEqualTo(expected);
        verify(orderRepository, times(1)).findByOrderNumberWithItems(ORDER_NUMBER);
    }

    @Test
    void getOrderByNumber_unknownOrder_throwsEntityNotFoundException() {
        when(orderRepository.findByOrderNumberWithItems(ORDER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber(ORDER_NUMBER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ORDER_NUMBER);

        verify(orderRepository, times(1)).findByOrderNumberWithItems(ORDER_NUMBER);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Customer buildCustomer(UUID id) {
        return Customer.builder()
                .id(id)
                .email("test@example.com")
                .fullName("Test User")
                .build();
    }

    private CreateOrderRequest buildRequest(UUID customerId) {
        return new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))),
                new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US")
        );
    }

    private Order buildOrder(OrderStatus status, Customer customer) {
        return Order.builder()
                .id(UUID.randomUUID())
                .orderNumber(ORDER_NUMBER)
                .status(status)
                .customer(customer)
                .totalAmount(new BigDecimal("19.98"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private OrderResponse buildResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCustomer().getEmail(),
                order.getCustomer().getFullName(),
                List.of(),
                new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
