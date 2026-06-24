# KYC Service Walkthrough & System Guide

This guide details the design, rationale, APIs, security, database design, and interview talking points for the **KYC (Know Your Customer) Service** inside **CryptoVault**.

---

## 1. Business Problem
To operate legally and prevent financial crimes (such as money laundering, terrorist financing, and identity fraud), financial and digital asset platforms must comply with strict **AML (Anti-Money Laundering)** and **KYC (Know Your Customer)** regulations. Key challenges include:
- **Identity Proofing:** Collecting and validating governmental identity records (e.g., PAN and AADHAAR cards).
- **Compliance Lifecycle Management:** Transitioning verification applications through structured verification states (Pending, Under Review, Approved, Rejected).
- **Secure File Storage:** Storing highly sensitive identity document files without introducing security vulnerabilities (like remote code execution or file path traversal) and allowing easy scaling.
- **Microservices Orchestration:** Ensuring compliance updates are propagated to auditing and notification sub-systems without coupling the core transaction engine.

---

## 2. Identity Verification Lifecycle
The KYC status of a user changes through a deterministic state machine:

```
    ┌───────────┐
    │  PENDING  │ ◄─────────────────────────┐ (Re-upload required)
    └─────┬─────┘                           │
          │ User uploads PAN & AADHAAR      │
          ▼                                 │
   ┌─────────────┐                          │
   │UNDER_REVIEW │                          │
   └──────┬──────┘                          │
          │ Admin approves / rejects        │
          ├─────────────────────────────────┘
          ├──► APPROVED
          └──► REJECTED
```

- **`PENDING`:** Record initialized. The user can upload documents but has not submitted the package for validation yet.
- **`UNDER_REVIEW`:** The user has submitted both PAN and AADHAAR. The application is locked, and administrators review the files.
- **`APPROVED`:** Verification approved. The user is cleared to execute financial transactions (such as deposits, withdrawals, and transfers).
- **`REJECTED`:** Verification rejected. Remarks explain the reason (e.g., blurry image), and the user can upload new files and resubmit.

---

## 3. Document Validation Pipeline
To protect the local filesystem and downstream storage, the service enforces strict validations:
- **Format Filtering:** Restricts uploads to `image/jpeg` (extensions `.jpg`, `.jpeg`), `image/png` (extension `.png`), and `application/pdf` (extension `.pdf`). Filename extensions are verified to match their MIME type headers.
- **Size Bounds:** Enforces a hard **5MB** limit per file size using both Tomcat properties and custom validators to prevent denial-of-service (DoS) storage exhaustion.
- **Traversal Mitigation:** Cleans filenames using `Paths.get(name).getFileName()` and throws a `SecurityException` if the input contains relative parent directory markers (`..`).
- **Uniqueness Check:** Enforces that a user can upload at most one document of each `DocumentType` (e.g., cannot upload multiple PAN cards), preventing duplicate file waste.

---

## 4. Submission Validation Logic
Before transitioning a KYC record to `UNDER_REVIEW`, the orchestrator checks:
1. **Document Completeness:** The user must have uploaded **both** a `PAN` and an `AADHAAR` document. Other document types (like `PASSPORT` or `DRIVING_LICENSE`) are optional.
2. **User Existence:** Queries the `auth-service` via Feign client `GET /api/auth/{userId}` to verify the user exists on the platform.
3. **Audit Dispatch:** Pushes a `KYC_SUBMITTED` compliance event synchronously to the `audit-service`.

---

## 5. Storage Strategy Pattern
The service uses the **Strategy Pattern** to decouple the business logic from physical storage:

- **`StorageService` Interface:**
  ```java
  public interface StorageService {
      String store(MultipartFile file, String subFolder);
      void delete(String filePath);
  }
  ```
- **`LocalStorageService`:** Saves files to the local disk inside user-specific folders (`uploads/<userId>/`).
- **`S3StorageService`:** A placeholder implementation preparing for AWS S3 cloud migration.
- **Strategy Selection (`StorageConfig`):** Dynamically wires the implementation bean based on the `kyc.storage.provider` property:
  ```properties
  kyc.storage.provider=LOCAL
  ```

---

## 6. Feign Integration
The KYC Service integrates with the wider CryptoVault platform using OpenFeign:
- **`AuthClient`:** Queries `auth-service` to verify user existence during submission.
- **`AuditClient`:** Dispatches structured compliance events (`KYC_SUBMITTED`, `KYC_APPROVED`, `KYC_REJECTED`) to the `audit-service` to preserve immutable compliance records.
- **`NotificationClient`:** Sends trigger dispatches (`KYC_APPROVED` or `KYC_REJECTED` emails) to `notification-service`.
- **Context Forwarding:** The Feign client configuration injects Gateway-propagated headers (`X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`) into outgoing calls, maintaining the user context across boundaries.

---

## 7. Event-Driven Design
For developer ease, the service implements synchronous REST calls to other services. However, it is fully prepared for asynchronous decoupling:
- **`SqsPlaceholderListener.java`:** Defines listener blueprints for consuming asynchronous SQS queues.
- **Asynchronous Flow:** In production, KYC state changes (such as approval) will publish events to an SQS queue or SNS topic. Services like the Wallet Service will listen for `KycApprovedEvent` to unlock wallets, and the Notification Service will consume it to trigger email dispatches without blocking the client thread.

---

## 8. Test Suite & Coverage
The KYC Service runs a test suite containing 18 unit and integration tests with >80% code coverage:
- **`LocalStorageServiceTest`:** Validates directory traversal prevention, file writing, folder creation, and deleting.
- **`KycServiceTest`:** Unit tests verifying validation rules, state transitions, Feign failure resilience, and database persistence.
- **`KycControllerTest`:** Integrations testing endpoints, security filters (restricting reviews to `ROLE_ADMIN`), context headers extraction, and error mappings.

---

## 9. Observability & Telemetry
Observed telemetry metrics are gathered and exposed at `/actuator/prometheus` via Micrometer:
- `kyc_submissions_total`: Cumulative counter tracking KYC review submissions.
- `kyc_approvals_total`: Cumulative counter tracking administrative approvals.
- `kyc_rejections_total`: Cumulative counter tracking administrative rejections.
- `document_uploads_total`: Cumulative counter tracking total document files uploaded.

---

## 10. Interview Talking Points
Use these architectural talking points for technical interviews:
1. **Decoupled File Storage (Strategy Pattern):** Explains how the Strategy Pattern decouples file system specifics from business logic, allowing us to swap the local directory storage for AWS S3 with a single configuration file property change.
2. **Path Traversal & Injection Prevention:** Demonstrates security awareness by validating MIME formats, matching extensions, and cleaning filenames to prevent directory traversal breakouts and remote code execution vulnerabilities.
3. **Role-Based Edge Security:** Emphasizes that admin endpoints `/api/kyc/approve` and `/api/kyc/reject` are secured with `@PreAuthorize("hasRole('ADMIN')")`, which relies on context headers injected at the Gateway boundary.
4. **Service Autonomy & Resilience:** Explains that the KYC Service queries user context from headers rather than querying the database or making synchronous network calls back to `auth-service` for token verification, maximizing throughput and boundary isolation.
