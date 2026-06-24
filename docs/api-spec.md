# REST API Specification

This document details the public API endpoints exposed by **CryptoVault** microservices.

All responses are packaged in a generic response envelope wrapper:
```json
{
  "success": true,
  "message": "Response description message",
  "data": { ... },
  "timestamp": "2026-06-18T21:00:00.000"
}
```

---

## 0. Gateway Ingress & Security Headers

All external client traffic terminates at the **API Gateway** on Port `8080`.

### Ingress Routing Rules
The Gateway maps routes as follows:
- `/api/auth/**` → Auth Service (Port 8083)
- `/api/wallets/**` → Wallet Service (Port 8081)
- `/api/transactions/**` → Transaction Service (Port 8082)
- `/api/notifications/**` → Notification Service (Port 8084)
- `/api/risk/**` → Risk Service (Port 8085)
- `/api/audit/**` → Audit Service (Port 8086)
- `/api/kyc/**` → KYC Service (Port 8087)

### OpenAPI & Swagger Ingress Routes
The Gateway exposes the following routes for documentation access (public, no authentication required):
- `GET /swagger-ui/index.html` → Central Swagger dashboard UI aggregating all microservices.
- `GET /v3/api-docs` → API Gateway OpenAPI specification.
- `GET /api/auth/v3/api-docs` → Auth Service OpenAPI specification.
- `GET /api/wallets/v3/api-docs` → Wallet Service OpenAPI specification.
- `GET /api/transactions/v3/api-docs` → Transaction Service OpenAPI specification.
- `GET /api/notifications/v3/api-docs` → Notification Service OpenAPI specification.
- `GET /api/risk/v3/api-docs` → Risk Service OpenAPI specification.
- `GET /api/audit/v3/api-docs` → Audit Service OpenAPI specification.
- `GET /api/kyc/v3/api-docs` → KYC Service OpenAPI specification.

### Public Routes (No Authentication Required)
The following endpoints are public and do not require JWT validation:
- `POST /api/auth/register`
- `POST /api/auth/login`

### Protected Routes (JWT Required)
All other paths (e.g. `/api/wallets/**`, `/api/transactions/**`, `/api/auth/me`) require a valid bearer JWT in the Authorization header:
`Authorization: Bearer <JWT_TOKEN>`

### Injected Downstream Security Headers
Upon successful JWT validation at the Gateway, the token claims are extracted and injected as standard HTTP headers sent to downstream microservices:
- `X-USER-ID`: The user's UUID (e.g., `123e4567-e89b-12d3-a456-426614174000`)
- `X-USER-EMAIL`: The user's registration email (e.g., `user@cryptovault.com`)
- `X-USER-ROLE`: The user's role (e.g., `USER` or `ADMIN`)

Downstream microservices consume these headers directly to establish authentication contexts, avoiding network calls back to the `auth-service` or database queries for user credentials. The gateway acts as a strict trust boundary, stripping any user-submitted `X-USER-` headers from external client requests before forwarding.

---

## 1. Authentication Endpoints

### Register User
- **Endpoint:** `/api/auth/register`
- **Method:** `POST`
- **Body:** `RegisterRequest` (name, email, password)
- **Responses:** `201 Created` on success, `400` on validation errors, `409` on duplicate email conflicts.

### Authenticate / Login
- **Endpoint:** `/api/auth/login`
- **Method:** `POST`
- **Body:** `LoginRequest` (email, password)
- **Responses:** `200 OK` on success (returns `accessToken`), `400` on validation errors, `401` on invalid credentials.

### Get Current User Profile
- **Endpoint:** `/api/auth/me`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <token>`
- **Responses:** `200 OK` (returns user detail object), `401` on token auth failures.

### Get User Profile by ID (Internal / Service-to-Service)
- **Endpoint:** `/api/auth/{id}`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <token>`
- **Response Payload (`UserResponse` in data)**
  ```json
  {
    "success": true,
    "message": "User profile retrieved successfully",
    "data": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Alice Developer",
      "email": "alice@cryptovault.com",
      "role": "USER"
    },
    "timestamp": "2026-06-19T18:40:00.000"
  }
  ```
- **Responses:** `200 OK` on success, `401` on token auth failures, `404` if user is not found.

---

## 2. Wallet Endpoints

All endpoints require authentication headers: `Authorization: Bearer <token>`.

### Create Wallet
- **Endpoint:** `/api/wallets`
- **Method:** `POST`
- **Body (`CreateWalletRequest`)**
  ```json
  {
    "currency": "BTC"
  }
  ```
  *Allowed values:* `BTC`, `ETH`, `USDT`, `INR`

- **Response Payload (`WalletResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Wallet created successfully",
    "data": {
      "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
      "currency": "BTC",
      "balance": 0.000000000000000000
    },
    "timestamp": "2026-06-18T23:20:00.000"
  }
  ```

- **Potential Status Codes**
  - **`201 Created`**: Wallet successfully initialized.
  - **`400 Bad Request`**: Duplicate wallet validation failed (wallet already exists for currency) or validation errors (invalid currency).
  - **`401 Unauthorized`**: JWT context invalid or missing.

### Get All User Wallets
- **Endpoint:** `/api/wallets`
- **Method:** `GET`

- **Response Payload (`List<WalletResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "Wallets retrieved successfully",
    "data": [
      {
        "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
        "currency": "BTC",
        "balance": 1.250000000000000000
      },
      {
        "walletId": "80b33a59-122e-407b-a1bc-cd14c2b9a720",
        "currency": "ETH",
        "balance": 10.000000000000000000
      }
    ],
    "timestamp": "2026-06-18T23:22:00.000"
  }
  ```

---

### Deposit Funds
- **Endpoint:** `/api/wallets/deposit`
- **Method:** `POST`
- **Body (`DepositRequest`)**
  ```json
  {
    "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
    "amount": 0.5
  }
  ```
  *Constraints:* `amount` must be greater than zero.

- **Response Payload (`WalletResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Funds deposited successfully",
    "data": {
      "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
      "currency": "BTC",
      "balance": 1.750000000000000000
    },
    "timestamp": "2026-06-18T23:25:00.000"
  }
  ```

- **Potential Status Codes**
  - **`200 OK`**: Deposit successful.
  - **`400 Bad Request`**: Negative deposit amount or invalid parameters.
  - **`403 Forbidden`**: User is not authorized to deposit to this wallet (wallet belongs to another user).
  - **`404 Not Found`**: Wallet not found.

---

### Withdraw Funds
- **Endpoint:** `/api/wallets/withdraw`
- **Method:** `POST`
- **Body (`WithdrawRequest`)**
  ```json
  {
    "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
    "amount": 0.2
  }
  ```
  *Constraints:* `amount` must be greater than zero.

- **Response Payload (`WalletResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Funds withdrawn successfully",
    "data": {
      "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
      "currency": "BTC",
      "balance": 1.550000000000000000
    },
    "timestamp": "2026-06-18T23:28:00.000"
  }
  ```

- **Potential Status Codes**
  - **`200 OK`**: Withdrawal successful.
  - **`400 Bad Request`**: Insufficient balance (overdraft) or negative/zero withdrawal request amount.
  - **`403 Forbidden`**: User is not authorized to withdraw from this wallet (ownership mismatch).
  - **`404 Not Found`**: Wallet not found.

---

## 3. Transaction Endpoints

All endpoints require authentication headers: `Authorization: Bearer <token>`.

### Deposit Funds (via Transaction Service)
- **Endpoint:** `/api/transactions/deposit`
- **Method:** `POST`
- **Body (`DepositRequest`)**
  ```json
  {
    "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
    "amount": 100.0,
    "description": "ACH deposit"
  }
  ```
- **Response Payload (`TransactionResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Deposit transaction processed successfully",
    "data": {
      "transactionId": "d748f219-c817-48f2-89ee-180b91d24a01",
      "type": "DEPOSIT",
      "status": "COMPLETED",
      "amount": 100.0,
      "currency": "USDT",
      "referenceNumber": "TXN-DEPOSIT-171874...",
      "timestamp": "2026-06-19T00:02:00.000"
    },
    "timestamp": "2026-06-19T00:02:00.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Deposit processed.
  - **`400 Bad Request`**: Invalid request parameters (e.g. negative amount).
  - **`403 Forbidden`**: User is not authorized to deposit to this wallet (ownership mismatch).
  - **`404 Not Found`**: Wallet not found.

### Withdraw Funds (via Transaction Service)
- **Endpoint:** `/api/transactions/withdraw`
- **Method:** `POST`
- **Body (`WithdrawRequest`)**
  ```json
  {
    "walletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
    "amount": 50.0,
    "description": "Atm withdraw"
  }
  ```
- **Response Payload (`TransactionResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Withdrawal transaction processed successfully",
    "data": {
      "transactionId": "a748f219-c817-48f2-89ee-180b91d24a02",
      "type": "WITHDRAW",
      "status": "COMPLETED",
      "amount": 50.0,
      "currency": "USDT",
      "referenceNumber": "TXN-WITHDRAW-171874...",
      "timestamp": "2026-06-19T00:03:00.000"
    },
    "timestamp": "2026-06-19T00:03:00.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Withdrawal processed.
  - **`400 Bad Request`**: Insufficient balance, invalid/negative amount.
  - **`403 Forbidden`**: User is not authorized to withdraw from this wallet (ownership mismatch).
  - **`404 Not Found`**: Wallet not found.

### Transfer Funds (via Transaction Service)
- **Endpoint:** `/api/transactions/transfer`
- **Method:** `POST`
- **Body (`TransferRequest`)**
  ```json
  {
    "senderWalletId": "79b33a59-122e-407b-a1bc-cd14c2b9a710",
    "receiverWalletId": "90b33a59-122e-407b-a1bc-cd14c2b9a720",
    "amount": 25.0,
    "description": "Peer transfer"
  }
  ```
- **Response Payload (`TransactionResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Transfer transaction processed successfully",
    "data": {
      "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
      "type": "TRANSFER",
      "status": "COMPLETED",
      "amount": 25.0,
      "currency": "USDT",
      "referenceNumber": "TXN-TRANSFER-171874...",
      "timestamp": "2026-06-19T00:04:00.000"
    },
    "timestamp": "2026-06-19T00:04:00.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Transfer processed.
  - **`400 Bad Request`**:
    - Insufficient balance in sender's wallet.
    - Currency mismatch between sender and receiver wallets.
    - Self-transfer attempt (sender wallet ID equals receiver wallet ID).
    - Invalid request parameters (e.g. negative amount).
  - **`403 Forbidden`**: User does not own the sender wallet.
  - **`404 Not Found`**: Sender or receiver wallet not found.

### Get Transaction History
- **Endpoint:** `/api/transactions`
- **Method:** `GET`
- **Response Payload (`List<TransactionResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "Transactions retrieved successfully",
    "data": [
      {
        "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
        "type": "TRANSFER",
        "status": "COMPLETED",
        "amount": 25.0,
        "currency": "USDT",
        "referenceNumber": "TXN-TRANSFER-171874...",
        "timestamp": "2026-06-19T00:04:00.000"
      }
    ],
    "timestamp": "2026-06-19T00:05:00.000"
  }
  ```

### Get Transaction by ID
- **Endpoint:** `/api/transactions/{id}`
- **Method:** `GET`
- **Response Payload (`TransactionResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Transaction details retrieved successfully",
    "data": {
      "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
      "type": "TRANSFER",
      "status": "COMPLETED",
      "amount": 25.0,
      "currency": "USDT",
      "referenceNumber": "TXN-TRANSFER-171874...",
      "timestamp": "2026-06-19T00:04:00.000"
    },
    "timestamp": "2026-06-19T00:05:30.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Details retrieved.
  - **`403 Forbidden`**: User is not authorized to view this transaction (does not belong to them).
  - **`404 Not Found`**: Transaction not found.

---

## 4. Notification Endpoints

All endpoints require authentication headers (either service-to-service credentials or `Authorization: Bearer <token>`).

### Send Notification
- **Endpoint:** `/api/notifications/send`
- **Method:** `POST`
- **Body (`SendNotificationRequest`)**
  ```json
  {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "recipient@cryptovault.com",
    "type": "TRANSFER",
    "subject": "Fund Transfer Processed",
    "message": "amount=25.0,currency=USDT,referenceNumber=TXN-1234"
  }
  ```
  *Allowed types:* `REGISTRATION`, `LOGIN`, `WALLET_CREATED`, `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`, `KYC_APPROVED`, `KYC_REJECTED`, `RISK_ALERT`

- **Response Payload (`NotificationResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Notification sent successfully",
    "data": {
      "notificationId": "1a80d344-38ca-4752-ac41-af33dc890fd6",
      "status": "SENT",
      "sentAt": "2026-06-19T13:50:53.773"
    },
    "timestamp": "2026-06-19T13:50:53.773"
  }
  ```
- **Potential Status Codes**
  - **`201 Created`**: Notification request received and processed successfully.
  - **`400 Bad Request`**: Validation errors on body payload.
  - **`500 Internal Server Error`**: Failures to resolve template placeholders or SMTP server failures.

### Get User Notification History
- **Endpoint:** `/api/notifications/user/{userId}`
- **Method:** `GET`
- **Response Payload (`List<NotificationResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "User notifications retrieved successfully",
    "data": [
      {
        "notificationId": "1a80d344-38ca-4752-ac41-af33dc890fd6",
        "status": "SENT",
        "sentAt": "2026-06-19T13:50:53.773"
      }
    ],
    "timestamp": "2026-06-19T13:51:00.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: History retrieved successfully.
  - **`403 Forbidden`**: User is not authorized to retrieve these notifications.
  - **`404 Not Found`**: Notifications not found for this user.

### Get Notification by ID
- **Endpoint:** `/api/notifications/{id}`
- **Method:** `GET`
- **Response Payload (`NotificationResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Notification details retrieved successfully",
    "data": {
      "notificationId": "1a80d344-38ca-4752-ac41-af33dc890fd6",
      "status": "SENT",
      "sentAt": "2026-06-19T13:50:53.773"
    },
    "timestamp": "2026-06-19T13:51:30.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Notification details retrieved.
  - **`403 Forbidden`**: User is not authorized to view this notification.
  - **`404 Not Found`**: Notification not found.

### Get All Platform Notifications History
- **Endpoint:** `/api/notifications`
- **Method:** `GET`
- **Response Payload (`List<NotificationResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "All notifications retrieved successfully",
    "data": [
      {
        "notificationId": "1a80d344-38ca-4752-ac41-af33dc890fd6",
        "status": "SENT",
        "sentAt": "2026-06-19T13:50:53.773"
      }
    ],
    "timestamp": "2026-06-19T13:52:00.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: All logs retrieved successfully.
  - **`401 Unauthorized`**: Authentication missing or invalid.
  - **`403 Forbidden`**: Requester lacks permission.

---

## 5. Audit Endpoints

All endpoints require authentication headers (either service-to-service credentials or `Authorization: Bearer <token>`).

### Log Event
- **Endpoint:** `/api/audit`
- **Method:** `POST`
- **Body (`AuditEventRequest`)**
  ```json
  {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "eventType": "USER_LOGIN",
    "serviceName": "auth-service",
    "action": "USER_LOGIN",
    "description": "User logged in",
    "ipAddress": "127.0.0.1"
  }
  ```
  *Allowed event types:* `USER_REGISTERED`, `USER_LOGIN`, `USER_LOGOUT`, `PASSWORD_CHANGED`, `WALLET_CREATED`, `DEPOSIT`, `WITHDRAW`, `TRANSFER_INITIATED`, `TRANSFER_COMPLETED`, `TRANSFER_FAILED`, `KYC_SUBMITTED`, `KYC_APPROVED`, `KYC_REJECTED`, `BALANCE_UPDATED`, `ACCOUNT_FLAGGED`, `SECURITY_VIOLATION`, `API_ACCESS`, `RISK_ALERT_GENERATED`

- **Response Payload (`AuditResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Audit log entry created successfully",
    "data": {
      "auditId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "eventType": "USER_LOGIN",
      "action": "USER_LOGIN",
      "eventTimestamp": "2026-06-19T14:15:30.000"
    },
    "timestamp": "2026-06-19T14:15:30.000"
  }
  ```
- **Potential Status Codes**
  - **`201 Created`**: Audit log successfully recorded.
  - **`400 Bad Request`**: Validation errors on payload or missing headers.
  - **`401 Unauthorized`**: Authentication missing or invalid.
  - **`403 Forbidden`**: Requester lacks permission.

### Get Audit Log by ID
- **Endpoint:** `/api/audit/{id}`
- **Method:** `GET`
- **Response Payload (`AuditResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Audit log details retrieved successfully",
    "data": {
      "auditId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "eventType": "USER_LOGIN",
      "action": "USER_LOGIN",
      "eventTimestamp": "2026-06-19T14:15:30.000"
    },
    "timestamp": "2026-06-19T14:16:00.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Log details retrieved successfully.
  - **`404 Not Found`**: Audit log not found.

### Get All Audit Logs
- **Endpoint:** `/api/audit`
- **Method:** `GET`
- **Response Payload (`List<AuditResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "All audit logs retrieved successfully",
    "data": [
      {
        "auditId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "eventType": "USER_LOGIN",
        "action": "USER_LOGIN",
        "eventTimestamp": "2026-06-19T14:15:30.000"
      }
    ],
    "timestamp": "2026-06-19T14:16:30.000"
  }
  ```

### Get User Audit Logs
- **Endpoint:** `/api/audit/user/{userId}`
- **Method:** `GET`
- **Response Payload (`List<AuditResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "User audit logs retrieved successfully",
    "data": [
      {
        "auditId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "eventType": "USER_LOGIN",
        "action": "USER_LOGIN",
        "eventTimestamp": "2026-06-19T14:15:30.000"
      }
    ],
    "timestamp": "2026-06-19T14:17:00.000"
  }
  ```

### Get Audit Logs by Type
- **Endpoint:** `/api/audit/type/{eventType}`
- **Method:** `GET`
- **Response Payload (`List<AuditResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "Audit logs by type retrieved successfully",
    "data": [
      {
        "auditId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "eventType": "USER_LOGIN",
        "action": "USER_LOGIN",
        "eventTimestamp": "2026-06-19T14:15:30.000"
      }
    ],
    "timestamp": "2026-06-19T14:17:30.000"
  }
  ```

### Get Audit Logs by Date Range
- **Endpoint:** `/api/audit/date-range`
- **Method:** `GET`
- **Query Parameters:**
  - `start` (required, ISO date time string, e.g. `2026-06-19T00:00:00`)
  - `end` (required, ISO date time string, e.g. `2026-06-19T23:59:59`)
- **Response Payload (`List<AuditResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "Audit logs within date range retrieved successfully",
    "data": [
      {
        "auditId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "eventType": "USER_LOGIN",
        "action": "USER_LOGIN",
        "eventTimestamp": "2026-06-19T14:15:30.000"
      }
    ],
    "timestamp": "2026-06-19T14:18:00.000"
  }
  ```

---

## 6. Risk Endpoints

All endpoints require authentication headers (either service-to-service credentials or `Authorization: Bearer <token>`).

### Evaluate Risk
- **Endpoint:** `/api/risk/evaluate`
- **Method:** `POST`
- **Body (`EvaluateRiskRequest`)**
  ```json
  {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
    "amount": 250000.0,
    "currency": "INR",
    "type": "TRANSFER"
  }
  ```
  *Allowed currencies:* `BTC`, `ETH`, `USDT`, `INR`
  *Allowed types:* `DEPOSIT`, `WITHDRAW`, `TRANSFER`

- **Response Payload (`RiskResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Risk evaluation completed successfully",
    "data": {
      "id": "e748f219-c817-48f2-89ee-180b91d24a09",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
      "riskLevel": "CRITICAL",
      "status": "BLOCKED",
      "riskScore": 90,
      "triggeredRule": "HIGH_AMOUNT_RULE(+40), FREQUENT_TRANSFER_RULE(+30), FAILED_TRANSFER_RULE(+20)",
      "comments": "Rules triggered: HIGH_AMOUNT_RULE(+40), FREQUENT_TRANSFER_RULE(+30), FAILED_TRANSFER_RULE(+20)",
      "createdAt": "2026-06-19T18:35:00.000"
    },
    "timestamp": "2026-06-19T18:35:00.000"
  }
  ```
- **Potential Status Codes**
  - **`201 Created`**: Risk assessment completed.
  - **`400 Bad Request`**: Validation errors on payload or missing headers.
  - **`401 Unauthorized`**: Authentication missing or invalid.
  - **`403 Forbidden`**: Requester lacks permission.

### Get Risk Assessment by ID
- **Endpoint:** `/api/risk/{id}`
- **Method:** `GET`
- **Response Payload (`RiskResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Risk assessment details retrieved successfully",
    "data": {
      "id": "e748f219-c817-48f2-89ee-180b91d24a09",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
      "riskLevel": "CRITICAL",
      "status": "BLOCKED",
      "riskScore": 90,
      "triggeredRule": "HIGH_AMOUNT_RULE(+40), FREQUENT_TRANSFER_RULE(+30), FAILED_TRANSFER_RULE(+20)",
      "comments": "Rules triggered: HIGH_AMOUNT_RULE(+40), FREQUENT_TRANSFER_RULE(+30), FAILED_TRANSFER_RULE(+20)",
      "createdAt": "2026-06-19T18:35:00.000"
    },
    "timestamp": "2026-06-19T18:35:30.000"
  }
  ```
- **Potential Status Codes**
  - **`200 OK`**: Assessment details retrieved successfully.
  - **`404 Not Found`**: Risk assessment not found.

### Get User Risk History
- **Endpoint:** `/api/risk/user/{userId}`
- **Method:** `GET`
- **Response Payload (`List<RiskResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "User risk history retrieved successfully",
    "data": [
      {
        "id": "e748f219-c817-48f2-89ee-180b91d24a09",
        "userId": "123e4567-e89b-12d3-a456-426614174000",
        "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
        "riskLevel": "CRITICAL",
        "status": "BLOCKED",
        "riskScore": 90,
        "createdAt": "2026-06-19T18:35:00.000"
      }
    ],
    "timestamp": "2026-06-19T18:36:00.000"
  }
  ```

### Get All Assessments History
- **Endpoint:** `/api/risk/history`
- **Method:** `GET`
- **Response Payload (`List<RiskResponse>` in data)**
  ```json
  {
    "success": true,
    "message": "All risk assessments history retrieved successfully",
    "data": [
      {
        "id": "e748f219-c817-48f2-89ee-180b91d24a09",
        "userId": "123e4567-e89b-12d3-a456-426614174000",
        "transactionId": "b748f219-c817-48f2-89ee-180b91d24a03",
        "riskLevel": "CRITICAL",
        "status": "BLOCKED",
        "riskScore": 90,
        "createdAt": "2026-06-19T18:35:00.000"
      }
    ],
    "timestamp": "2026-06-19T18:36:30.000"
  }
  ```

---

## 7. KYC Endpoints

All endpoints require authentication headers: `Authorization: Bearer <token>`.

### Initialize KYC Record
- **Endpoint:** `/api/kyc`
- **Method:** `POST`
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "KYC record initialized successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "PENDING",
      "remarks": null,
      "submittedAt": null,
      "reviewedAt": null,
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": []
    },
    "timestamp": "2026-06-19T18:41:00.000"
  }
  ```
- **Potential Status Codes:**
  - **`201 Created`**: KYC record successfully initialized.
  - **`400 Bad Request`**: KYC record already initialized/exists for this user.
  - **`401 Unauthorized`**: JWT authentication token missing or invalid.

### Upload Identity Document
- **Endpoint:** `/api/kyc/upload`
- **Method:** `POST`
- **Request Parameters (Multipart Form Data):**
  - `file`: Binary file upload bounds (max 5MB, format JPEG/PNG/PDF).
  - `documentType`: Enumerated type (`PAN`, `AADHAAR`, `PASSPORT`, `DRIVING_LICENSE`).
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "Document uploaded successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "PENDING",
      "remarks": null,
      "submittedAt": null,
      "reviewedAt": null,
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": [
        {
          "documentId": "2b2b2b2b-3c3c-4d4d-5e5e-6f6f6f6f6f6f",
          "documentType": "PAN",
          "fileName": "pan_card.jpg",
          "mimeType": "image/jpeg",
          "fileSize": 1048576,
          "uploadedAt": "2026-06-19T18:41:30.000"
        }
      ]
    },
    "timestamp": "2026-06-19T18:41:30.000"
  }
  ```
- **Potential Status Codes:**
  - **`200 OK`**: File uploaded successfully.
  - **`400 Bad Request`**: Invalid file type, file size bounds exceeded, or duplicate upload for document type.
  - **`404 Not Found`**: KYC record not initialized for the user.

### Submit KYC for Review
- **Endpoint:** `/api/kyc/submit`
- **Method:** `POST`
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "KYC submitted for review successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "UNDER_REVIEW",
      "remarks": null,
      "submittedAt": "2026-06-19T18:42:00.000",
      "reviewedAt": null,
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": [
        {
          "documentId": "2b2b2b2b-3c3c-4d4d-5e5e-6f6f6f6f6f6f",
          "documentType": "PAN",
          "fileName": "pan_card.jpg",
          "mimeType": "image/jpeg",
          "fileSize": 1048576,
          "uploadedAt": "2026-06-19T18:41:30.000"
        },
        {
          "documentId": "3c3c3c3c-4d4d-5e5e-6f6f-7g7g7g7g7g7g",
          "documentType": "AADHAAR",
          "fileName": "aadhaar_card.pdf",
          "mimeType": "application/pdf",
          "fileSize": 2097152,
          "uploadedAt": "2026-06-19T18:41:45.000"
        }
      ]
    },
    "timestamp": "2026-06-19T18:42:00.000"
  }
  ```
- **Potential Status Codes:**
  - **`200 OK`**: Submission successful.
  - **`400 Bad Request`**: Validation error (missing required `PAN` or `AADHAAR` documents) or KYC record is not in `PENDING`/`REJECTED` state.
  - **`404 Not Found`**: KYC record not found for the user.

### Approve KYC (Admin Only)
- **Endpoint:** `/api/kyc/approve`
- **Method:** `POST`
- **Request Parameters:**
  - `kycId`: UUID of the KYC record.
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "KYC approved successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "APPROVED",
      "remarks": null,
      "submittedAt": "2026-06-19T18:42:00.000",
      "reviewedAt": "2026-06-19T18:42:30.000",
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": [...]
    },
    "timestamp": "2026-06-19T18:42:30.000"
  }
  ```
- **Potential Status Codes:**
  - **`200 OK`**: Approved successfully.
  - **`400 Bad Request`**: KYC record is not in `UNDER_REVIEW` status.
  - **`403 Forbidden`**: User is not an admin (requires `ROLE_ADMIN`).
  - **`404 Not Found`**: KYC record not found.

### Reject KYC (Admin Only)
- **Endpoint:** `/api/kyc/reject`
- **Method:** `POST`
- **Request Parameters:**
  - `kycId`: UUID of the KYC record.
  - `remarks`: Reason description for rejection.
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "KYC rejected successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "REJECTED",
      "remarks": "PAN photo is blurry, please re-upload clear image.",
      "submittedAt": "2026-06-19T18:42:00.000",
      "reviewedAt": "2026-06-19T18:42:45.000",
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": [...]
    },
    "timestamp": "2026-06-19T18:42:45.000"
  }
  ```
- **Potential Status Codes:**
  - **`200 OK`**: Rejected successfully.
  - **`400 Bad Request`**: KYC record is not in `UNDER_REVIEW` status.
  - **`403 Forbidden`**: User lacks `ROLE_ADMIN` permissions.
  - **`404 Not Found`**: KYC record not found.

### Get KYC Status of a User
- **Endpoint:** `/api/kyc/status/{userId}`
- **Method:** `GET`
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "KYC status retrieved successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "APPROVED",
      "remarks": null,
      "submittedAt": "2026-06-19T18:42:00.000",
      "reviewedAt": "2026-06-19T18:42:30.000",
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": [...]
    },
    "timestamp": "2026-06-19T18:43:00.000"
  }
  ```
- **Potential Status Codes:**
  - **`200 OK`**: KYC record retrieved successfully.
  - **`401 Unauthorized`**: Token missing or invalid.
  - **`403 Forbidden`**: User is not authorized to fetch another user's status (unless they are `ADMIN`).
  - **`404 Not Found`**: KYC record not found for this user.

### Get KYC Record Details by ID
- **Endpoint:** `/api/kyc/{id}`
- **Method:** `GET`
- **Response Payload (`KycResponse` in data)**
  ```json
  {
    "success": true,
    "message": "KYC record details retrieved successfully",
    "data": {
      "kycId": "1a1a1a1a-2b2b-3c3c-4d4d-5e5e5e5e5e5e",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "status": "APPROVED",
      "remarks": null,
      "submittedAt": "2026-06-19T18:42:00.000",
      "reviewedAt": "2026-06-19T18:42:30.000",
      "createdAt": "2026-06-19T18:41:00.000",
      "documents": [...]
    },
    "timestamp": "2026-06-19T18:43:30.000"
  }
  ```
- **Potential Status Codes:**
  - **`200 OK`**: Details retrieved successfully.
  - **`401 Unauthorized`**: Token missing or invalid.
  - **`403 Forbidden`**: User is not authorized to view another user's record (unless they are `ADMIN`).
  - **`404 Not Found`**: KYC record not found.

