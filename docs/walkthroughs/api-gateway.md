# API Gateway Walkthrough & Architecture Reference

This document provides a comprehensive technical walkthrough of the **API Gateway** microservice in **CryptoVault**, serving as an architectural blueprint and an interview prep reference.

---

## 1. Why the API Gateway Exists
The API Gateway serves as the single entry point (ingress portal) for all client traffic into the CryptoVault microservices environment.
- **Unified Entry Point:** Clients only communicate with one domain and port (`8080`), abstracting the complexity of port mappings and internal networking routes.
- **Stateless Perimeter Security:** Cryptographic token checks are centralized at the edge, blocking bad or expired requests before they consume computational power downstream.
- **Cross-Cutting Concerns:** Centralizes logging, CORS, rate limiting, and routing logic rather than duplicating them in every service.

---

## 2. Routing Strategy
The gateway is built using the **Spring Cloud Gateway Server MVC** framework (Servlet-based/WebMVC). Routing is configured programmatically via [GatewayConfig.java](file:///d:/CryptoVault/backend/api-gateway/src/main/java/com/cryptovault/gateway/config/GatewayConfig.java) using functional WebMvc routing beans.

Incoming requests are mapped using path-matching predicates:
- `/api/auth/**` → Auth Service (Port `8083`)
- `/api/wallets/**` → Wallet Service (Port `8081`)
- `/api/transactions/**` → Transaction Service (Port `8082`)

### Placeholders for Future Extensions:
- `/api/notifications/**` → Port `8084`
- `/api/risk/**` → Port `8085`
- `/api/audit/**` → Port `8086`
- `/api/kyc/**` → Port `8087`

Downstream targets are configured via `application.properties` variables (`services.<name>.url`), allowing easy overrides depending on the deployment environment (e.g., Docker, Local, AWS).

---

## 3. JWT Validation Lifecycle
The gateway validates every incoming request requiring authentication:
1. **Perimeter Interception:** Inbound requests pass through the custom [GatewayAuthenticationFilter.java](file:///d:/CryptoVault/backend/api-gateway/src/main/java/com/cryptovault/gateway/filter/GatewayAuthenticationFilter.java).
2. **Public Endpoint Exclusion:** Endpoint rules permit anonymous access to authentication routes like `POST /api/auth/register` and `POST /api/auth/login`.
3. **Cryptographic Validation:** The filter extracts the Bearer token from the `Authorization` header and validates its HMAC-SHA256 signature locally using the shared symmetric base64-encoded key:
   - Evaluates expiration (`exp` claim).
   - Validates signature matches the shared secret.
4. **Immediate Boundary Rejections:** If the token signature is invalid or expired, the Gateway immediately returns a structured `401 Unauthorized` response matching the platform's `ApiResponse<Void>` structure.

---

## 4. User Context Propagation
Once the token is validated, the Gateway acts as a **trust boundary converter**:
- It extracts the custom claims from the JWT: `userId`, `email`, and `role`.
- It decorates the servlet request wrapper to inject standard, trusted headers:
  - `X-USER-ID` = User's UUID
  - `X-USER-EMAIL` = User's Email
  - `X-USER-ROLE` = User's security role (e.g., `USER`, `ADMIN`)
- **Trust Boundary Enforcement:** The Gateway automatically strips any incoming client-submitted `X-USER-` headers to prevent header injection spoofing.
- Downstream microservices trust these headers implicitly, saving database lookups and eliminating decryption tasks.

---

## 5. Security Model
The gateway security configuration in [SecurityConfig.java](file:///d:/CryptoVault/backend/api-gateway/src/main/java/com/cryptovault/gateway/config/SecurityConfig.java) enforces stateless security:
- **Stateless Sessions:** Session creation is disabled (`SessionCreationPolicy.STATELESS`), meaning no cookies are stored or checked.
- **CSRF Disabled:** Since authentication is stateless and uses JWTs sent in headers rather than session cookies, CSRF protection is safely disabled.
- **CORS Handling:** Enforces global CORS mappings allowing client apps to query resources, exposing only safe headers (`Authorization`).
- **Standardized Exception EntryPoint:** If a request reaches the security filter without authentication headers, the gateway translates Spring Security's rejection into a standardized JSON response envelope.

---

## 6. Service Communication
- **Synchronous Forwarding:** The Gateway acts as an HTTP proxy, forwarding the client request payload and headers directly to internal microservices.
- **Downstream Context Retention:** By injecting the `X-USER-` context headers, downstream services can propagate identity downstream to other microservices via OpenFeign client interceptors.

---

## 7. Logging & Observability Strategy
The gateway implements a custom [LoggingFilter.java](file:///d:/CryptoVault/backend/api-gateway/src/main/java/com/cryptovault/gateway/filter/LoggingFilter.java):
- Logs every request with its method, path, response status code, execution duration (ms), and authenticated user email.
- The log formatting uses structured key-value pairs (e.g., `GATEWAY_METRIC | method=GET | path=/api/wallets/... | status=200 | duration_ms=15 | user=alice@email.com`).
- **Observability Readiness:** This structured format is ready for Logstash/Fluentd scrapers to harvest, store in Prometheus, and display in Grafana dashboard metrics.

---

## 8. Testing Strategy
Our testing architecture ensures >80% code coverage across the gateway layers:
1. [JwtServiceTest.java](file:///d:/CryptoVault/backend/api-gateway/src/test/java/com/cryptovault/gateway/security/JwtServiceTest.java): Uses JJWT to sign mock tokens and asserts that `JwtService` correctly extracts claims and handles expiration.
2. [GatewayAuthenticationFilterTest.java](file:///d:/CryptoVault/backend/api-gateway/src/test/java/com/cryptovault/gateway/filter/GatewayAuthenticationFilterTest.java): Uses Mock HttpServletRequests to verify that public endpoints are bypassed, invalid tokens are blocked with 401s, and valid tokens successfully inject custom headers.
3. [SecurityConfigTest.java](file:///d:/CryptoVault/backend/api-gateway/src/test/java/com/cryptovault/gateway/config/SecurityConfigTest.java): Bootstraps a lightweight MockMvc test to verify that URL endpoint rules block or allow traffic correctly.

---

## 9. Future Scalability
- **mTLS Network Isolation:** In production, downstreams should only accept traffic that passes through the gateway. We will enforce this via **mTLS** or cloud-level Security Groups.
- **Asymmetric Signature (RS256):** To remove the dependency of distributing a shared secret key across the microservices environment, we will transition to public-private key cryptography (JWKS). The gateway verifies with the public key, or downstream services call a Gateway JWKS endpoint.

---

## 10. Interview Talking Points
If asked to explain this API Gateway design in a technical review:
1. **Servlet-Based Gateway Server MVC vs. Reactive WebFlux:** "We chose Spring Cloud Gateway Server MVC (Servlet-based) because the platform's downstream services are built on standard Spring MVC. This avoids mixing reactive (WebFlux) and servlet (Spring MVC) paradigms on the classpath, simplifying standard thread-local security context propagation and eliminating reactive debugging complexity."
2. **Context Propagation Headers:** "We parse and validate the JWT *only once* at the gateway boundary. Once the signature matches, the gateway extracts user metadata and forwards it to downstreams as simple custom headers (X-USER-ID). This keeps internal services decoupled from security dependencies and dramatically speeds up downstream API processing times."
3. **Boundary Defenses Against Header Spoofing:** "Because downstream services trust the custom X-USER headers implicitly, we treat the gateway as a strict trust boundary. The gateway's authentication filter automatically clears or overrides any incoming client-submitted X-USER headers. In production, downstreams are also firewalled so that only calls from the gateway's IP range are processed."
