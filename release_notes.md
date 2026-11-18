# API Gateway Security & CORS Configuration

- Integrated Spring Security OAuth2 Resource Server into the API Gateway to validate Keycloak Access Tokens.
- Configured JWT validation using Keycloak's Issuer URI and JWKS endpoint.
- Implemented custom JSON error responses for `401 Unauthorized` and `403 Forbidden`.
- Configured public, protected, and restricted Gateway endpoints.
- Centralized CORS configuration in Spring Security at the API Gateway level.
- Removed duplicate CORS configurations from Gateway YAML and downstream services.
- Restricted external access to Actuator endpoints through Nginx.
- Integrated Bearer JWT authentication into Swagger UI.
- Tested authentication, endpoint protection, CORS preflight requests, and Authorization Code Flow with PKCE.

## Important Details

### 1. Gateway as a Resource Server

The Gateway validates incoming JWTs by checking their signature, expiration, and issuer using Keycloak's public keys (JWKS).

### 2. Authentication vs. Authorization

- `401`: Missing or invalid Access Token.
- `403`: Authenticated user is not allowed to perform the requested action.
- `authenticated()`: Requires a valid token.
- `permitAll()`: Allows access without authentication.
- `denyAll()`: Always rejects access.

### 3. CORS (Cross-Origin Resource Sharing)

CORS allows specific frontend origins to communicate with the API through the browser.

- `allowedOrigins`: Defines permitted frontend URLs.
- `allowedMethods`: Defines permitted HTTP methods.
- `allowedHeaders`: Defines permitted request headers.
- `exposedHeaders`: Allows frontend JavaScript to read specific response headers.
- `allowCredentials`: Controls browser credentials such as cookies.
- `maxAge`: Defines how long the browser can cache preflight results.

### 4. Preflight Requests

Before certain cross-origin requests, the browser sends an `OPTIONS` request to check whether the actual request is allowed.

CORS must be processed before authentication because the preflight request does not contain the Access Token.

### 5. Centralized CORS

CORS is configured only at the Gateway because it is the frontend's entry point. Configuring it again in downstream services may produce duplicate CORS headers and cause browser errors.

### 6. Important Security Consideration

Gateway authentication alone is not sufficient. Every microservice must independently validate JWTs and enforce the appropriate roles and resource ownership rules, especially when services communicate directly through Eureka.
