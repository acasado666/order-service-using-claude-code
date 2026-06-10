package com.skmcore.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.model.OrderStatus;
import com.skmcore.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser
    void createOrder_validRequest_returns201WithBody() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))
        ));
        OrderResponse response = buildResponse();

        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void createOrder_missingCustomerId_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("", List.of(
                new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))
        ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getOrder_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        OrderResponse response = buildResponse();

        when(orderService.getOrderById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    @WithMockUser
    void getOrder_missingId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrderById(id))
                .thenThrow(new EntityNotFoundException("Order", id.toString()));

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_unauthenticated_returns401() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))
        ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private OrderResponse buildResponse() {
        return new OrderResponse(
                UUID.randomUUID(),
                "cust-1",
                OrderStatus.PENDING,
                new BigDecimal("19.98"),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
