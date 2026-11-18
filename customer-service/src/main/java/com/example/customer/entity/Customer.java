package com.example.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Document // For Mongodb
public class Customer {

    @Id
    private String id;
    private String firstname;
    private String lastname;
    // Creates a unique index to prevent duplicate email values in the database.
    @Indexed(unique = true)   // Mongo rejects a second document with the same email
    private String email;
    @Indexed(unique = true)
    private String keycloakUserId;
    private Address address;
}