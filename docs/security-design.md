# Security Design Document

This document outlines the security architecture, token designs, authentication/authorization flows, and threat modeling/mitigation details for **CryptoVault**.

## 1. Authentication & Authorization Flows

### Authentication (Token Issuance)
1. A client submits raw credentials (email and password) to `/api/auth/login`.
2. The `auth-service` retrieves user details from the database.
3. Passwords are verified using **BCrypt** check algorithms.
4. Upon successful validation, the service compiles user attributes (UUID, email, role) into a claims payload and signs a JWT token using **HMAC-SHA256** with a 256-bit base64-encoded secret key.
5. The JWT token is returned inside the `accessToken` parameter.

### Authorization (JWT Validation Lifecycle & Context Propagation)
1. For protected requests, the client passes the token in the header: `Authorization: Bearer <token>` to the API Gateway.
2. The **API Gateway** intercepts the request via `GatewayAuthenticationFilter`. It parses and validates the JWT signature and expiration locally.
3. If validation fails, the Gateway immediately terminates the request, returning a standard `401 Unauthorized` envelope.
4. If validation succeeds, the Gateway extracts key claims: `userId`, `email`, and `role`.
5. The Gateway mutates request headers to inject the user context:
   - `X-USER-ID` = user's UUID
   - `X-USER-EMAIL` = user's email
   - `X-USER-ROLE` = user's security role (e.g. `USER`, `ADMIN`)
6. Downstream microservices (such as **Wallet Service**, **Transaction Service**, **Notification Service**, and **Audit Service**) consume these headers directly to build their internal `SecurityContext` / `JwtUserPrincipal`. They trust these headers implicitly, entirely bypassing local JWT parsing and public-key/symmetric-key validation steps.
7. **Cross-Service Header Propagation:** When the **Transaction Service** calls the **Wallet Service** or triggers notifications/auditing via Feign/HTTP, its Feign request interceptor (or headers copy mechanism) propagates the `X-USER-ID`, `X-USER-EMAIL`, and `X-USER-ROLE` headers, maintaining authorization context across the virtual network.


### Gateway Trust Boundary
The API Gateway serves as the perimeter security shield. To prevent header injection attacks:
- The Gateway automatically strips or overwrites any inbound `X-USER-ID`, `X-USER-EMAIL`, and `X-USER-ROLE` headers sent by external clients.
- Downstream services accept incoming requests only if they originate from the Gateway's internal IP address, or trust the `X-USER-` headers exclusively because the Gateway is the single point of entry.

---

## 2. JWT Claim Schema Structure

```
Headers:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "alice@cryptovault.com",         // Subject (Email)
  "userId": "e303b719-7561-460d-83b6...", // Custom claim (UUID)
  "role": "USER",                         // Custom claim (User clearance)
  "iat": 1781985600,                      // Issued at (seconds)
  "exp": 1782072000                       // Expiration (seconds)
}
```

---

## 3. Fraud Risk Analysis & Mitigation Controls

To protect the platform against financial crime, AML (Anti-Money Laundering), and velocity exploits, a dedicated **Risk Service** evaluates transactions using a modular rule engine (Strategy Pattern).

### Risk Scoring Brackets & Action Policies
Aggregated risk scores (ranging from 0 to 100) are computed dynamically. The final score maps to a Risk Level and a status action:

| Score Bracket | Risk Level | Status Action | Security Action & Enforcement |
| :--- | :--- | :--- | :--- |
| **0 - 25** | `LOW` | `APPROVED` | Allowed to proceed without further checks. |
| **26 - 50** | `MEDIUM` | `APPROVED` | Allowed to proceed. Logged for security trend analysis. |
| **51 - 75** | `HIGH` | `FLAGGED` | Transaction proceeds but the user account and transaction are flagged for manual compliance review. |
| **76 - 100** | `CRITICAL` | `BLOCKED` | Transaction is immediately rejected. Source and destination accounts are blocked from performing further operations. |

### Rule Engine Velocity Rules
The risk evaluation engine processes the following rule strategies:
1. **High Amount Rule (`HighAmountRule`):** Scrutinizes transfer/withdrawal transactions over 10,000 USD/INR equivalent, identifying massive capital flight or anomalous transactions (adds 40 points).
2. **Frequent Transfer Rule (`FrequentTransferRule`):** Checks if a user attempts more than 5 transfers/withdrawals within a rolling 10-minute window, preventing bot velocity abuse (adds 30 points).
3. **Failed Transfer Rule (`FailedTransferRule`):** Analyzes if a user had more than 3 failed transactions in the last 24 hours, mitigating trial-and-error attacks or credentials abuse (adds 20 points).
4. **Rapid Withdrawal Rule (`RapidWithdrawalRule`):** Flags if a withdrawal is requested within 5 minutes of a deposit, suggesting money laundering or wash trading behavior (adds 40 points).

---

## 4. Threat Modeling & Mitigation Details

| Threat / Attack Vector | Severity | Mitigation Strategy |
| :--- | :--- | :--- |
| **Password Compromise / Leakage** | Critical | Passwords are never stored in plaintext. Hashing is performed using **BCrypt** (with a configurable work factor), preventing brute-force calculations if the database is leaked. |
| **User ID Enumeration** | High | All resource mappings (such as user accounts and wallets) utilize **UUIDv4** keys rather than standard incremental indices (`1`, `2`, `3`). This prevents attackers from guessing and harvesting profiles sequentially. |
| **Token Tampering** | Critical | Every JWT token is cryptographically signed using **HMAC-SHA256**. Any modifications made to headers or payloads by third parties will mismatch the signature validation and automatically be rejected by `JwtService` / `JwtUserInterceptorFilter`. |
| **Database Injection (SQLi)** | Critical | Database interaction is performed using **Hibernate / Spring Data JPA**, which automatically sanitizes variables and uses prepared statements for SQL parameters, neutralizing injection vectors. |
| **CSRF (Cross-Site Request Forgery)** | Medium | Session creation is completely **stateless** (`SessionCreationPolicy.STATELESS`). The server does not store session cookies. Instead, headers are read directly, neutralizing standard cookie-based CSRF vectors. |
| **Balance Overdraft Attacks** | Critical | Enforced inside [WalletService.java](file:///d:/CryptoVault/backend/wallet-service/src/main/java/com/cryptovault/wallet/service/WalletService.java). Validates that the withdrawal amount does not exceed the wallet balance, and balance columns use database-level check constraints to guarantee `balance >= 0`. |
| **Unauthorized Asset Interception / Theft** | Critical | Enforced in `TransactionService`. When initiating a transfer, withdrawal, or deposit, the service validates ownership by ensuring the `userId` claim inside the authenticated JWT principal matches the owner `userId` of the source wallet. |
| **Tokens Leaked via Downstream Calls** | Medium | Microservices communicate via an isolated internal virtual network. Token context is forwarded only through secure HTTP headers via Spring Cloud Feign `RequestInterceptor`, ensuring credentials are never exposed in log streams or public pathways. |
| **Notification Spoofing / Spamming** | High | The Notification Service is restricted inside the private internal network. The API Gateway automatically strips any incoming `X-USER-` headers from external clients, and `SecurityConfig` blocks unauthorized users from triggering arbitrary alerts. |
| **Audit Log Tampering / Deletion** | Critical | Enforced in `AuditService` and `AuditLog` JPA entity level. Log entries are strictly append-only. JPA lifecycle interceptors (`@PreUpdate` and `@PreRemove`) throw `UnsupportedOperationException` if updates or deletes are attempted. In production, the database user role is restricted to INSERT and SELECT operations on `audit_logs` table. |
| **High Velocity/Volume Fraud** | Critical | Mitigated by `risk-service`. Integrates velocity-based rule evaluations (Frequent Transfers, Failed Transactions, Rapid Withdrawals) and volume caps (High Amount) to flag or block fraudulent accounts instantly prior to ledger state modification. |
| **Path Traversal File Uploads** | High | Mitigated in `LocalStorageService`. When mapping files to subdirectories, path cleaning utilizing `Paths.get(fileName).getFileName()` is executed to resolve traversal attempts, stripping out relative references like `../`. Explicit checks reject filenames containing `..` with security exceptions. |
| **Storage Exhaustion (DoS)** | Medium | Configured at the Spring Web Gateway layer and inside `DocumentValidator`, restricting uploads to a maximum of 5MB per file. |
| **Malicious File Execution (RCE)** | Critical | Restricted in `DocumentValidator` via strict MIME type verification. Only `image/jpeg`, `image/png`, and `application/pdf` formats are accepted. Filename extensions are verified to match their respective content-types, preventing executable uploads. |
| **KYC Approval Bypass / Hijack** | High | Protected by `SecurityConfig` in `kyc-service`. Admin endpoints `/api/kyc/approve` and `/api/kyc/reject` are secured via `@PreAuthorize("hasRole('ADMIN')")`, rejecting non-admin requests at the authorization boundary. |

---

## 5. KYC & Document Upload Security Controls

To comply with banking regulations and prevent cluster vulnerabilities, the **KYC Service** implements a multi-tiered file verification pipeline:

### A. Size Restrictions
- Default upload capacity is restricted to **5MB** via Spring Boot properties:
  - `spring.servlet.multipart.max-file-size=5MB`
  - `spring.servlet.multipart.max-request-size=5MB`
- Any request exceeding this limit is terminated at Tomcat boundary, throwing `MaxUploadSizeExceededException`.

### B. Format & Content Validation
- Valid MIME types are strictly restricted to:
  - `image/jpeg` (extensions: `.jpg`, `.jpeg`)
  - `image/png` (extensions: `.png`)
  - `application/pdf` (extensions: `.pdf`)
- `DocumentValidator` verifies both the incoming `MultipartFile#getContentType()` and matches the extension of the filename against the allowed set. Any mismatch or invalid format throws a validation error.

### C. Path Traversal & Shell Mitigation
- Files are saved in folders isolated by the user's UUID (e.g. `<upload-dir>/<user-uuid>/`).
- Filenames are cleaned using standard `Paths.get(fileName).getFileName().toString()` logic.
- To prevent breakout directory traversal attempts, the system scans the raw filename for double-dot characters (`..`). If detected, the file is rejected immediately with a `SecurityException` to prevent hackers from writing to systemic directories (e.g., `/etc/` or Windows startup folders).
- Uploaded files are stored with randomly generated names (or structured namespaces) to prevent direct invocation and execute permissions are disabled at the directory level in production.

### D. Administrative Boundary Protection
- The KYC verification state is updated to `APPROVED` or `REJECTED` only via HTTP `POST` requests targeted to `/api/kyc/approve` or `/api/kyc/reject`.
- These operations are protected by role-based filters in Spring Security, allowing access exclusively to principals decorated with the `ROLE_ADMIN` role context.
- Audit trails track the admin user's identity who performed the verification review to preserve regulatory compliance accountability.

---

## 6. Swagger UI OpenAPI Security Integration

To enable developers and testing teams to securely query API endpoints via the Swagger UI, the platform integrates standard OpenAPI security schemes:

### A. Bearer Token Authorization Flow
1. **Security Scheme Configuration:** Every microservice defines a global OpenAPI security scheme named `BearerAuth` in its `OpenApiConfig.java`. This is configured as a `http` type scheme with a scheme type of `bearer` and bearer format `JWT`.
2. **Global Requirement Binding:** The security scheme is bound globally to all endpoints using a `SecurityRequirement` metadata reference. This registers an "Authorize" button in the Swagger UI interface.
3. **Execution Steps:**
   - The user visits the API Gateway Swagger dashboard: `http://localhost:8080/swagger-ui/index.html`.
   - The user registers and logs in using the `/api/auth/register` and `/api/auth/login` endpoints.
   - The login response returns an `accessToken` JWT string.
   - The user clicks the green "Authorize" button at the top-right of Swagger UI.
   - The user inputs the raw JWT access token string (the UI automatically appends the `Bearer ` prefix).
   - Subsequent calls from Swagger UI automatically pass the token inside the `Authorization: Bearer <token>` request header.
   - The Gateway intercepts the request, verifies the signature, propagates the user context headers (`X-USER-ID`, etc.), and routes the request securely to downstream microservices.

