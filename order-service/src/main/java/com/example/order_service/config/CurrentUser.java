package com.example.order_service.config;


import org.springframework.security.core.Authentication;

public record CurrentUser(String id, boolean admin) {

    public static CurrentUser from(Authentication auth) {
        // auth.getName() is the token's "sub" (Keycloak user id): the identity nobody can fake
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return new CurrentUser(auth.getName(), admin);
    }
}