# E-Commerce Microservices Backend

An e-commerce backend built as a set of Spring Boot microservices. It covers customers, products with stock control, orders, payments and email notifications, connected through REST and Kafka. Orders are completed by a choreography-based saga with compensation. Reliable messaging uses a transactional outbox, idempotent consumers and dead-letter handling. Services are discovered through Eureka, configured centrally, reached through an API Gateway behind an Nginx load balancer, secured with Keycloak (OAuth2 / OpenID Connect), and observed with distributed tracing, structured logs and Prometheus metrics. The whole system runs with Docker Compose.

## Tech stack

| Area | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.1.2 (Spring Framework 7), Spring Cloud 2025.1.4 |
| Service infrastructure | Spring Cloud Config Server, Eureka (service discovery), Spring Cloud Gateway, Spring Cloud LoadBalancer |
| Persistence | PostgreSQL (database per service), Spring Data JPA (Hibernate 7), Flyway migrations |
| Documents | MongoDB (Customer and Notification services) |
| Cache / rate limiting | Redis |
| Messaging | Apache Kafka 4.2 (KRaft), RabbitMQ 4 |
| Security | Keycloak 26, OAuth2 Resource Server, JWT (RS256 via JWKS), Spring Security |
| Resilience | Resilience4j 2.4.0 (Retry, Circuit Breaker) |
| Service calls | OpenFeign, RestClient |
| Observability | Micrometer Tracing, Zipkin, structured JSON logs, Actuator, Prometheus, Grafana |
| Edge | Nginx load balancer in front of two Gateway instances |
| Mapping / boilerplate | MapStruct, Lombok |
| API docs | springdoc-openapi (Swagger UI aggregated at the Gateway) |
| Testing | JUnit 5, Mockito, Testcontainers 2.0.5 |
| Infrastructure | Docker, Docker Compose, MailDev, Kafka UI, pgAdmin, Mongo Express, Redis Commander |

## Features

### System design
- Domain-driven decomposition into bounded contexts: Customer, Product, Order, Payment and Notification.
- Database per service: no shared tables and no cross-service joins.
- Written architecture diagram, requirements and architecture decision records for the main choices (mono repo, database per service, Kafka for async).
- Synchronous calls only where an answer is needed immediately; everything else is event-driven.
- Mono repo with one folder per service and a single `docker-compose.yml` that starts the entire system.

### Infrastructure
- Config Server with a `config-repo` holding one file per service plus a shared `application.yml`, with `dev` and `docker` profiles.
- Eureka service discovery with heartbeats and leases.
- Secrets are supplied through environment variables and a git-ignored `.env`, never stored in the config repo.
- Health checks and `depends_on: service_healthy` ordering in Compose; named volumes keep data across restarts.
- Startup order enforced: Config Server, then Eureka, then the services.

### API foundation (every service)
- Uniform error response (`timestamp`, `status`, `code`, `message`, `path`) through `@RestControllerAdvice`.
- Jakarta Bean Validation on all DTOs.
- Correlation id filter: reads or generates `X-Correlation-Id`, puts it in the logging MDC and returns it in the response header.
- Actuator liveness and readiness probes wired into the Compose health checks.
- DTOs at every boundary and MapStruct for mapping.

### Customer service (MongoDB)
- CRUD with validation and consistent exceptions.
- Address and contact data modelled as embedded documents.
- Unique index on email, with `409 Conflict` for duplicates.
- `/customers/me` creates or returns the profile of the authenticated user (id taken from the token `sub`).

### Product service (PostgreSQL, Redis)
- Products and categories with schema owned by Flyway migrations and a dedicated database user.
- Single-product reads cached in Redis with a TTL and evicted on update; available quantity is deliberately not cached because it changes on every purchase.
- Stock deduction with one atomic conditional `UPDATE ... WHERE available_quantity >= :q`, checking the affected row count, so overselling is impossible under concurrent buyers.
- Batch purchase in a single transaction: if one item lacks stock the whole purchase rolls back and the error names the failing product.
- Purchase idempotency through a unique `purchase_id` in a `stock_movements` table, so a retried request never deducts twice.
- Idempotent stock restoration when an order fails (compensating transaction), plus a safety-net sweeper for orders that never completed.

### Order service
- `Order` and `OrderLine` with Flyway migrations; order status lifecycle `PENDING` → `CONFIRMED` / `CANCELLED`.
- Create-order flow: validate the customer, purchase the products, persist the order, then continue asynchronously.
- Customer and Product calls implemented with both OpenFeign and `RestClient`, with explicit connect and read timeouts.
- Resilience4j `@Retry` (exponential back-off on transient errors only) and `@CircuitBreaker` with fallbacks; a clear `503` when the Customer service is down and no artificial fallback for stock.
- Client-side load balancing across several Product instances by service name.
- Ownership checks: a customer can only read their own orders and order lines; other customers' objects return `404` (protection against broken object-level authorization).
- `/orders/me` lists the caller's orders without loading lines one by one (entity graph).
- Bearer token relay to downstream services through a Feign `RequestInterceptor`.
- Circuit breaker state exposed through Actuator.

### Payment service
- Payment processing with PostgreSQL and a payment confirmation event.
- Idempotent per `orderId` through a unique constraint, even if the event arrives twice.
- Payment failure is reported as an event so other services can compensate.

### Notification service (MongoDB, Kafka, RabbitMQ)
- Kafka listener stores each notification in MongoDB and publishes a `SendEmailJob` to RabbitMQ instead of sending directly.
- RabbitMQ topology: direct exchange, work queue, retry queue with TTL, dead-letter exchange and a dead-letter queue.
- Email worker with manual acknowledgement: ack on success, retry with a delay on failure, and move to the DLQ after the maximum attempts (counted from `x-death`).
- Emails delivered to MailDev; customer details fetched from the Customer service through a load-balanced `RestClient`.
- Service-to-service authentication with the OAuth2 client credentials grant and a cached, early-renewed token.

### Event-driven architecture
- Kafka topics for order, payment and order-failed events; RabbitMQ used only for the email job queue, with the choice between the two documented (log with replay and consumer groups vs. task queue with routing and acknowledgements).
- Kafka concepts exercised in practice: partitions, offsets, keys, consumer groups, and at-least-once delivery.
- Idempotent consumers using a `processed_events` table with a unique event id, so duplicate messages have no second effect.
- Consumer error handling with `DefaultErrorHandler`, exponential back-off and `DeadLetterPublishingRecoverer` to per-service dead-letter topics.
- Transactional Outbox in the Order and Payment services: events are written to `outbox_events` in the same transaction as the business change, and a poller publishes them to Kafka or RabbitMQ and marks them published, which removes the dual-write problem.

### Saga and eventual consistency
- Choreography-based saga: each service reacts to events published by the others, with the orchestration alternative studied and compared.
- Successful payment confirms the order; a failed payment cancels it and triggers stock restoration.
- Compensating transactions are idempotent.
- Tested end to end for three scenarios: successful payment, failed payment, and the mail server being down (message retried, then dead-lettered).

### API Gateway
- Spring Cloud Gateway (WebFlux) with `lb://` routes resolved through Eureka for customers, products and orders.
- Global filter generating and propagating `X-Correlation-Id`; predicates and filters used for routing and request shaping.
- `RequestRateLimiter` backed by Redis (token bucket per client IP) returning `429`.
- Circuit breaker with a fallback controller when a downstream service is unavailable.
- CORS configured in one place, inside the Gateway security chain.
- Internal-only endpoints (such as stock purchase) denied at the edge.
- Swagger / OpenAPI documentation of all services aggregated in one UI.
- Two Order instances and two Gateway instances running side by side, verified through logs and traces.
- Nginx in front of the Gateways as the single entry point (round robin and least connections tested), illustrating edge versus in-cluster load balancing.
- A saved `.http` file covering the full end-to-end flow.

### Security (Keycloak, OAuth2, JWT)
- Keycloak realm with `customer`, `admin` and `service` roles, a public client using Authorization Code with PKCE for users, and a confidential client using Client Credentials for service-to-service calls.
- User registration and login handled by Keycloak; the Gateway and every service are OAuth2 Resource Servers that verify the RS256 signature through JWKS, so bypassing the Gateway does not bypass authentication.
- `JwtAuthenticationConverter` maps realm roles to Spring authorities; admin endpoints protected with `@PreAuthorize`.
- Custom JSON handlers for `401` and `403` responses.
- Only the user id travels in Kafka events, never the token.
- Verified behaviours: `401` without a token, `403` with a customer token on an admin endpoint, and rejection of expired tokens.

### Observability
- Distributed tracing with Micrometer Tracing and Zipkin; the W3C `traceparent` header carries the trace across HTTP and messaging, so one order can be followed from the Gateway to every service.
- Structured JSON logs containing trace id, correlation id and service name.
- Actuator and Micrometer metrics exposed at `/actuator/prometheus`, scraped by Prometheus and shown in Grafana (request count, p95 latency, circuit breaker state).

## Testing
- Unit tests for the service layer with JUnit 5 and Mockito.
- Repository integration tests on real databases with Testcontainers (PostgreSQL and MongoDB).
- Kafka integration test with Testcontainers: creating an order must publish the event to the topic.

## Skills
Programming Languages: Java

Backend & Frameworks: Spring Boot, Spring Cloud (Config, Eureka, Gateway, LoadBalancer, OpenFeign), Spring Data JPA, Hibernate, Spring Security, Bean Validation, MapStruct, Multithreading

Microservices & Architecture: Microservices Architecture, Domain-Driven Design (DDD), Database per Service, Saga Pattern (Choreography), Eventual Consistency, Event-Driven Architecture, Transactional Outbox, API Gateway, Service Discovery, Centralized Configuration, Client-side and Server-side Load Balancing

Messaging: Apache Kafka, RabbitMQ, Dead Letter Queues, Retry with Back-off, Idempotent Consumers

Resilience & Concurrency: Resilience4j (Retry, Circuit Breaker), Rate Limiting, Timeouts, Idempotency, Atomic Updates, Concurrency Control

Databases: PostgreSQL, MongoDB, Redis, Flyway migrations

Security & Authentication: Keycloak, OAuth2, OpenID Connect, JWT, Authorization Code with PKCE, Client Credentials, Role-Based Access Control (RBAC), CORS, Ownership Checks (OWASP BOLA)

Observability: Micrometer Tracing, Zipkin, Prometheus, Grafana, Structured Logging, Correlation IDs

DevOps & Tools: Docker, Docker Compose, Nginx, MailDev

API Design: RESTful API Design, Consistent Error Responses, OpenAPI / Swagger

Testing: JUnit 5, Mockito, Testcontainers
