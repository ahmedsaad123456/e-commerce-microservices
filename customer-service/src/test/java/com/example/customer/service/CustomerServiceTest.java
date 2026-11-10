
package com.example.customer.service;

import com.example.customer.dto.CustomerRequest;
import com.example.customer.entity.Customer;
import com.example.customer.exceptions.ResourceNotFoundException;
import com.example.customer.mapper.CustomerMapper;
import com.example.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Enables Mockito annotations in this test class.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    // Mock the repository to avoid accessing the database.
    @Mock
    private CustomerRepository repository;

    // Mock the mapper to control the mapping behavior.
    @Mock
    private CustomerMapper mapper;

    // Inject the mocks into CustomerService.
    @InjectMocks
    private CustomerService service;

    // Reusable customer request for the test cases.
    private final CustomerRequest request =
            new CustomerRequest("Ahmed", "Ali", "ahmed@example.com", null);

    @Test
    void createCustomer_savesAndReturnsId() {
        // Create the customer entity before saving.
        var entity = Customer.builder().firstname("Ahmed").build();

        // Simulate the saved customer returned by the repository.
        var saved = Customer.builder().id("c1").build();

        // Define what the mapper should return.
        when(mapper.toCustomer(request)).thenReturn(entity);

        // Define what the repository should return after saving.
        when(repository.save(entity)).thenReturn(saved);

        // Call the service and get the created customer's ID.
        String id = service.createCustomer(request).id();

        // Check that the returned ID is correct.
        assertThat(id).isEqualTo("c1");

        // Verify that the customer was saved.
        verify(repository).save(entity);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        // Simulate a customer that does not exist.
        when(repository.findById("x")).thenReturn(Optional.empty());

        // Check that the service throws ResourceNotFoundException.
        assertThatThrownBy(() -> service.findById("x"))
                .isInstanceOf(ResourceNotFoundException.class);

        // No mapping should happen when the customer is not found.
        verifyNoInteractions(mapper);
    }

    @Test
    void updateCustomer_mergesAndSaves() {
        // Create an existing customer.
        var existing = Customer.builder().id("c1").build();

        // Return the existing customer when searching by ID.
        when(repository.findById("c1")).thenReturn(Optional.of(existing));

        // Call the update method.
        service.updateCustomer("c1", request);

        // Verify that the request data was merged into the existing customer.
        verify(mapper).mergeCustomer(existing, request);

        // Verify that the updated customer was saved.
        verify(repository).save(existing);
    }

    @Test
    void deleteCustomer_whenMissing_throwsAndDeletesNothing() {
        // Simulate a customer that does not exist.
        when(repository.existsById("x")).thenReturn(false);

        // Check that deleting a missing customer throws an exception.
        assertThatThrownBy(() -> service.deleteCustomer("x"))
                .isInstanceOf(ResourceNotFoundException.class);

        // Verify that no delete operation was performed.
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteCustomer_whenExists_deletes() {
        // Simulate an existing customer.
        when(repository.existsById("c1")).thenReturn(true);

        // Call the delete method.
        service.deleteCustomer("c1");

        // Verify that the customer was deleted.
        verify(repository).deleteById("c1");
    }
}
