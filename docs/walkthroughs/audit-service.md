# Audit Service Walkthrough & System Guide

This guide details the design, rationale, APIs, security, database design, and interview talking points for the **Audit Service** inside **CryptoVault**.

---

## 1. Architectural & Design Explanations

### Role of Audit Service in Port 8086
The Audit Service runs as a dedicated microservice on port `8086`. It serves as the centralized, high-security logging store for all compliance, identity, security, and transaction events across the platform. Keeping this service isolated prevents heavy audit querying from impacting core wallet or authentication database transactions.

### Service Segregation (No Business Logic mutability)
Under microservice architectural principles, the Audit Service maintains strict segregation. It does NOT own or modify wallet balances, execute transfer transactions, authenticate users, or send notifications directly. By remaining 100% focused on compliance append-only logs, it maintains a clean boundary and avoids side-effect mutations.

### Data Immutability Pattern
To prevent tampering (internal fraud or external attackers deleting trails), audit logs are immutable:
1. **JPA Lifecycle Interceptors:** The `AuditLog` entity registers `@PreUpdate` and `@PreRemove` callbacks that immediately throw `UnsupportedOperationException`. This guarantees application-level compile and runtime write-once safety.
2. **REST Constraints:** No `PUT`, `PATCH`, or `DELETE` controller mapping routes are exposed.
3. **Database Privilege Isolation:** In production deployments, database credentials assigned to the audit service are restricted to only `INSERT` and `SELECT` statements, making data alterations physically impossible.

### Stateless Trust Boundary & Context Propagation
Like the other microservices in the platform:
- The **API Gateway** acts as the cryptographic perimeter guard, validating incoming JWTs and stripping external headers.
- It propagates verified claims via HTTP headers: `X-USER-ID`, `X-USER-EMAIL`, and `X-USER-ROLE`.
- The Audit Service reads these headers using `HeaderUserInterceptorFilter` to construct a local `SecurityContext`, bypassing local token decryption or network roundtrips to `auth-service`.

---

## 2. Walkthrough Specifications

### Domain Model
The core domain model is `AuditLog`. It registers details such as:
- Which user performed the action (`userId` and `performedBy`).
- The action and service origin (`action` and `serviceName`).
- Specific type of compliance event (`eventType`).
- Audit metadata (`description`, `ipAddress`, `eventTimestamp`, `createdAt`).

### Database Design
Table `audit_logs` is owned by the Audit Service. Columns include:
- `id` (UUID Primary Key)
- `user_id` (UUID index, Nullable for system-level actions)
- `event_type` (VARCHAR enum values mapped as strings)
- `service_name` (VARCHAR)
- `action` (VARCHAR)
- `description` (VARCHAR(1000))
- `ip_address` (VARCHAR)
- `performed_by` (VARCHAR email or SYSTEM)
- `event_timestamp` & `created_at` (TIMESTAMP via JPA Auditing)

### API Design
- `POST /api/audit`: Creates a compliance event log record.
- `GET /api/audit`: Retrieves all historical logs (Admin clearance).
- `GET /api/audit/{id}`: Retrieves details of a specific log by UUID.
- `GET /api/audit/user/{userId}`: Filters logs associated with a user ID.
- `GET /api/audit/type/{eventType}`: Filters logs by event classification.
- `GET /api/audit/date-range?start={start}&end={end}`: Filters logs chronologically.

### Observability & Custom Telemetry
Prometheus metrics are collected via Spring Actuator and Micrometer. The service increments custom counters:
- `audit_events_created`: Tracks logged events grouped by type and service.
- `audit_queries_executed`: Tracks audit history searches.
- `failed_audit_requests`: Tracks failed audit payload validations.

### Future Event-Driven SQS Integration
To handle massive workloads, we implement the async blueprint `SqsPlaceholderListener.java`. Downstream services will write to SQS queues instead of making block REST calls. The listener consumes JSON event streams, translating and inserting them asynchronously to keep response times sub-millisecond.

---

## 3. Class-by-Class Documentation

### `AuditApplication`
- **Purpose:** Bootstraps the Audit microservice.
- **Why It Exists:** Configures and launches the Spring Application Context.
- **Interview Talking Points:** Runs Spring Boot 3 auto-configurations and component scans on package `com.cryptovault.audit`.

### `JpaAuditingConfig`
- **Purpose:** Enables JPA lifecycle auditable entities.
- **Why It Exists:** Automatic population of `@CreatedDate` fields during entity persistence.
- **Interview Talking Points:** Separates timestamp maintenance from business controller layers.

### `SecurityConfig`
- **Purpose:** Restricts route access parameters.
- **Why It Exists:** Allows public internal POST requests for services logging events while restricting GET query paths.
- **Interview Talking Points:** Establishes a stateless security policy, resolving session vulnerabilities.

### `HeaderUserInterceptorFilter`
- **Purpose:** Reads security contexts propagated from the API Gateway.
- **Why It Exists:** Decodes `X-USER-ID`, `X-USER-EMAIL`, and `X-USER-ROLE` headers to construct local auth principals.
- **Interview Talking Points:** Eliminates the need to call auth-service or verify JWT signature repeatedly, boosting performance.

### `AuditLog`
- **Purpose:** Mapped persistence entity representing an audit log entry.
- **Why It Exists:** Persists records inside PostgreSQL database.
- **Interview Talking Points:** Uses `@PreUpdate` and `@PreRemove` lifecycle hooks to throw `UnsupportedOperationException`, guaranteeing immutable data contracts at the application layer.

### `AuditLogRepository`
- **Purpose:** Database persistence access layer.
- **Why It Exists:** Declares query mapping methods filtering logs by service, user, type, and dates.
- **Interview Talking Points:** Automatically mapped dynamically by Spring Data JPA interfaces.

### `AuditService`
- **Purpose:** Business rule coordinator.
- **Why It Exists:** Inserts log records, performs immutability validation, and updates Micrometer Prometheus metrics.
- **Interview Talking Points:** Showcases integration of custom Micrometer counters (`MeterRegistry`) to build business observability.

### `AuditController`
- **Purpose:** Exposes REST query mapping endpoints.
- **Why It Exists:** Binds client JSON payloads and parses URL search query filters.
- **Interview Talking Points:** Packages responses in a generic `ApiResponse<T>` envelope wrapper.

### `SqsPlaceholderListener`
- **Purpose:** Asynchronous event consumer blueprint.
- **Why It Exists:** Demonstrates architecture readiness for high-throughput messaging integration.
- **Interview Talking Points:** Sets up event-driven asynchronous decoupled log parsing structure.
