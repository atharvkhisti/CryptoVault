# Developer Guide: Transaction Service Walkthrough

Welcome to the Developer Guide for the **CryptoVault Transaction Service**. This microservice functions as the financial ledger and lifecycle manager for all asset transfers, deposits, and withdrawals across the platform.

---

## 1. Context & Architecture

The Transaction Service does not own user balances directly. Instead, it maintains a strict transaction history ledger and orchestrates adjustments to user balances by calling the **Wallet Service** via REST endpoints.

```
                  ┌────────────────────────┐
                  │  Transaction Service   │
                  │      (Port 8082)       │
                  └───────────┬────────────┘
                              │
                              │ Feign Client HTTP Calls
                              ▼
                  ┌────────────────────────┐
                  │     Wallet Service     │
                  │      (Port 8081)       │
                  └────────────────────────┘
```

### Key Design Principles:
- **Separation of Concerns:** Wallets and balances are owned by the Wallet Service. Transactions and audits are owned by the Transaction Service.
- **Stateless Authorization:** The service decodes incoming JWT claims locally using shared secrets to establish caller security contexts.
- **ACID Integrity in Distributed Systems:** As standard database transactions cannot span network calls, we implement compensating saga rollbacks to keep balances consistent.

---

## 2. Distributed ACID & Saga Rollback Pattern

In a monolithic application, transferring funds between two wallets is done inside a single database transaction:
```java
// Monolithic pseudo-code
@Transactional
public void transfer(UUID from, UUID to, BigDecimal amount) {
    walletRepo.debit(from, amount);
    walletRepo.credit(to, amount);
    // Any failure rolls back both updates automatically
}
```

In our microservices architecture, the Transaction Service must call the Wallet Service twice via HTTP:
1. **Debit** the sender wallet.
2. **Credit** the receiver wallet.

If step 2 fails due to a network timeout or system crash, the sender's funds have already been debited, leaving the system in an inconsistent state. To solve this, the `TransactionService` implements a **Compensating Action**:

```java
// com.cryptovault.transaction.service.TransactionService
public TransactionResponse transfer(UUID userId, TransferRequest request) {
    // 1. Initialize pending transaction in DB
    Transaction txn = transactionRepository.save(new Transaction(...));

    // 2. Perform validation (e.g., wallet ownership and currency checks)
    ...

    // 3. Debit Sender
    walletClient.debitWallet(request.getSenderWalletId(), request.getAmount());

    try {
        // 4. Credit Receiver
        walletClient.creditWallet(request.getReceiverWalletId(), request.getAmount());
        
        // 5. Complete transaction
        txn.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(txn);
    } catch (Exception e) {
        // 6. COMPENSATING ROLLBACK: Credit sender to refund debited amount
        log.error("Failed to credit receiver wallet. Attempting rollback of sender debit.", e);
        try {
            walletClient.creditWallet(request.getSenderWalletId(), request.getAmount());
        } catch (Exception rollbackException) {
            log.error("CRITICAL: Rollback refund failed for wallet " + request.getSenderWalletId(), rollbackException);
        }
        
        // 7. Update status to FAILED
        txn.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(txn);
        
        throw new InvalidTransactionException(ErrorCode.INVALID_TRANSACTION, "Transaction credit failed, refunded sender.");
    }
}
```

---

## 3. Security Design & Token Propagation

### Stateless Authorization
The Transaction Service intercepts requests using `JwtUserInterceptorFilter`. It:
1. Extracts the bearer token from the `Authorization` header.
2. Cryptographically validates the HMAC-SHA256 signature using the shared secret.
3. Populates the `SecurityContextHolder` with `JwtUserPrincipal` claims context.

### JWT Context Propagation via Feign
When calling the Wallet Service, we must forward the authenticated user's credentials so the Wallet Service can authorize the debit/credit changes. We do this transparently using a Feign `RequestInterceptor`:

```java
// com.cryptovault.transaction.config.FeignClientConfig
@Bean
public RequestInterceptor requestTokenBearerInterceptor() {
    return requestTemplate -> {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String authHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                requestTemplate.header(HttpHeaders.AUTHORIZATION, authHeader);
            }
        }
    };
}
```

---

## 4. Testing Strategies & Verification

To verify the transaction engine under Spring Boot 4.x, we split testing into three tiers:

### 1. Plain Mockito Service Tests (`TransactionServiceTest.java`)
- Bypasses the Spring boot context to test pure business logic rules.
- Asserts successful transfers, withdrawals, and deposits.
- Validates exception conditions (self-transfers, insufficient balances, currency mismatch).
- Specifically tests the **Compensating Rollback** flow by mocking credit failures and verifying that a refund credit call is sent back to the sender.

### 2. Spring WebMvc Slice Tests (`TransactionControllerTest.java`)
- Focuses exclusively on the API presentation layer using MockMvc.
- Asserts that REST endpoints map correct URL routes (e.g., `/api/transactions/transfer`).
- Verifies input validation constraints (`@NotNull`, `@DecimalMin` for request bodies).
- *Spring Boot 4.x Note:* Test slices now use the updated package path `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and require mock beans declared via `@MockitoBean` instead of deprecated annotations.

### 3. Application Smoke Tests (`TransactionServiceApplicationTests.java`)
- Verifies that the complete ApplicationContext bootstrap compiles and initializes with database profiles without warnings.

---

## 5. Technical Interview Talking Points

If discussing this project in a technical review (e.g., Barclays or major financial firm), focus on these core design implementations:

1. **Modular Spring Boot 4.x Starters:** "In this design, we utilized Spring Boot 4.x's modular test starters (such as `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test`). We adapted our controller slice tests to match the updated package layouts where MVC test annotations reside under `org.springframework.boot.webmvc.test.autoconfigure` rather than legacy paths."
2. **Saga Orchestration & Eventual Consistency:** "In microservice architectures, HTTP calls can fail midway. To guarantee consistency without introducing heavy distributed locking (like 2PC), we utilized synchronous Feign execution with local compensating transactions. If the receiver's credit fails, we catch the exception and refund the sender's wallet, maintaining audit trails in our transaction service database."
3. **Monetary Precision:** "We represent all monetary balances and transaction amounts using arbitrary-precision `BigDecimal` scale structures instead of standard floats. This prevents binary floating-point rounding errors and allows us to scale up to 18 decimal places for crypto assets like Ethereum."
4. **JWT Context Delegation:** "We designed a zero-lookup authorization scheme. The gateway forwards JWTs, which our services validate signature-wise in-memory. Downstream propagation is handled by a Feign interceptor copying bearer authorization tokens into remote headers, avoiding redundant database calls for user profile checking."
