package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.CreateCustomerRequest;
import com.skmcore.orderservice.dto.CustomerResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.dto.UpdateCustomerRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse getCustomerByEmail(String email);

    PagedResponse<CustomerResponse> getAllCustomers(Pageable pageable);

    CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request);

    void deleteCustomer(UUID id);
}
