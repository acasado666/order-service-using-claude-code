package com.skmcore.orderservice.mapper;

import com.skmcore.orderservice.dto.CreateOrderRequest;
import com.skmcore.orderservice.dto.OrderItemRequest;
import com.skmcore.orderservice.dto.OrderItemResponse;
import com.skmcore.orderservice.dto.OrderResponse;
import com.skmcore.orderservice.dto.ShippingAddressDto;
import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderItem;
import com.skmcore.orderservice.model.ShippingAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    @Mapping(target = "customerId", source = "customer.id")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    ShippingAddressDto toAddressDto(ShippingAddress address);

    ShippingAddress toAddress(ShippingAddressDto dto);
}
