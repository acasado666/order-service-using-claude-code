package com.skmcore.orderservice.dto;

import jakarta.validation.constraints.Email;

public record UpdateCustomerRequest(
        @Email String email,
        String fullName,
        String phone
) {
}
