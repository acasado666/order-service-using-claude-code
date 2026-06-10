package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.PagedResponse;
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

    @Test
    void createOrder_validRequest_savesAndPublishesEvent() {
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
        verify(orderRepository).save(order);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void createOrder_unknownCustomer_throwsEntityNotFoundException() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(buildRequest(customerId)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    @Test
    void getOrderByNumber_existingOrder_returnsResponse() {
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findByOrderNumberWithItems(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expected);

        assertThat(orderService.getOrderByNumber(ORDER_NUMBER)).isEqualTo(expected);
    }

    @Test
    void getOrderByNumber_unknownOrder_throwsEntityNotFoundException() {
        when(orderRepository.findByOrderNumberWithItems(ORDER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber(ORDER_NUMBER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ORDER_NUMBER);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listOrders_noFilters_returnsAllOrders() {
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));
        OrderResponse orderResponse = buildResponse(order);
        var pageable = PageRequest.of(0, 20);

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        PagedResponse<OrderResponse> result = orderService.listOrders(null, null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void updateOrderStatus_validTransition_savesAndPublishesEvent() {
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.updateOrderStatus(ORDER_NUMBER, OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getClass().getSimpleName()).isEqualTo("OrderStatusChangedEvent");
    }

    @Test
    void cancelOrder_delegatesToUpdateOrderStatus() {
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any())).thenReturn(buildResponse(order));

        orderService.cancelOrder(ORDER_NUMBER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void cancelOrder_unknownOrder_throwsEntityNotFoundException() {
        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_NUMBER))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Customer buildCustomer(UUID id) {
        return Customer.builder().id(id).email("test@example.com").fullName("Test User").build();
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
