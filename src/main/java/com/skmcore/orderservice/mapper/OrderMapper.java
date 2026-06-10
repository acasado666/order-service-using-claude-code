package com.skmcore.orderservice.mapper;

import com.skmcore.orderservice.dto.AddressRequest;
import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderItemResponse;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderItem;
import com.skmcore.orderservice.model.ShippingAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    OrderItem toItemEntity(OrderItemRequest request);

    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "customerName", source = "customer.fullName")
    OrderResponse toResponse(Order order);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    OrderItemResponse toItemResponse(OrderItem item);

    ShippingAddress toShippingAddress(AddressRequest dto);

    AddressRequest toAddressRequest(ShippingAddress address);
}
