# Keycloak Integration — Setup & Configuration

- Added Keycloak 26.8.2 to Docker Compose with persistent storage and secure environment variables.
- Created the `ecommerce` Realm and configured `customer`, `admin`, and `service` roles.
- Configured `ecommerce-web` as a public client using Authorization Code Flow with PKCE (S256).
- Configured `notification-service` as a confidential client using Client Credentials Flow.
- Created test users and assigned their corresponding roles.
- Enabled user registration and configured default customer roles.
- Configured Access Token expiration and Refresh Token rotation.
- Tested token generation and inspected important JWT claims.

## Important Details

### 1. Realm, Clients & Roles

- **Realm:** An isolated environment containing users, clients, and roles. We created `ecommerce` instead of using `master`.
- **Clients:** `ecommerce-web` represents the frontend, while `notification-service` represents a backend service.
- **Roles:** `customer`, `admin`, and `service` define the permissions assigned to users and services.

### 2. Authentication Flows

- **Authorization Code + PKCE:** Used for frontend user login. PKCE protects the authorization code from being used by an attacker.
- **Client Credentials:** Used for service-to-service authentication without a human user. The service authenticates using its Client ID and Secret.

### 3. Tokens & JWT Claims

- **Access Token:** Used to access protected APIs.
- **Refresh Token:** Used to obtain a new Access Token.
- **ID Token:** Contains information about the authenticated user.

Important JWT claims:
- `iss`: Token issuer; must match the expected Keycloak URL.
- `sub`: Unique user identifier; used to associate users with application data.
- `exp`: Token expiration time.
- `realm_access.roles`: User or service roles.
- `azp`: Client that requested the token.

### 4. Security & Docker Configuration

- `KC_HOSTNAME` defines Keycloak's public URL and token issuer.
- `keycloak_data` preserves Keycloak data across container restarts.
- Browser access uses `http://localhost:8180`, while Docker services use `http://keycloak:8080`.
- Spring Security will validate JWT signatures, issuer, and expiration using Keycloak's public keys (JWKS).
- Password Grant was used only for local testing; Authorization Code + PKCE is preferred for frontend applications.

### 5. User Authorization

We created `ahmed` and `mhmd` as customers and `admin1` as an admin.

The `sub` claim allows services to identify the authenticated user and enforce resource ownership. 
For example, a customer must not be able to access another customer's orders, even with a valid Access Token.

=================================================================================================================
# A comprehensive breakdown of three primary **API authentication** methods. Choosing the right one is critical for security and system performance:

### 1. API Keys

*   **What they are:** A simple, unique string of characters acting as a secret token. They function like a VIP pass for your API.
*   **How they work:** The client includes this key in the request, typically within the **request headers** for better security.
*   **When to use:**
    *   For **public APIs** (e.g., weather data, maps, stock market data).
    *   For **internal microservices** communication within a company.
    *   When you need to track usage or implement **rate limiting** without needing complex user identity verification.

### 2. JSON Web Tokens (JWT)

*   **What they are:** A compact, self-contained token that securely transmits information between parties. It consists of three parts: a header, a payload (user data/claims), and a signature.
*   **How they work:** The server generates a token after verifying user credentials; the client then stores this token and sends it in the **Authorization header** with every subsequent request. Because it's self-contained and signed, the server can verify it without checking a database every time, making it **stateless and scalable** .
*   **When to use:**
    *   For **web and mobile applications** where users need to remain logged in while navigating different sections.
    *   When you want to improve server performance by removing the need for session storage.

### 3. OAuth (OAuth 2.0)

*   **What they are:** An advanced authorization framework that enables **delegated access**. It allows users to grant third-party applications permission to access their resources (like their email or profile) without sharing their password.
*   **How they work:** It involves a multi-step flow where the user is redirected to an authorization server (e.g., Google or Facebook), approves access, and the application receives an **access token** to use for future requests.
*   **When to use:**
    *   When you want to implement **social logins** (e.g., "Sign in with Google").
    *   When your application needs to interact with external APIs (like GitHub or Twitter) on behalf of a user without handling their credentials directly.

**Summary of Selection:**

*   **API Keys:** Best for basic, non-sensitive machine-to-machine tasks.
*   **JWT:** Best for scalable user session management in apps.
*   **OAuth:** Best for secure, third-party delegated authorization.

## **OAuth2 & OpenID Connect — Summary**

### **1. What are OAuth2 and OpenID Connect?**

- **OAuth2 (Authorization):** A framework that allows an application or service to access protected resources on behalf of a user, based on specific permissions.
- **OpenID Connect (OIDC – Authentication):** An identity layer built on top of OAuth2. It allows the client application to verify who the logged-in user is.

**The difference:**

- OAuth2 answers: *What is this application allowed to access?*
- OIDC answers: *Who is the logged-in user?*

### **2. Main components**

| Component            | Responsibility                                                                   |
| -------------------- | -------------------------------------------------------------------------------- |
| Resource Owner       | The user who owns the data.                                                      |
| Client               | The application requesting access on behalf of the user.                         |
| Authorization Server | Authenticates users and issues tokens, e.g. Keycloak.                            |
| Resource Server      | The API that protects resources and validates access tokens, e.g. Order Service. |

### **3. OAuth2 flows**

- **Authorization Code Flow with PKCE(Proof Key for Code Exchange):** Used when a user logs in through a frontend application. The user authenticates with Keycloak, which returns an authorization code. PKCE protects the code exchange by requiring the original `code_verifier`, so stealing the authorization code alone is not enough.
- **Client Credentials Flow:** Used for service-to-service communication when there is no human user involved. For example, Notification Service needs to call Customer Service.

### **4. The three important tokens**

| Token         | Purpose                                                  | Where is it used?               |
| ------------- | -------------------------------------------------------- | ------------------------------- |
| Access Token  | Grants permission to access protected APIs.              | Sent to Order Service.          |
| Refresh Token | Obtains a new Access Token when the current one expires. | Sent to Keycloak.               |
| ID Token      | Contains information about the authenticated user.       | Used by the client application. |

**Important:** Don't confuse the tokens:

- Access Token → APIs.
- Refresh Token → Keycloak.
- ID Token → Client application.

### **5. Scopes and Roles**

- **Scopes:** Define what access or actions the client is requesting, such as `read:orders` or `write:orders`.
- **Roles:** Define the user's permissions or responsibilities, such as `ADMIN` or `CUSTOMER`.

### **6. What is Keycloak?**

**Keycloak** is an Identity and Access Management solution that acts as an Authorization Server.

It handles:

- User authentication and login.
- Token generation.
- Access control using roles and scopes.
- Token renewal using Refresh Tokens.

## **Quick revision — remember these points**

| Concept                   | Remember                                          |
| ------------------------- | ------------------------------------------------- |
| OAuth2                    | Authorization — granting access to resources.     |
| OIDC                      | Authentication — identifying the user.            |
| Keycloak                  | Authenticates users and issues tokens.            |
| Authorization Code + PKCE | Secure login flow for frontend applications.      |
| Client Credentials        | Service-to-service authentication without a user. |
| Access Token              | Used to call protected APIs.                      |
| Refresh Token             | Used to get a new Access Token from Keycloak.     |
| ID Token                  | Used by the client to know who logged in.         |
| Scopes                    | Define the requested permissions.                 |
| Roles                     | Define what a user is allowed to do.              |

**The most important idea:** OAuth2/OIDC separate authentication from authorization. 
Keycloak handles the identity and token issuance, while each microservice protects its APIs by validating the Access Token and checking the required permissions.


## OAuth2 = Authorization.

## OpenID Connect = Authentication above OAuth2.