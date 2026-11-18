package com.example.order_service.repository;

import com.example.order_service.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<Order> findWithLinesById(Long id);


    Optional<Order> findByReference(UUID reference);

    boolean existsByReference(UUID reference);

    // Loads the lines in the same query, so mapping the response does not fire one query per order
    @EntityGraph(attributePaths = "lines")
    List<Order> findAllByCustomerId(String customerId);
}
