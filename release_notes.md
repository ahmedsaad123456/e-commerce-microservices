# Token Propagation, Ownership & Monitoring

- Implemented JWT token propagation from Order Service to downstream services using Feign Request Interceptors.
- Added service-to-service authentication for Notification Service using Keycloak Client Credentials Flow.
- Implemented `ServiceTokenProvider` with token caching and automatic renewal before expiration.
- Integrated the service access token into Customer Service requests using a load-balanced RestClient.
- Secured customer data retrieval using service roles and authenticated user identity.
- Implemented ownership-based authorization to prevent customers from accessing other customers' orders and payments.
- Updated order creation to derive the customer identity from the authenticated JWT instead of trusting the request body.
- Added security test scenarios covering invalid tokens, expired tokens, role restrictions, direct service access, and resource ownership.
- Integrated Spring Boot Actuator and Micrometer to collect application and business metrics.
- Added custom order creation counters and Resilience4j circuit breaker metrics.
- Integrated Prometheus to scrape metrics from Gateway and microservice instances.
- Added Grafana with a provisioned Prometheus data source and an E-Commerce monitoring dashboard.

## Important Details

### 1. User Token Propagation
For synchronous service-to-service calls, the original user's Access Token is forwarded through Feign or RestClient. This allows downstream services to identify the original user and enforce ownership rules.

### 2. Client Credentials Flow
Notification Service uses its own Keycloak client credentials to obtain a service Access Token. `ServiceTokenProvider` caches the token and renews it before expiration.

### 3. Kafka Security
Access Tokens are never included in Kafka events. Events contain the user identifier (`customerId` or `userId`) instead of authentication credentials.

### 4. Ownership Authorization (BOLA "Broken Object Level Authorization")
The authenticated user's Keycloak `sub` is used to identify resource ownership. Customers can access only their own orders, while authorized admins can access all orders.

### 5. Security Testing
Validated authentication and authorization scenarios:
- `401`: Missing, invalid, or expired token.
- `403`: Authenticated user without the required role.
- `404`: Resource not found or not owned by the requesting customer.

### 6. Monitoring Architecture
- **Micrometer:** Collects application and business metrics.
- **Prometheus:** Periodically scrapes and stores metrics from service instances.
- **Grafana:** Visualizes metrics through dashboards.

### 7. Key Monitoring Metrics
- Total orders created.
- Orders per minute.
- p95 API response time.
- Resilience4j circuit breaker state.

### 8. Key Security Principles
- Forward user tokens only to trusted internal services.
- Use Client Credentials when a service acts without a human user.
- Never store Access Tokens in Kafka events or logs.
- Validate ownership on every endpoint that accesses user-specific resources.