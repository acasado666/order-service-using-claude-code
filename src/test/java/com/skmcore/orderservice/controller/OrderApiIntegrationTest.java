package com.skmcore.orderservice.controller;

import com.skmcore.orderservice.BaseIntegrationTest;
import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.UpdateOrderStatusRequest;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiIntegrationTest extends BaseIntegrationTest {

    // ── createOrder ──────────────────────────────────────────────────────────

    @Test
    void test_createOrder_returns201() throws Exception {
        Customer customer = createTestCustomer();

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest(customer.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("ORD-")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerEmail").value(customer.getEmail()))
                .andExpect(jsonPath("$.totalAmount").value(19.98));
    }

    @Test
    void test_createOrder_invalidRequest_returns400() throws Exception {
        Customer customer = createTestCustomer();

        // empty items list violates @NotEmpty
        CreateOrderRequest invalid = new CreateOrderRequest(
                customer.getId(),
                List.of(),
                new AddressRequest("1 Main St", "Springfield", "IL", "62701", "US")
        );

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("items"));
    }

    // ── getOrder ─────────────────────────────────────────────────────────────

    @Test
    void test_getOrder_returns200() throws Exception {
        Customer customer = createTestCustomer();
        String orderNumber = createOrder(customer.getId());

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerEmail").value(customer.getEmail()))
                .andExpect(jsonPath("$.customerName").value(customer.getFullName()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value("prod-1"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Springfield"))
                .andExpect(jsonPath("$.totalAmount").value(19.98));
    }

    @Test
    void test_getOrder_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderNumber}", "ORD-DOESNOTEXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void test_updateStatus_validTransition() throws Exception {
        Customer customer = createTestCustomer();
        String orderNumber = createOrder(customer.getId());

        mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void test_updateStatus_invalidTransition_returns409() throws Exception {
        Customer customer = createTestCustomer();
        String orderNumber = createOrder(customer.getId());

        // CREATED → DELIVERED is invalid (must go CREATED→CONFIRMED→SHIPPED→DELIVERED)
        mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("CREATED")));
    }

    // ── listOrders ───────────────────────────────────────────────────────────

    @Test
    void test_listOrders_withPagination() throws Exception {
        Customer customer = createTestCustomer();
        for (int i = 0; i < 15; i++) {
            createOrder(customer.getId());
        }

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String createOrder(UUID customerId) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest(customerId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("orderNumber").asText();
    }

    private CreateOrderRequest buildOrderRequest(UUID customerId) {
        return new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("9.99"))),
                new AddressRequest("1 Main St", "Springfield", "IL", "62701", "US")
        );
    }
}
