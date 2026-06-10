package com.skmcore.orderservice.service.impl;

import com.skmcore.orderservice.dto.CreateCustomerRequest;
import com.skmcore.orderservice.dto.CustomerResponse;
import com.skmcore.orderservice.dto.PagedResponse;
import com.skmcore.orderservice.dto.UpdateCustomerRequest;
import com.skmcore.orderservice.exception.DuplicateResourceException;
import com.skmcore.orderservice.exception.EntityNotFoundException;
import com.skmcore.orderservice.mapper.CustomerMapper;
import com.skmcore.orderservice.model.Customer;
import com.skmcore.orderservice.repository.CustomerRepository;
import com.skmcore.orderservice.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.email());
        if (customerRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("Customer", "email", request.email());
        }
        Customer saved = customerRepository.save(customerMapper.toEntity(request));
        log.info("Customer created with id: {}", saved.getId());
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Customer", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Customer", email));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        Page<CustomerResponse> page = customerRepository.findAll(pageable).map(customerMapper::toResponse);
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
        log.info("Updating customer: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer", id.toString()));

        if (request.email() != null && !request.email().isBlank()) {
            customerRepository.findByEmail(request.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(conflict -> {
                        throw new DuplicateResourceException("Customer", "email", request.email());
                    });
            customer.setEmail(request.email());
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            customer.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    public void deleteCustomer(UUID id) {
        log.info("Deleting customer: {}", id);
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer", id.toString());
        }
        customerRepository.deleteById(id);
    }
}
