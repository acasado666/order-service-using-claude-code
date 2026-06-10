package com.skmcore.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressDto(
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        @NotBlank String country
) {
}
