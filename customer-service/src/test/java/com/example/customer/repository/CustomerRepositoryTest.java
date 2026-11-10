
package com.example.customer.repository;

import com.example.customer.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

// Starts the Spring context and disables external services during tests.
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",   // No Config Server in tests.
        "eureka.client.enabled=false",         // No Eureka in tests.
        "spring.data.mongodb.auto-index-creation=true" // Create MongoDB indexes automatically.
})
@Testcontainers
class CustomerRepositoryTest {

    // Starts a MongoDB container for repository tests.
    @Container
    @ServiceConnection // Automatically configures Spring to use this MongoDB container.
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    // Inject the real repository connected to the test database.
    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void cleanUp() {
        // Remove previous test data before each test.
        repository.deleteAll();
    }

    @Test
    void saveAndFindById() {
        // Save a new customer and get the saved entity.
        var saved = repository.save(customer("ahmed@example.com"));

        // Check that the customer can be found by ID with the correct email.
        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(Customer::getEmail)
                .isEqualTo("ahmed@example.com");
    }

    @Test
    void duplicateEmail_isRejectedByUniqueIndex() {
        // Save the first customer with this email.
        repository.save(customer("same@example.com"));

        // Check that MongoDB rejects another customer with the same email.
        assertThatThrownBy(() -> repository.save(customer("same@example.com")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    // Creates a customer with the given email for testing.
    private Customer customer(String email) {
        return Customer.builder()
                .firstname("Ahmed")
                .lastname("Ali")
                .email(email)
                .build();
    }
}
