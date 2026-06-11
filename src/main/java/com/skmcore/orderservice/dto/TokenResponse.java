package com.skmcore.orderservice.dto;

public record TokenResponse(
        String token,
        String tokenType,
        long expiresIn
) {}
