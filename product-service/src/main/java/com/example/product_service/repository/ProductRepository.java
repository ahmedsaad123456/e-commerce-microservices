package com.example.product_service.repository;

import com.example.product_service.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // load the category in the same query, otherwise mapping it later is an extra query per product (N+1)
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll();

    // Flushes pending changes before the query and clears the persistence context afterward to avoid stale entities.
    @Transactional   // works standalone (like in this test) and joins the caller's transaction when there is one
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Product p
               set p.availableQuantity = p.availableQuantity - :quantity
             where p.id = :id
               and p.availableQuantity >= :quantity
            """)
    int decreaseStock(@Param("id") Integer id, @Param("quantity") int quantity);

    @Query("select p.availableQuantity from Product p where p.id = :id")
    int findAvailableQuantity(@Param("id") Integer id);


    // Native SQL on purpose: it does not depend on the entity field names, and it is one atomic UPDATE
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE product SET available_quantity = available_quantity + :quantity WHERE id = :id",
            nativeQuery = true)
    int increaseStock(@Param("id") Integer id, @Param("quantity") int quantity);
}
