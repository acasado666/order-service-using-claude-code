package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
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
    private CustomerRepository customerRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_validRequest_savesAndReturnsResponse() {
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
    void getOrderById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));
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
    void cancelOrder_createdOrder_setsStatusCancelled() {
        UUID id = UUID.randomUUID();
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));

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
    void updateOrderStatus_createdToConfirmed_returnsUpdatedResponse() {
        UUID id = UUID.randomUUID();
        Order order = buildOrder(OrderStatus.CREATED, buildCustomer(UUID.randomUUID()));
        OrderResponse expected = buildResponse(order);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.updateOrderStatus(id, OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result).isEqualTo(expected);
    }

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
                "ORD-ABC12345",
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
