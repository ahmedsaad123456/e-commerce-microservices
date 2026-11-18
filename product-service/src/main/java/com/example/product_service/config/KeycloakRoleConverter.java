package com.example.product_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KeycloakRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        // Extract the realm_access object from the JWT.
        Map<String, Object> realmAccess =
                jwt.getClaimAsMap("realm_access");

        // Return an empty list if the token has no realm roles.
        if (realmAccess == null ||
                !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }

        // Convert Keycloak roles into Spring Security authorities.
        return roles.stream()
                .map(Object::toString)
                .<GrantedAuthority>map(role ->
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.toUpperCase(Locale.ROOT)
                        )
                )
                .toList();
    }
}