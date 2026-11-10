package com.example.customer.service;

import com.example.customer.common.Constants;
import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.exceptions.ResourceNotFoundException;
import com.example.customer.mapper.CustomerMapper;
import com.example.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public String createCustomer(CustomerRequest request) {
        var customer = repository.save(mapper.toCustomer(request));
        return customer.getId();
    }

    public void updateCustomer(String id, CustomerRequest request) {
        var customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.CUSTOMER, id));

        mapper.mergeCustomer(customer, request);

        repository.save(customer);
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