# Microservices Security & Role-Based Authorization

- Enabled Keycloak JWT validation in Customer, Product, Order, and Payment services.
- Configured Spring Security OAuth2 Resource Server using the Keycloak Issuer URI and JWKS endpoint.
- Implemented a custom `KeycloakRoleConverter` to map Keycloak realm roles into Spring Security authorities.
- Configured stateless authentication and centralized JSON error handling for `401 Unauthorized` and `403 Forbidden`.
- Enabled method-level security using `@EnableMethodSecurity` and `@PreAuthorize`.
- Restricted sensitive endpoints based on user roles and permissions.
- Introduced ownership-based authorization to prevent customers from accessing other customers' data.
- Tested direct service access, JWT validation, and role-based endpoint protection.

## Important Details

### 1. Defense in Depth
Every microservice validates JWTs independently instead of relying only on the API Gateway. 
This prevents unauthorized direct access to service endpoints.

### 2. KeycloakRoleConverter
Extracts roles from `realm_access.roles` and converts them into Spring Security authorities, 
such as `ROLE_CUSTOMER` and `ROLE_ADMIN`.

### 3. SecurityConfig
Configures JWT authentication, stateless sessions, public endpoints, 
and custom authentication and authorization error handlers.

### 4. Role-Based Authorization
`@PreAuthorize` protects endpoints based on roles:
- `hasRole('ADMIN')`: Requires the ADMIN role.
- `hasRole('CUSTOMER')`: Requires the CUSTOMER role.
- `hasAnyRole('ADMIN','SERVICE')`: Allows either role.

### 5. Ownership Authorization
Uses the authenticated user's Keycloak `sub` claim to identify the user and ensure customers can access only their own resources, 
while allowing authorized admins or services.

### 6. HTTP Security Responses
- `401`: Missing, invalid, or expired token.
- `403`: Validly authenticated user without the required permission.
