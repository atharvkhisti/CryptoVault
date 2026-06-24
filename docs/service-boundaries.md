# Service Boundaries & Data Ownership

This document defines context boundaries, data ownership, modification policies, and communication rules across the **CryptoVault** platform.

## 1. Domain Context Boundaries

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                CryptoVault System                                                │
│                                                                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │
│  │  Authentication │  │     Wallet      │  │   Transaction   │  │  Notification   │  │          Audit          │ │
│  │    Boundary     │  │    Boundary     │  │    Boundary     │  │    Boundary     │  │        Boundary         │ │
│  │                 │  │                 │  │                 │  │                 │  │                         │ │
│  │ Owns: Profiles, │  │ Owns: Balances  │  │ Owns: Ledger    │  │ Owns: History   │  │ Owns: Immutable         │ │
│  │       Creds     │  │                 │  │       History   │  │       Logs      │  │       Compliance Logs   │ │
│  │                 │  │                 │  │                 │  │                 │  │                         │ │
│  │  Auth Serv. DB  │  │  Wallet Serv DB │  │ Transaction DB  │  │ Notification DB │  │      Audit Serv DB      │ │
│  └────────┬────────┘  └────────▲────────┘  └────────┬────────┘  └────────▲────────┘  └────────────▲────────────┘ │
│           │                    │                    │                    │                        │              │
│           └─────── Propagates ─┴───────── Propagates┘                    │                        │              │
│                    JWT Context            JWT Context via Feign          │                        │              │
│                                                                          │                        │              │
│           └────────────────────────── Trigger Asynchronously / Rest ─────┴────────────────────────┘              │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

```

---

## 2. Service-Specific Data Ownership & Modification Rules

### API Gateway (`api-gateway`)
- **Owned Data:** None. The API Gateway is completely **stateless**. It does not own databases, cache business data, or persist user info.
- **Access Rule:** Forbidden from querying any databases or holding business domain logic.
- **Modification Rule:** Mutates HTTP requests to inject verified identity headers (`X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`) after successful cryptographic JWT signature validation.
- **Responsibilities:**
  - Ingress traffic routing to downstream microservices.
  - local JWT signature checking and expiration checks.
  - Header context propagation.
  - Global CORS handling.
  - Centralized request/response logging for security audit trails.
  - Consolidated edge error mapping.

### Authentication Service (`auth-service`)
- **Owned Data:** User accounts, logins, credential records, access roles (`USER`, `ADMIN`).
- **Access Rule:** Only the `auth-service` database is allowed to read or write credentials mappings.
- **Modification Rule:** Only `auth-service` can register users or modify credentials.

### Wallet Service (`wallet-service`)
- **Owned Data:** Wallet balances, currency scopes, internal wallet metadata.
- **Access Rule:** Owns the `wallets` table in PostgreSQL. Cannot query user credentials or transaction ledger databases directly.
- **Modification Rule:** Read/write wallet accounts.
- **Ownership Boundary:** Downstream transaction services request balance updates through `/api/wallets/{id}/debit` or `/api/wallets/{id}/credit` routes. No other service can update a wallet's balance directly.

### Transaction Service (`transaction-service`)
- **Owned Data:** Transaction records, lifecycle status (`PENDING`, `COMPLETED`, `FAILED`), business reference numbers.
- **Access Rule:** Owns the `transactions` table in PostgreSQL. Cannot query user credentials or wallet databases directly.
- **Modification Rule:** Read/write transaction history ledger entries.
- **Ownership Boundary:** Owns transaction history. Never writes directly to the wallets table; it must invoke the **Wallet Service** via Feign client APIs.

### Notification Service (`notification-service`)
- **Owned Data:** Notification history logs, delivery status (`PENDING`, `SENT`, `FAILED`), mail headers/metadata.
- **Access Rule:** Owns the `notifications` table in PostgreSQL. Cannot query credentials, wallets, or transaction databases directly.
- **Modification Rule:** Read/write notification logs.
- **Ownership Boundary:** Only the Notification Service can modify notification records. Other microservices send alerts via HTTP REST API or via future asynchronous SQS event message queues.

### Audit Service (`audit-service`)
- **Owned Data:** Immutable compliance logs, event types, forensic metadata.
- **Access Rule:** Owns the `audit_logs` table in PostgreSQL. Cannot query credentials, wallets, transactions, or notifications databases.
- **Modification Rule:** APPEND-ONLY. Updates, deletes, or truncates are forbidden at both application (via JPA Entity callbacks) and database levels.
- **Ownership Boundary:** Only `audit-service` has access. Other microservices push audit logging via REST endpoints or asynchronously via SQS.

### Risk Service (`risk-service`)
- **Owned Data:** Compliance and fraud risk assessment logs (`risk_assessments` table).
- **Access Rule:** Owns the `risk_assessments` table in PostgreSQL. Cannot query user credentials, wallets, or transaction databases directly.
- **Modification Rule:** Read/write risk assessment history records. It is forbidden from altering or modifying the data owned by transaction, wallet, or audit services.
- **Ownership Boundary:** Only `risk-service` writes risk assessment profiles. Other services request evaluations synchronously via `/api/risk/evaluate` or check history logs.
- **Read-Only External Access:** To evaluate risk rules, `risk-service` uses Feign clients to query transaction lists from `transaction-service`, audit logs from `audit-service`, and wallet metadata from `wallet-service`. This access is strictly **read-only**.

### KYC Service (`kyc-service`)
- **Owned Data:** KYC verification records (`kyc_records` table) and document file metadata (`kyc_documents` table), plus physical file assets (saved locally or on S3).
- **Access Rule:** Owns the `kyc_records` and `kyc_documents` tables in PostgreSQL. Cannot query credentials, wallets, or transaction databases directly.
- **Modification Rule:** Read/write KYC profiles and upload/delete file context objects. It is forbidden from altering or modifying the data owned by transaction, wallet, or audit services.
- **Ownership Boundary:** Only `kyc-service` writes compliance status and document records. Other services query status check routes.
- **Read-Only External Access:** To verify user registration validity, `kyc-service` queries `auth-service` via a Feign client. This access is strictly **read-only**.
- **Write-Only External Calls:** `kyc-service` uses Feign clients to log security events on `audit-service` and trigger alerts on `notification-service`.

---

## 3. Inter-Service Communication Rules

1. **Stateless Trust Boundary & Context Propagation:**
   - The **API Gateway** acts as the trusted perimeter guard. It validates the client's JWT token cryptographically.
   - Once validated, the Gateway injects the identity claims as trusted headers: `X-USER-ID`, `X-USER-EMAIL`, and `X-USER-ROLE`.
   - Downstream services (`wallet-service`, `transaction-service`, `notification-service`, `risk-service`, `kyc-service`) consume these headers directly to determine the calling user's identity. They **never** call `auth-service` over the network to validate users or tokens, which drastically reduces network chatter, removes database loads on the Auth service, and maximizes horizontal scalability.
2. **Database Isolation:**
   - No microservice is allowed to query the database of another microservice. All cross-boundary database transactions are strictly prohibited.
3. **Synchronous Feign Interactions:**
   - The **Transaction Service** communicates synchronously with the **Wallet Service** using Spring Cloud OpenFeign to query wallet details and request debit/credit operations.
   - The **Risk Service** communicates synchronously with the **Transaction Service**, **Audit Service**, and **Wallet Service** via OpenFeign to fetch historical context required for evaluating risk rules (e.g., transaction velocities, login history, and wallet details).
   - The **KYC Service** communicates synchronously with the **Auth Service** to check user profile status, and integrates with the **Audit Service** and **Notification Service** using OpenFeign to log administrative transitions and trigger compliance emails.
   - All Feign clients must use an interceptor to propagate the user context headers (`X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`) downstream, maintaining the user security context across microservices boundaries.
4. **Local Rollback & Compensating Actions:**
   - Since HTTP operations do not participate in a single global database transaction (absence of 2PC), the **Transaction Service** must implement compensating rollback API requests (e.g., refunding the sender if the receiver's credit fails) to preserve system-wide consistency.
5. **Decoupled Notification, Auditing, and Risk/KYC Architecture (Future Event-Driven State):**
   - Currently, other services call `/api/notifications/send`, `/api/audit`, `/api/risk/evaluate`, and dispatch actions synchronously.
   - In the future, these services will publish JSON messages representing events (such as `TransactionEvent`, `KycSubmittedEvent`, or `AuditEvent`) to **AWS SQS Queues**. The **Notification Service**, **Audit Service**, **Risk Service**, and **KYC Service** will asynchronously consume these messages, completely decoupling core operations from alerts, compliance logging, and fraud analysis. For example, high-risk evaluations or KYC updates will asynchronously trigger SNS alerts or account flags.


