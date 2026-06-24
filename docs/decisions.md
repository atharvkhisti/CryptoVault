# Architectural Decision Records (ADR)

This document maps major architectural design choices made for **CryptoVault**.

---

## ADR 01: JWT User Context Instead Of Auth Service Calls

### Context
Downstream services (e.g. `wallet-service`, `transaction-service`) must know the caller's identity (user ID, email, role) to authorize requests and associate entities correctly.

### Alternatives Considered
1. **Direct Auth Service Verification:** Call `auth-service` via HTTP REST/gRPC on every downstream incoming request.
2. **Stateless JWT Claims Propagation:** Pack user identity directly into cryptographically signed JWT tokens and let downstream services decode them locally.

### Chosen Solution
**Stateless JWT Claims Propagation**

### Pros
- **High Scalability & Low Latency:** Eliminates the need for downstream services to perform an extra HTTP request to `auth-service` for every incoming request.
- **Service Independence:** Downstream services can authenticate and authorize incoming calls independently without relying on the availability of `auth-service`.

### Cons
- **Token Revocation Complexity:** If a user is deleted or banned, active tokens remain valid until they naturally expire, unless a centralized redis-based blacklist is implemented.

### Future Improvements
- Implement a token blacklist cache using Redis to reject revoked tokens immediately.

---

## ADR 02: UUID Instead of Auto-Incrementing Integer Primary Keys

### Context
We must model entity primary keys for database tables across services (e.g. `users` and `wallets` tables).

### Alternatives Considered
1. **Incremental Integers (`Long`):** Standard auto-increment columns (1, 2, 3...).
2. **Universally Unique Identifier (`UUID`):** Generate a randomized 128-bit UUID for each record.

### Chosen Solution
**UUID Primary Keys**

### Pros
- **ID Enumeration Prevention:** Attackers cannot guess or crawl resource details sequentially (prevents insecure direct object reference - IDOR vulnerabilities).
- **Service Independence:** UUIDs can be generated locally in code without requiring database roundtrips to obtain next sequence index numbers, which supports distributed databases.

### Cons
- **Larger Storage Overhead:** UUIDs take up 16 bytes (compared to 8 bytes for `Long`) and are harder to read in raw databases.

---

## ADR 03: Creation of Shared common-lib Module

### Context
Different microservices must agree on core models (like enums, request response envelopes, shared exceptions) to interact successfully.

### Alternatives Considered
1. **Code Duplication:** Manually copy-paste the same Java classes (Enums, Exception, ApiResponse) across every microservice repository.
2. **Shared Maven Library (`common-lib`):** Package shared enums, exceptions, and DTOs in a single library and import it via Maven.

### Chosen Solution
**Shared Maven Library (`common-lib`)**

### Pros
- **DRY (Don't Repeat Yourself) Principle:** Changes to error codes, API wrappers, or enums are modified in one place and propagated globally.
- **Consistent API Design:** Enforces matching JSON API response wrappers across all microservices.

### Cons
- **Coupling & Dependency Hell:** Changes in `common-lib` require recompiling and redeploying all microservices referencing it.

---

## ADR 04: Use BigDecimal For Wallet Balances and Asset Ledger Metrics

### Context
We must model wallet currency balances inside the database and services.

### Alternatives Considered
1. **Floating-Point Types (`double` or `float`):** Use built-in IEEE 754 floating-point metrics.
2. **Arbitrary-Precision Decimal (`BigDecimal`):** Use Java's arbitrary-precision numbers.

### Chosen Solution
**BigDecimal**

### Pros
- **Precision Integrity:** Floating-point numbers use binary representation, which leads to precision loss when representing decimals (e.g., `0.1 + 0.2` becomes `0.30000000000000004`). For financial systems, this rounding error is unacceptable.
- **Support for Cryptocurrencies:** Highly divisible coins (such as BTC with 8 decimals or ETH with 18 decimals) require massive precision scales that double types cannot support.

### Cons
- **Arithmetic Complexity:** Requires using method calls (like `.add()`, `.subtract()`, and `.compareTo()`) instead of standard math operators (`+`, `-`, `<`), slowing down computation slightly.

---

## ADR 05: Transaction Service Does Not Own Wallet Balances

### Context
We must decide how to manage wallet balances and transaction records. Specifically, should the Transaction Service modify the Wallet Service database directly or coordinate modifications via API calls?

### Alternatives Considered
1. **Direct Database Modification:** Share the PostgreSQL database or tables, allowing the Transaction Service to query and update the `wallets` table directly.
2. **REST API Coordination via Feign:** The Wallet Service owns the `wallets` table, and the Transaction Service requests balance changes (debit/credit) using synchronous REST API endpoints.

### Chosen Solution
**REST API Coordination via Feign**

### Pros
- **Single Source of Truth:** Balance calculations and constraints (such as `balance >= 0` check constraints) are owned and validated by the Wallet Service exclusively, preventing inconsistencies or race conditions.
- **Loose Coupling:** The internal storage schema of wallets can change without breaking the Transaction Service, as long as the Feign client API contract remains stable.
- **Improved Security Boundaries:** High-risk write operations on wallet balances are restricted to dedicated REST endpoints that validate authorization, preventing arbitrary database writes from outside.

### Cons
- **Lack of Distributed Transactions (No 2PC):** If the Transaction Service debits a sender, but the subsequent credit to the receiver fails (e.g. due to a network timeout), the system is left in an inconsistent state unless a Saga pattern or compensating transaction is implemented.

### Future Improvements
- Implement event-driven eventual consistency using an asynchronous message broker (such as RabbitMQ or Apache Kafka) with transactional outbox patterns to handle heavy transaction volumes asynchronously.

---

## ADR 06: JWT Validation At Gateway

### Context
In a microservices architecture, incoming HTTP requests must be authenticated. Having every downstream service intercept, decrypt, cryptographically verify signatures, and validate expiration times of JWT tokens introduces duplication of security configurations and processing overhead, and increases the attack surface.

### Alternatives Considered
1. **Downstream Validation:** Downstream services (`wallet-service`, `transaction-service`) validate the token locally using a shared secret. (Previous approach, now shifted).
2. **Gateway Verification and Header Propagation:** The API Gateway validates incoming JWT tokens cryptographically at the perimeter. Upon successful validation, it injects the identity claims (`userId`, `email`, `role`) as HTTP headers (`X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`) and forwards the request. Downstream services read these headers and trust them implicitly.

### Chosen Solution
**Gateway Verification and Header Propagation**

### Pros
- **Decoupled Security Concerns:** Downstream services do not need to parse or decrypt JWTs. They rely on simple HTTP headers, leading to simpler code and fewer dependencies.
- **Lower In-Network Processing Overhead:** Cryptographic validation is performed once at the gateway rather than repeatedly at each microservice hop (e.g. Transaction Service calling Wallet Service).
- **Reduced Attack Surface:** Downstream services do not need access to the JWT symmetric secret key (minimizing key distribution risks).
- **Early Rejections:** Invalid or expired tokens are rejected immediately at the edge (401 Unauthorized), preventing unauthenticated requests from consuming downstream resource threads.

### Cons
- **Single Point of Failure for Auth Checks:** If the gateway authentication filter fails or has a misconfiguration, the security boundary of the entire cluster could be compromised.
- **Header Injection Vulnerabilities:** If an external client is able to bypass the gateway or direct-connect to downstream microservices, they can inject spoofed headers (e.g., `X-USER-ID=admin-uuid`).

### Future Improvements
- Implement **mTLS (Mutual TLS)** or private VPC subnets to ensure downstream services accept traffic *only* from the API Gateway.
- Enforce digital signature checks (e.g., asymmetric RS256 keys) where the Gateway signs a short-lived internal token for downstream verification, or implement a firewall rule to drop direct external calls.

---

## ADR 07: Asynchronous Notification Processing

### Context
When transactions (deposits, withdrawals, transfers) or wallet initializations occur, the system needs to send alerts (emails, in-app notifications). Performing these tasks synchronously within the critical path of the transaction processing flow increases response time, couples business logic with delivery states, and can cause transaction failures if the email server times out.

### Alternatives Considered
1. **Synchronous REST Call:** The Transaction or Wallet Service calls the Notification Service via a synchronous REST request (`POST /api/notifications/send`) during the transaction workflow.
2. **Asynchronous Event-Driven Messaging (AWS SQS):** The initiating service commits the business data, returns success immediately, and publishes a JSON event message to an AWS SQS queue. The Notification Service consumes the event asynchronously from the queue and processes it.

### Chosen Solution
**Asynchronous Event-Driven Messaging (AWS SQS) with Synchronous REST Integration**
We implement the synchronous REST path as the initial microservice integration mechanism while shipping a pre-designed AWS SQS consumer blueprint (`SqsPlaceholderListener.java`). This guarantees synchronous operation under developer mode while setting up a zero-friction migration to event-driven processing.

### Pros
- **Reduced Latency:** Resolves SMTP handshakes out-of-band, returning transaction status to the user instantly.
- **Fault Isolation:** If the email server fails or times out, the transaction process remains unaffected.
- **Reliability:** Message queues natively support retry configurations and Dead Letter Queues (DLQ) to prevent notification loss.

### Cons
- **Operational Complexity:** Adds a message broker dependency to the platform infrastructure.
- **Eventual Consistency:** Introduces a minor delay (usually under 2 seconds) between transaction completion and receipt of the email alert.

---

## ADR 08: Immutable Audit Log Architecture

### Context
Forensic auditing and compliance regulations (SOX, GDPR, SOC2) require keeping an immutable record of all user, security, and transaction-related actions across the platform. We must design a system that stores these audit trails, guaranteeing that no entry can be altered, overwritten, or deleted after creation.

### Alternatives Considered
1. **Application-Level Immutability via ORM Interceptors:** Enforce write-once, read-many rules in Java code using JPA `@PreUpdate` and `@PreRemove` lifecycle callbacks.
2. **Database-Level Privilege Isolation:** Restrict the database user assigned to the audit service to ONLY `INSERT` and `SELECT` permissions, dropping `UPDATE` or `DELETE` rights entirely.
3. **Write-Once-Read-Many (WORM) Storage:** Offload records directly to cloud object storage (e.g., AWS S3 with Object Lock/Glacier Vault Lock enabled).

### Chosen Solution
**Hybrid Approach: Application-Level JPA Interceptors with Database Privilege Restrictions**
We implement application-level hooks in the `AuditLog` entity class that intercept and block `PreUpdate` and `PreRemove` operations by throwing `UnsupportedOperationException`. In production, this is paired with database user permission restriction (allowing only `INSERT` and `SELECT` privileges on the `audit_logs` table) to prevent manual database-level manipulation.

### Pros
- **Compliance Alignment:** Fully complies with audit regulations requiring tamper-evident logs.
- **Dual-Layer Protection:** Prevents developer error in the application layer and malicious injection in the database layer.
- **Familiar Query Models:** Retains standard PostgreSQL query capability (indexes, joins) for audit dashboard reporting without latency penalties of WORM object storage access.

### Cons
- **Infinite Data Growth:** Immutable logs can never be pruned, necessitating a partition and archive strategy for historical records.
- **Write-Intense Storage:** Heavy traffic generates massive log volume, requiring future partitioning or async message buffering to avoid database lockups.

### Future Improvements
- Implement database table partitioning on the `audit_logs` table by month/quarter.
- Set up an automated archiving pipeline to stream logs older than 90 days to AWS S3 Glacier WORM storage for cheap long-term retention.

---

## ADR 09: Strategy Pattern For Risk Rules

### Context
The Risk Service must evaluate transaction and user actions for fraud and compliance risk. We need to execute multiple independent checks (such as transaction amount verification, velocity monitors, failed transaction counts, and money laundering indicators). The design must allow these rules to be developed, tested, and maintained independently, and must support adding new rules without editing the core evaluation engine orchestrator.

### Alternatives Considered
1. **Monolithic Evaluation Method:** Implement all checks inside a single massive service class method (e.g. `RiskService.evaluate()`). This concentrates logic, leading to complex conditional branches and making unit testing individual rules in isolation difficult.
2. **Strategy Pattern (Modular Rule Engine):** Define a standard `RiskRule` interface. Each rule is implemented as a separate Spring `@Component` class. The orchestrator `RiskService` autowires a `List<RiskRule>` and executes them dynamically in sequence.

### Chosen Solution
**Strategy Pattern (Modular Rule Engine)**

### Pros
- **Extensibility & Open-Closed Principle (OCP):** Adding a new risk check requires writing a new class implementing the `RiskRule` interface and decorating it with `@Component`. The core `RiskService` orchestrator does not require modifications.
- **Single Responsibility Principle (SRP):** Each class encapsulates exactly one fraud risk verification rule, making the code clean and maintainable.
- **Isolatable Testing:** Each rule class can be unit-tested independently by mocking its specific downstream Feign dependencies.
- **Dynamic Configuration Ready:** Rules can easily be enabled, disabled, or re-weighted at runtime using database configurations or feature flags without modifying code.

### Cons
- **Class Explosion:** Each new rule creates a new class file, increasing codebase complexity.
- **Rule Ordering & Dependencies:** If a rule's execution depends on the outcome of a previous rule, the strategy pattern requires additional execution order constraints (e.g., using `@Order` annotations) or custom context sharing.

### Future Improvements
- Move rule weights (penalty points) to a dynamic configuration source (database or Spring Cloud Config).
- Implement short-circuit evaluation logic if a critical rule determines that the transaction must be blocked immediately, bypassing subsequent rule checks.

---

## ADR 10: Storage Strategy Pattern For File Storage

### Context
The KYC Service manages physical file uploads (identity proof images, PDFs) alongside structured compliance records. In local development environments, we need a zero-dependency file storage mechanism that works offline without cloud resources. In staging and production cloud environments, files must be saved securely on high-durability cloud storage (e.g. AWS S3) to support multi-replica scalability, lifecycle archiving, and security boundaries.

### Alternatives Considered
1. **Direct Local File Storage:** Hardcode file-saving logic utilizing Java file system utility packages. This doesn't scale in cloud containers because files written to the local disk are ephemeral and lost on container restart or not shared across multiple nodes.
2. **Direct AWS S3 Storage:** Integrate S3 clients directly in the KYC controller/service code. This couples the codebase to AWS, requiring active AWS credentials, local credentials management, or local S3 emulation tools (like LocalStack) for local testing.
3. **Storage Strategy Pattern:** Define a unified `StorageService` interface. Code two strategy implementations: `LocalStorageService` (writing to absolute directory paths) and `S3StorageService` (streaming bytes to AWS S3 buckets). Bind them via dynamic configuration properties (`kyc.storage.provider = LOCAL | S3`) to instantiate the appropriate bean at startup.

### Chosen Solution
**Storage Strategy Pattern (Strategy Pattern)**

### Pros
- **Zero-Dependency Dev Mode:** Developers run tests and startup microservices locally using simple system folders without setting up cloud accounts or emulators.
- **Environment Agnostic / Cloud-Ready:** In production, swapping the configuration property to `S3` activates the AWS strategy, saving assets to distributed cloud storage.
- **Separation of Concerns:** Business logic classes (e.g., `KycService`) remain entirely unaware of the underlying physical storage mechanism. They interact with `StorageService` interface, saving and deleting files transparently.
- **Easy Extension:** Future storage providers (e.g. Google Cloud Storage, Azure Blob, or distributed IPFS storage) can be supported by writing a new implementation of the interface without modifying the service logic.

### Cons
- **Abstraction Overhead:** Introduces interfaces and conditional configuration classes.
- **Asset URI Disparity:** Database records store local filesystem paths (e.g. `/uploads/...`) in dev mode but S3 keys/URLs in production, requiring path resolver logic.

### Future Improvements
- Implement a path resolver service to map the stored file identifier to a signed pre-authenticated download URL (presigned URL) for admin document views, keeping documents private.
- Configure S3 Object Lock and lifecycle transitions to automatically transition old documents to Glacier for archival compliance.
