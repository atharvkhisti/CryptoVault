# Notification Service Walkthrough & System Guide

This guide details the design, rationale, APIs, security, database design, and interview talking points for the **Notification Service** inside **CryptoVault**.

---

## 1. Architectural & Design Explanations

### Stateless Header Authorization Boundary (Implicit Trust)
Unlike other microservices that parse JWTs directly, the Notification Service leverages the **API Gateway trust boundary**. The API Gateway acts as the perimeter guard, validating incoming JWT signatures cryptographically at the edge of the system.
Once verified, the Gateway extracts token claims and injects them as downstream HTTP headers (`X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`). Downstream, the Notification Service reads these headers directly using `HeaderUserInterceptorFilter` and builds a local `SecurityContext`. 
This implicit trust model removes the need for downstream microservices to maintain JWT secret keys or perform CPU-heavy cryptographic validations, ensuring maximum horizontal scalability. 

### Why a Lightweight Local Template Engine is Used
Rather than pulling in heavy template rendering engines (such as Thymeleaf, FreeMarker, or Mustache), which introduce massive transit dependencies, JAR bloat, and memory overhead, we designed a lean `SimpleTemplateEngine`. 
It performs exact string substitutions of `${placeholder}` variables on basic HTML files loaded from classpath resources. This keeps the execution footprint minimal, reduces compilation time, and satisfies the microservice requirement for minimal dependency overhead.

### Decoupled Asynchronous Alerting Blueprint
For production banking systems, notification delivery should not run synchronously within the transaction critical path. If SMTP servers fail or encounter network latency, synchronous operations would hang, causing transactional timeouts.
We designed a decoupled architecture where the business service (e.g. Transaction Service) returns success immediately after persisting records, while publishing a notification event asynchronously. The Notification Service consumes this message from an **AWS SQS Queue** and executes delivery out-of-band. 
Our service includes a pre-designed, commented listener `SqsPlaceholderListener.java` showing the exact `@SqsListener` binding, ensuring a zero-friction migration path to a fully event-driven state.

---

## 2. Walkthrough Specifications

### Domain Model
The core entity is `Notification`, mapping an alert payload record containing the user UUID, recipient email, notification type, subject, body content, status (e.g. `PENDING`, `SENT`, `FAILED`), and potential SMTP error traces.

### Database Design
The table `notifications` is owned by `notification-service`. Columns include:
- `id` (UUID Primary Key)
- `user_id` (UUID Index)
- `recipient_email` (VARCHAR)
- `type` (VARCHAR)
- `subject` (VARCHAR)
- `body` (TEXT)
- `status` (VARCHAR)
- `error_message` (TEXT - Nullable)
- `created_at` & `updated_at` (TIMESTAMP via JPA Auditing)

### API Design
- `POST /api/notifications/send`: Trigger a notification delivery.
- `GET /api/notifications/user/{userId}`: Retrieve historical alerts logs for a specific user.
- `GET /api/notifications/{id}`: View specific alert log details by UUID.
- `GET /api/notifications`: Retrieve all historical alert logs (typically for platform admins).

### Security Considerations
- Security context is parsed from trusted gateway headers using `HeaderUserInterceptorFilter`.
- Direct endpoint calls for sending notifications (`POST /api/notifications/send`) are secured so only authorized services or gateways can call them internally, while history endpoints require matching `userId` or administrative clearance.

### Testing Strategy
- Core tests verify template substitutions, MIME message wrapping, SMTP failure catches, and Spring Security mapping.
- All test suites run successfully using an in-memory H2 database under Spring Boot test slices (`@WebMvcTest`, `@DataJpaTest`), achieving >80% coverage.

---

## 3. Class-by-Class Documentation

### `NotificationApplication`
- **Purpose:** Entry point of the microservice.
- **Why It Exists:** Bootstraps the Spring Boot runtime context.
- **How It Fits:** Configures bean mappings, auto-configuration parameters, and scans package components.
- **Interview Talking Points:** Uses `@SpringBootApplication` and is configured to run on port `8084`.

### `JpaAuditingConfig`
- **Purpose:** Enables JPA Auditing callbacks.
- **Why It Exists:** Automates setting of audit timestamps.
- **How It Fits:** Integrates with `@CreatedDate` and `@LastModifiedDate` on the entity layer.
- **Interview Talking Points:** Eliminates manual date management code, keeping persistence audits clean.

### `SecurityConfig`
- **Purpose:** Configures Spring Security authorization rules.
- **Why It Exists:** Binds stateless request interceptors and secures endpoints.
- **How It Fits:** Disables CSRF (stateless session boundary) and plugs in the custom header interceptor.
- **Interview Talking Points:** Restricts endpoint mappings to stateless operations, permitting internal service calls while protecting user query boundaries.

### `HeaderUserInterceptorFilter`
- **Purpose:** Context boundary filter.
- **Why It Exists:** Extracts security headers injected by the API Gateway.
- **How It Fits:** Intercepts incoming requests, parses `X-USER-` headers, and builds the local `SecurityContextHolder`.
- **Interview Talking Points:** Illustrates the microservices trust boundary model, avoiding redundant token validations downstream.

### `Notification` (Entity)
- **Purpose:** Database entity mapping.
- **Why It Exists:** Maps persistent audit logs of notification delivery events to PostgreSQL.
- **How It Fits:** Interacted with by the JPA repository.
- **Interview Talking Points:** Employs UUID primary keys, stores enums as strings (`EnumType.STRING`), and isolates the template body as text.

### `NotificationRepository`
- **Purpose:** JPA database operations accessor.
- **Why It Exists:** Exposes queries to the database tables.
- **How It Fits:** Leverages Spring Data JPA interfaces.
- **Interview Talking Points:** Exposes user-specific chronological lookups sorted descending by creation time.

### `SendNotificationRequest` & `NotificationResponse` (DTOs)
- **Purpose:** Request/Response payloads contracts.
- **Why It Exists:** Decouples internal database entities from external network data.
- **How It Fits:** validated in controllers via `@Valid` annotations.
- **Interview Talking Points:** Includes validation constraints like `@NotBlank`, `@Email`, and `@NotNull` to filter payload errors at the service boundary.

### `NotificationMapper`
- **Purpose:** Component translator.
- **Why It Exists:** Maps DTOs to entities and response objects.
- **How It Fits:** Simple component mapping to avoid boilerplate code.
- **Interview Talking Points:** Manages clean translation to isolate persistence models from serialization schemas.

### `SimpleTemplateEngine`
- **Purpose:** Local template compiler.
- **Why It Exists:** Substitutes dynamic parameters in HTML mails.
- **How It Fits:** Read files from resources classpath and executes exact string mappings.
- **Interview Talking Points:** Offers a zero-dependency, high-speed substitution engine for minimal memory consumption.

### `EmailService`
- **Purpose:** SMTP wrapper.
- **Why It Exists:** Manages SMTP MIME email processing.
- **How It Fits:** Leverages Spring's `JavaMailSender` helper beans.
- **Interview Talking Points:** Prepares rich HTML emails, setting from, recipient, subject, and body variables.

### `NotificationService`
- **Purpose:** Business workflow coordinator.
- **Why It Exists:** Resolves templates, triggers mail deliveries, audits records, and handles SMTP errors.
- **How It Fits:** Integrates the repository, template engine, and email service under transactional boundaries.
- **Interview Talking Points:** Records notification state changes (`PENDING` -> `SENT`/`FAILED`) and logs stack traces to `error_message` on delivery failure.

### `NotificationController`
- **Purpose:** Rest Endpoint layer.
- **Why It Exists:** Handles route mapping and payload processing.
- **How It Fits:** Intercepts caller invocations, coordinates services, and packages results.
- **Interview Talking Points:** Returns responses wrapped in uniform `ApiResponse<T>` envelopes.

### `SqsPlaceholderListener`
- **Purpose:** Message broker listener blueprint.
- **Why It Exists:** Documents how to migrate from REST calls to event-driven processing.
- **How It Fits:** Placeholder component containing commented-out `@SqsListener` bindings.
- **Interview Talking Points:** Pre-designed listener verifying how AWS SQS integration allows fully decoupled asynchronous operations.
