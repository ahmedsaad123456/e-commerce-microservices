package com.example.customer.repository;


import com.example.customer.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String > {

    Optional<Customer> findByKeycloakUserId(String keycloakUserId);
    Boolean existsByKeycloakUserId(String keycloakUserId);

}
