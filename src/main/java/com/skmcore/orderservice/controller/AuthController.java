package com.skmcore.orderservice.controller;

import com.skmcore.orderservice.dto.TokenRequest;
import com.skmcore.orderservice.dto.TokenResponse;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.repository.CustomerRepository;
import com.skmcore.orderservice.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Token issuance (demo — no password check)")
public class AuthController {

    private final CustomerRepository customerRepository;
    private final JwtService jwtService;

    public AuthController(CustomerRepository customerRepository, JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    @Operation(summary = "Issue a JWT for an existing customer email")
    public ResponseEntity<TokenResponse> issueToken(@RequestBody @Valid TokenRequest request) {
        Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("Customer", request.email()));

        String token = jwtService.generateToken(
                customer.getId().toString(),
                customer.getEmail(),
                List.of("ROLE_USER"));

        return ResponseEntity.ok(new TokenResponse(token, "Bearer", 86400));
    }
}
