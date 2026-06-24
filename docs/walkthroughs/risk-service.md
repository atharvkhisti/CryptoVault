# Risk Service Walkthrough & System Guide

This guide details the design, rationale, APIs, security, database design, and interview talking points for the **Risk Service** inside **CryptoVault**.

---

## 1. Business Problem
In digital asset wallet platforms, transactions execute rapidly, and balances can be instantly depleted. This speed exposes the platform to significant business risks, including:
- **Capital Flight & Theft:** Large unauthorized transactions transferring assets out of the platform.
- **Velocity Abuse:** Rapid, automated transaction requests that overload the system or attempt to exploit race conditions.
- **Systematic Fraud:** Credential stuffing or routing exploits that manifest as a high frequency of failed transactions.
- **Wash-Trading / Money Laundering:** Rapidly cycling funds by depositing and immediately withdrawing to obfuscate the source of wealth.

To prevent financial loss and ensure regulatory compliance, the platform must evaluate risk **preemptively** before finalizing ledger updates.

---

## 2. Fraud Detection
The Risk Service identifies anomalous behaviors through dynamic behavioral heuristics:
- **Massive Capital Transfers:** Spotting single transaction amounts exceeding defined thresholds.
- **High-Velocity Cycles:** Spotting high-frequency transfer patterns within short time frames.
- **Probing & Trial-and-Error Attacks:** Identifying users experiencing high failure rates in recent transactions.
- **Wash-Trading Indicator:** Detecting deposits immediately followed by withdrawals within a 5-minute rolling window, bypassing traditional holding periods.

---

## 3. Risk Scoring
The service computes a numerical risk score on a scale of **0 to 100**. This score maps directly to a risk level and enforcement action:

| Score Bracket | Risk Level | Status Action | Security Action & Enforcement |
| :--- | :--- | :--- | :--- |
| **0 - 25** | `LOW` | `APPROVED` | Transaction is allowed to proceed automatically. |
| **26 - 50** | `MEDIUM` | `APPROVED` | Transaction proceeds; metadata is flagged in logs for statistical tracking. |
| **51 - 75** | `HIGH` | `FLAGGED` | Transaction proceeds but user profile and ledger records are flagged for manual compliance review. |
| **76 - 100** | `CRITICAL` | `BLOCKED` | Transaction is rejected immediately. Source/destination accounts are flagged for blocking. |

---

## 4. Rule Engine
The core risk engine orchestrator (`RiskService.java`) runs synchronous evaluations:
1. Collects context payload (`EvaluateRiskRequest`).
2. Leverages Feign clients to query historical transaction logs, audit trails, and wallet details.
3. Loops through all autowired `RiskRule` strategies, aggregating penalty points.
4. Restricts the aggregated score to a maximum of 100.
5. Maps the final score to a `RiskLevel` and `RiskStatus`.
6. Increments Prometheus metrics counters.
7. Saves the immutable `RiskAssessment` record to the PostgreSQL database.
8. Returns the result enveloped inside the `ApiResponse<RiskResponse>` structure.

---

## 5. Strategy Pattern
The service uses the **Strategy Pattern** to achieve modularity and satisfy the **Open-Closed Principle (OCP)**:
- **`RiskRule` Interface:** Defines the common execution contract:
  ```java
  public interface RiskRule {
      int evaluate(EvaluateRiskRequest request, Map<String, Object> context);
  }
  ```
- **Spring Autowiring:** The `RiskService` dynamically autowires a list of rules: `List<RiskRule> rules`. Adding a new rule is as simple as adding a new `@Component` class implementing the `RiskRule` interface. The core orchestrator remains untouched.
- **Rule Implementations:**
  - **`HighAmountRule`:** Adds +40 points if the transfer/withdrawal exceeds 10,000 USD/INR equivalent.
  - **`FrequentTransferRule`:** Adds +30 points if the user has more than 5 transfers/withdrawals within a rolling 10-minute window.
  - **`FailedTransferRule`:** Adds +20 points if the user has more than 3 failed transactions in the last 24 hours.
  - **`RapidWithdrawalRule`:** Adds +40 points if a withdrawal is requested within 5 minutes of any deposit.

---

## 6. Feign Integration
To pull historical context without accessing databases of other services directly, the Risk Service integrates resilient Feign clients:
- **`TransactionClient`:** Queries transaction lists from `transaction-service`.
- **`AuditClient`:** Queries user audit logs from `audit-service`.
- **`WalletClient`:** Queries wallet details from `wallet-service`.
- **Resilience:** Feign calls are wrapped in defensive try-catch blocks. If a downstream microservice is offline, the rule engine falls back to empty datasets, allowing the evaluation to complete gracefully rather than causing cascading failures in the transaction flow.

---

## 7. Event-Driven
While risk evaluations are currently synchronous, the system is designed for a transition to an asynchronous, event-driven architecture:
- **`SqsPlaceholderListener.java`:** Consumes event payloads asynchronously from an AWS SQS queue.
- **AWS SNS Publisher:** In a production configuration, when a `CRITICAL` or `HIGH` assessment is recorded, the service will publish a `RiskAlertEvent` to an AWS SNS topic. Other microservices (like Auth or Wallet services) consume this topic to lock user accounts or freeze wallet balances instantly.

---

## 8. Testing
The Risk Service maintains high reliability using a comprehensive test suite in `src/test/java`:
- **`RiskRuleTests`:** Unit tests verifying individual rules against mocked historical contexts.
- **`RiskServiceTest`:** Verifies orchestrator scoring computations, bracket mappings, and Feign exception handling.
- **`RiskControllerTest`:** Tests the REST API endpoints using `MockMvc` with mock security filters.
- **`RiskRepositoryTest`:** Tests persistence layer operations against an H2 database.
- **Execution:** Running `mvn test` compiles and executes the test suite, achieving over 80% coverage.

---

## 9. Monitoring
Observed telemetry is exposed via Spring Boot Actuator and Prometheus at `/actuator/prometheus`.
Micrometer counters track real-time security events:
- `risk_evaluations_total`: Total number of evaluations executed.
- `high_risk_transactions`: Count of evaluations scoring in the HIGH (51-75) bracket.
- `critical_risk_transactions`: Count of evaluations scoring in the CRITICAL (76-100) bracket.
- `flagged_accounts`: Number of unique accounts flagged due to high/critical risk.

---

## 10. Interview Points
Key talking points for technical architecture interviews:
1. **Perimeter-Validated JWT Security:** Downstream services (including `risk-service`) trust user claims forwarded in HTTP headers (`X-USER-`) injected by the API Gateway. This eliminates redundant token parsing, public-key verification, and Auth Service lookups.
2. **Strategy Pattern for Extensibility:** Utilizes dependency injection to automatically wire a collection of rule strategies. New fraud rules are implemented as self-contained component beans without altering core service orchestrator logic.
3. **Read-Only Data Isolation:** The Risk Service reads transaction and audit logs from external services via HTTP API contracts but has no permission to write to or alter those services' databases, enforcing strong domain boundary isolation.
4. **Cascading Failure Prevention:** Implements resilient Feign integration by falling back to empty histories during downstream service outages, guaranteeing that a failure in logging or auditing does not block normal user transaction flows.
