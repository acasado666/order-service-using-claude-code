package com.skmcore.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.exception.GlobalExceptionHandler;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createOrder_validRequest_returns201WithBody() throws Exception {
        CreateOrderRequest request = buildRequest(UUID.randomUUID());
        OrderResponse response = buildResponse();

        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.customerEmail").value(response.customerEmail()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createOrder_missingCustomerId_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                null,
                List.of(new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))),
                new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US")
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_existingOrderNumber_returns200() throws Exception {
        String orderNumber = "ORD-ABC12345";
        OrderResponse response = buildResponse();

        when(orderService.getOrderByNumber(orderNumber)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void getOrder_unknownOrderNumber_returns404() throws Exception {
        String orderNumber = "ORD-NOTFOUND";
        when(orderService.getOrderByNumber(orderNumber))
                .thenThrow(new EntityNotFoundException("Order", orderNumber));

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                .andExpect(status().isNotFound());
    }

    private CreateOrderRequest buildRequest(UUID customerId) {
        return new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))),
                new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US")
        );
    }

    private OrderResponse buildResponse() {
        return new OrderResponse(
                UUID.randomUUID(),
                "ORD-ABC12345",
                OrderStatus.CREATED,
                "test@example.com",
                "Test User",
                List.of(),
                new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"),
                new BigDecimal("19.98"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
