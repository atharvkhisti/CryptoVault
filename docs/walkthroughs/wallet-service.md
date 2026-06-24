# Wallet Service Walkthrough & System Guide

This guide details the design, rationale, APIs, security, database design, and interview talking points for the **Wallet Service** inside **CryptoVault**.

---

## 1. Architectural & Design Explanations

### Why `BigDecimal` is Used Instead of `double`
Floating-point numbers (`double` and `float`) represent decimals using binary fractions (IEEE 754 standard). This binary encoding cannot accurately represent simple base-10 decimals, resulting in rounding errors (e.g. `0.1 + 0.2` resolves to `0.30000000000000004`). 
For a financial ledger or cryptocurrency app handling currencies with high decimal divisions (e.g., Bitcoin's 8 decimal places or Ethereum's 18 decimal places), floating-point errors would result in missing funds and audit failures. `BigDecimal` allows arbitrary-precision calculations, guaranteeing exact mathematical correctness at the cost of slight performance overhead.

### Why Wallet Service Owns Wallet Data
Under the **Single Responsibility Principle** and microservices encapsulation patterns, each service must hold absolute ownership of its database tables. If the `auth-service` or `transaction-service` could write directly to the `wallets` table, it would bypass validation logic (such as checking for negative balances) and lead to database lock conflicts. Centralizing wallet logic in the `wallet-service` ensures data integrity, safe transactional access, and easy horizontal scaling of the ledger database.

### Why JWT User Context is Preferred Over Auth Service API Calls
If downstream services had to call the `auth-service` via REST/gRPC for every incoming request to verify user details, it would:
1. Double the latency of every request.
2. Introduce a single point of failure (if `auth-service` goes down, the entire application stops working).
3. Generate massive network traffic.
By packing user context (UUID, email, role) into a cryptographically signed JWT, downstream services can decode and validate user details locally in memory in $O(1)$ time with zero network overhead.

### How This Design Scales in a Real Banking Environment
- **Stateless Services:** Since all credentials come from JWTs and services are stateless, we can spin up multiple instances of `wallet-service` behind a load balancer without session stickiness.
- **Database Partitioning / Sharding:** Because we index wallets by `user_id` (a UUID), we can easily shard the PostgreSQL database horizontally based on `user_id` ranges, distributing read/write loads across separate database instances as the user base grows.
- **Transactional isolation:** Operations are wrapped in `@Transactional` ensuring rollback on failure, and using optimistic locking can prevent race conditions.

---

## 2. Walkthrough Specifications

### Domain Model
The core entity is `Wallet`, mapping an ownership pair `(userId, currency)` to a `balance`. The unique constraint on the pair ensures a user can never initialize multiple wallets for the same asset type.

### Database Design
Table `wallets` is owned by `wallet-service`. Columns include:
- `id` (UUID Primary Key)
- `user_id` (UUID Index)
- `currency` (VARCHAR)
- `balance` (NUMERIC(38,18) with a CHECK constraint `balance >= 0`)
- `created_at` & `updated_at` (TIMESTAMP via JPA Auditing)

### API Design
- `POST /api/wallets`: Create a wallet for a specific currency asset.
- `GET /api/wallets`: Retrieve all wallet balances owned by the caller.
- `POST /api/wallets/deposit`: Add funds.
- `POST /api/wallets/withdraw`: Deduct funds, ensuring balance does not go below zero.

### Security Considerations
- JWT validation is performed locally in `JwtUserInterceptorFilter` using a shared HMAC-SHA256 signature key.
- Ownership check: For every deposit/withdrawal, the service verifies that the target `wallet.getUserId()` matches the caller's authenticated `principal.getUserId()`.

### Testing Strategy
- Unit tests use **Mockito** to mock dependencies (`WalletRepository` and `WalletMapper`), testing service rules (creation duplicates, positive amount checks, overdraft rejections) in isolation.
- Target code coverage is >80% to satisfy production quality criteria.

---

## 3. Class-by-Class Documentation

### `WalletServiceApplication`
- **Purpose:** Entry point of the microservice.
- **Why It Exists:** Bootstraps Spring Boot.
- **Dependencies:** Standard Spring Boot starters.
- **How It Fits:** Configures the Spring IOC context.
- **Interview Talking Points:** Uses `@SpringBootApplication`, which bundles configuration, auto-configuration, and package scanning.

### `JpaAuditingConfig`
- **Purpose:** Enable JPA lifecycle audit.
- **Why It Exists:** Automates setting of audit timestamps.
- **Dependencies:** `@EnableJpaAuditing` annotation.
- **How It Fits:** Integrates with `@CreatedDate` and `@LastModifiedDate` on the entity layer.
- **Interview Talking Points:** Decouples creation/modification timestamps management from business services.

### `SecurityConfig`
- **Purpose:** Restricts route access parameters.
- **Why It Exists:** Enforces stateless API sessions.
- **Dependencies:** Spring Security dependencies.
- **How It Fits:** Integrates the custom JWT parsing filter.
- **Interview Talking Points:** Sets session creation policy to `STATELESS` to eliminate session-based state and CSRF issues.

### `JwtUserInterceptorFilter`
- **Purpose:** Standalone token parser filter.
- **Why It Exists:** Extracts security details from incoming requests locally.
- **Dependencies:** `io.jsonwebtoken` parsing library.
- **How It Fits:** Executes once per request and populates Spring's `SecurityContextHolder` with `JwtUserPrincipal`.
- **Interview Talking Points:** Demonstrates stateless JWT authorization context parsing with zero external network overhead.

### `Wallet`
- **Purpose:** Database model representation.
- **Why It Exists:** Holds mapping for SQL tables.
- **Dependencies:** JPA annotations.
- **How It Fits:** Persisted by JPA repositories.
- **Interview Talking Points:** Maps monetary balances using `BigDecimal` with precision/scale bounds, and uses UUID keys to prevent enumeration attacks.

### `WalletRepository`
- **Purpose:** Persistence layer lookup.
- **Why It Exists:** Implements database query methods.
- **Dependencies:** `JpaRepository`.
- **How It Fits:** Called by service layer to query wallets.
- **Interview Talking Points:** Leverages Spring Data JPA dynamic query generation (e.g. `existsByUserIdAndCurrency`).

### `WalletService`
- **Purpose:** Business rules orchestrator.
- **Why It Exists:** Performs deposits, withdrawals, and balance updates.
- **Dependencies:** `WalletRepository`, `WalletMapper`.
- **How It Fits:** Acts as the transactional boundary of the service.
- **Interview Talking Points:** Explains business validations (overdraft protections, positive checks) and transaction rollbacks (`@Transactional`).

### `WalletController`
- **Purpose:** Exposes API controllers.
- **Why It Exists:** Maps client calls to service layers.
- **Dependencies:** `WalletService`.
- **How It Fits:** Binds HTTP JSON payloads and validates them using `@Valid`.
- **Interview Talking Points:** Uses `@RestController` (combines controller and response mapping) and wraps return payloads in a generic `ApiResponse`.
