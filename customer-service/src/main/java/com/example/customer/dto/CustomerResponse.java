package com.example.customer.dto;


import com.example.customer.entity.Address;
import lombok.Builder;

@Builder
public record CustomerResponse(
        String id,
        String firstname,
        String lastname,
        String email,
        Address address
) {

}