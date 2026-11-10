package com.example.customer.service;

import com.example.customer.common.Constants;
import com.example.customer.common.ErrorCode;
import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.exceptions.AppException;
import com.example.customer.exceptions.ResourceNotFoundException;
import com.example.customer.mapper.CustomerMapper;
import com.example.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerResponse createCustomer(CustomerRequest request) {
        try {
            var customer = repository.save(mapper.toCustomer(request));
            return mapper.toCustomerResponse(customer);
        } catch (DuplicateKeyException e) {
            throw new AppException(
                    ErrorCode.CUSTOMER_EMAIL_ALREADY_EXISTS, request.email()
            );
        }

    }

    public CustomerResponse updateCustomer(String id, CustomerRequest request) {
        var customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.CUSTOMER, id));

        mapper.mergeCustomer(customer, request);

        try {
            repository.save(customer);
            return mapper.toCustomerResponse(customer);
        } catch (DuplicateKeyException e) {
            throw new AppException(
                    ErrorCode.CUSTOMER_EMAIL_ALREADY_EXISTS, request.email()
            );
        }
    }

    public List<CustomerResponse> findAllCustomers() {
        return repository.findAll()
                .stream()
                .map(mapper::toCustomerResponse)
                .toList();
    }

    public CustomerResponse findById(String id) {
        return repository.findById(id)
                .map(mapper::toCustomerResponse)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.CUSTOMER, id));
    }

    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    public void deleteCustomer(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(Constants.CUSTOMER, id);
        }

        repository.deleteById(id);
    }
}