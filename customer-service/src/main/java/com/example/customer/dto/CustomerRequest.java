package com.example.customer.dto;

import com.example.customer.entity.Address;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
        @NotNull(message = "Customer firstname is required")
        @NotBlank
        String firstname,
        @NotNull(message = "Customer lastname is required")
        @NotBlank
        String lastname,
        @NotNull(message = "Customer Email is required")
        @Email(message = "Customer Email is not a valid email address")
        @NotBlank
        String email,
        Address address
) {

}
