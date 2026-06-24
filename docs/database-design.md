# Database Design Document

This document lists database schema details, constraint definitions, index optimizations, and data ownership rules for **CryptoVault**.

## 1. Authentication Service Database (`cryptovault` database)

### Table: `users`
Represents user credential registries and authorization profile configurations.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique user identity key generated automatically. |
| `name` | `VARCHAR(255)` | `NOT NULL` | Display name of the user. |
| `email` | `VARCHAR(255)` | `UNIQUE`, `NOT NULL` | Registration email address, used for login verification. |
| `password` | `VARCHAR(255)` | `NOT NULL` | Hashed credential (encoded using BCrypt). |
| `role` | `VARCHAR(50)` | `NOT NULL` | Authorization profile mapping (`USER`, `ADMIN`). |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Record creation timestamp. |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Last modifications timestamp. |

---

## 2. Wallet Service Database (`cryptovault` database / isolated schema)

### Table: `wallets`
Represents user wallets containing asset balances and asset type definitions.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique wallet identifier. |
| `user_id` | `UUID` | `NOT NULL` | Owner's user identifier (references the user context). |
| `currency` | `VARCHAR(50)` | `NOT NULL` | Crypto/Fiat asset classification (`BTC`, `ETH`, `USDT`, `INR`). |
| `balance` | `NUMERIC(38,18)` | `NOT NULL`, `CHECK (balance >= 0)` | Accurate asset ledger balance (18 decimal places of precision). |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Timestamp when the wallet was initialized. |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Timestamp when balance was last modified. |

*Unique Constraint:* Combined index on `(user_id, currency)` ensures a user can never hold multiple wallets for the same asset type.

---

## 3. Transaction Service Database (`cryptovault` database / isolated schema)

### Table: `transactions`
Represents the historical ledger of financial events (deposits, withdrawals, and transfers).

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique transaction ledger ID generated automatically. |
| `user_id` | `UUID` | `NOT NULL` | The user executing the transaction. |
| `type` | `VARCHAR(50)` | `NOT NULL` | Transaction type (`DEPOSIT`, `WITHDRAW`, `TRANSFER`). |
| `status` | `VARCHAR(50)` | `NOT NULL` | Transaction state (`PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`). |
| `amount` | `NUMERIC(38,18)` | `NOT NULL`, `CHECK (amount > 0)` | Accurate asset amount (18 decimal places of precision). |
| `currency` | `VARCHAR(50)` | `NOT NULL` | Currency of the asset (`BTC`, `ETH`, `USDT`, `INR`). |
| `sender_wallet_id` | `UUID` | Nullable | Source wallet ID (null for DEPOSIT). |
| `receiver_wallet_id` | `UUID` | Nullable | Target wallet ID (null for WITHDRAW). |
| `reference_number` | `VARCHAR(100)` | `UNIQUE`, `NOT NULL` | Business reference number (e.g., `TXN-171874...`). |
| `description` | `VARCHAR(255)` | Nullable | Custom transaction note. |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Timestamp when transaction was created. |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Timestamp of the last status update. |

---

## 4. Notification Service Database (`cryptovault` database / isolated schema)

### Table: `notifications`
Represents persistent audit logs of notification delivery events.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique notification record identifier. |
| `user_id` | `UUID` | `NOT NULL` | Reference key to the user's account. |
| `recipient_email` | `VARCHAR(255)` | `NOT NULL` | Target email address the notification was sent to. |
| `type` | `VARCHAR(50)` | `NOT NULL` | Category of alert (`REGISTRATION`, `LOGIN`, `WALLET_CREATED`, `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`, `KYC_APPROVED`, `KYC_REJECTED`, `RISK_ALERT`). |
| `subject` | `VARCHAR(255)` | `NOT NULL` | Email subject header line. |
| `body` | `TEXT` | `NOT NULL` | Processed HTML message payload body. |
| `status` | `VARCHAR(50)` | `NOT NULL` | Delivery state (`PENDING`, `SENT`, `FAILED`). |
| `error_message` | `TEXT` | Nullable | Detail trace message logged if mail delivery fails. |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | When the alert request was first logged. |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Last update timestamp (status change). |

---

## 5. Audit Service Database (`cryptovault` database / isolated schema)

### Table: `audit_logs`
Represents immutable security, transaction, and system event compliance log registries.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique audit log entry ID. |
| `user_id` | `UUID` | Nullable | ID of the user associated with the event (if applicable). |
| `event_type` | `VARCHAR(50)` | `NOT NULL`, `UPDATABLE = FALSE` | Event classification (e.g. `USER_LOGIN`, `TRANSFER_COMPLETED`). |
| `service_name` | `VARCHAR(255)` | `NOT NULL`, `UPDATABLE = FALSE` | The microservice triggering the event (e.g. `auth-service`). |
| `action` | `VARCHAR(255)` | `NOT NULL`, `UPDATABLE = FALSE` | High-level user or system action description. |
| `description` | `VARCHAR(1000)` | `NOT NULL`, `UPDATABLE = FALSE` | Detailed metadata regarding the action. |
| `ip_address` | `VARCHAR(255)` | `NOT NULL`, `UPDATABLE = FALSE` | IP address of the user or trigger source. |
| `performed_by` | `VARCHAR(255)` | `NOT NULL`, `UPDATABLE = FALSE` | Subject who triggered the event (email or `SYSTEM`). |
| `event_timestamp` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Epoch timestamp of when the event occurred. |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Record creation timestamp. |

---

## 6. Risk Service Database (`cryptovault` database / isolated schema)

### Table: `risk_assessments`
Represents compliance and fraud risk assessment logs.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique risk assessment record ID. |
| `user_id` | `UUID` | `NOT NULL` | Unique user ID evaluated. |
| `transaction_id` | `UUID` | Nullable | Associated transaction ID (if transaction risk evaluation). |
| `risk_level` | `VARCHAR(50)` | `NOT NULL` | Evaluated risk tier (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). |
| `status` | `VARCHAR(50)` | `NOT NULL` | Mitigation state (`PENDING`, `APPROVED`, `FLAGGED`, `BLOCKED`). |
| `risk_score` | `INTEGER` | `NOT NULL` | Numerical aggregated risk score (0 to 100). |
| `triggered_rule` | `VARCHAR(255)` | Nullable | Comma-separated list of triggered rules. |
| `comments` | `VARCHAR(1000)` | Nullable | Explanation comments/description. |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Record creation timestamp. |

---

## 7. KYC Service Database (`cryptovault` database / isolated schema)

### Table: `kyc_records`
Represents the user compliance verification status record.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique KYC record ID. |
| `user_id` | `UUID` | `UNIQUE`, `NOT NULL` | The user ID associated with this KYC registration. |
| `status` | `VARCHAR(50)` | `NOT NULL` | Current KYC lifecycle state (`PENDING`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `EXPIRED`). |
| `remarks` | `VARCHAR(1000)` | Nullable | Explanations regarding rejections or validations. |
| `submitted_at` | `TIMESTAMP` | Nullable | Timestamp when documents were submitted for review. |
| `reviewed_at` | `TIMESTAMP` | Nullable | Timestamp when administrative decision was taken. |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | When the KYC record was first initialized. |
| `updated_at` | `TIMESTAMP` | Nullable | Last modification timestamp. |

### Table: `kyc_documents`
Represents metadata files associated with a KYC record.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique document ID. |
| `kyc_record_id` | `UUID` | `NOT NULL` | Foreign key referencing `kyc_records` (`id`). |
| `document_type` | `VARCHAR(50)` | `NOT NULL` | Document classification (`PAN`, `AADHAAR`, `PASSPORT`, `DRIVING_LICENSE`). |
| `file_name` | `VARCHAR(255)` | `NOT NULL` | Original filename uploaded. |
| `file_path` | `VARCHAR(255)` | `NOT NULL` | Path to storage destination (local directory path or S3 key). |
| `mime_type` | `VARCHAR(100)` | `NOT NULL` | MIME type format (`image/jpeg`, `image/png`, `application/pdf`). |
| `file_size` | `BIGINT` | `NOT NULL` | Uploaded file size bounds in bytes. |
| `uploaded_at` | `TIMESTAMP` | `NOT NULL`, `UPDATABLE = FALSE` | Document upload timestamp. |

---

## 8. Table Ownership and Modification Rules

- **`users` Table:**
  - **Service Owner:** `auth-service`
  - **Access Rule:** Only `auth-service` database layers query and write directly to this table.
- **`wallets` Table:**
  - **Service Owner:** `wallet-service`
  - **Access Rule:** Only `wallet-service` reads and modifies wallet records. Other services request operations via REST APIs.
- **`transactions` Table:**
  - **Service Owner:** `transaction-service`
  - **Access Rule:** Only `transaction-service` reads and modifies transaction history records.
- **`notifications` Table:**
  - **Service Owner:** `notification-service`
  - **Access Rule:** Only `notification-service` reads and modifies notification records. Other services request creation via API Gateway or backend calls.
- **`audit_logs` Table:**
  - **Service Owner:** `audit-service`
  - **Access Rule:** Only `audit-service` has access. Writes are strictly APPEND-ONLY. Updates or deletions are blocked at the JPA entity layer via lifecycle interceptors and are database-level forbidden.
- **`risk_assessments` Table:**
  - **Service Owner:** `risk-service`
  - **Access Rule:** Only `risk-service` reads and writes risk assessment profiles logs.
- **`kyc_records` & `kyc_documents` Tables:**
  - **Service Owner:** `kyc-service`
  - **Access Rule:** Only `kyc-service` database layers read and write directly to these tables. File metadata is saved in databases, and raw files are delegated to strategy configurations.

---

## 9. Database Index Recommendations

### Index: `idx_users_email`
- **Target Table:** `users`
- **Target Column:** `email`
- **Index Type:** B-Tree
- **SQL:** `CREATE UNIQUE INDEX idx_users_email ON users(email);`

### Index: `idx_wallets_user_id`
- **Target Table:** `wallets`
- **Target Column:** `user_id`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_wallets_user_id ON wallets(user_id);`
- **Rationale:** Speeds up operations requesting wallet list lookups for a specific user ID (e.g., retrieving the user's dashboard balances).

### Index: `idx_wallets_user_currency`
- **Target Table:** `wallets`
- **Target Columns:** `(user_id, currency)`
- **Index Type:** B-Tree (Unique)
- **SQL:** `CREATE UNIQUE INDEX idx_wallets_user_currency ON wallets(user_id, currency);`
- **Rationale:** Essential for quick execution of unique checks during wallet creation and quick resource lookup during deposits and withdrawals.

### Index: `idx_transactions_user_id`
- **Target Table:** `transactions`
- **Target Column:** `user_id`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_transactions_user_id ON transactions(user_id);`
- **Rationale:** Crucial for retrieving a user's transaction history records rapidly.

### Index: `idx_transactions_ref_num`
- **Target Table:** `transactions`
- **Target Column:** `reference_number`
- **Index Type:** B-Tree (Unique)
- **SQL:** `CREATE UNIQUE INDEX idx_transactions_ref_num ON transactions(reference_number);`
- **Rationale:** Quick query lookup of a specific transaction based on its unique business identifier reference.

### Index: `idx_transactions_created_at_desc`
- **Target Table:** `transactions`
- **Target Columns:** `(user_id, created_at DESC)`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_transactions_created_at_desc ON transactions(user_id, created_at DESC);`
- **Rationale:** Optimizes performance for paginated lists of transactions sorted from newest to oldest.

### Index: `idx_notifications_user_created`
- **Target Table:** `notifications`
- **Target Columns:** `(user_id, created_at DESC)`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);`
- **Rationale:** Optimizes paginated history lookups for notification delivery events for a specific user.

### Index: `idx_audit_logs_user_id`
- **Target Table:** `audit_logs`
- **Target Column:** `user_id`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);`
- **Rationale:** Optimizes compliance queries filtering log records by a specific user identity.

### Index: `idx_audit_logs_event_type`
- **Target Table:** `audit_logs`
- **Target Column:** `event_type`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);`
- **Rationale:** Facilitates rapid querying and aggregation of compliance audits grouped by specific categories (e.g. security violations).

### Index: `idx_audit_logs_service_name`
- **Target Table:** `audit_logs`
- **Target Column:** `service_name`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_audit_logs_service_name ON audit_logs(service_name);`
- **Rationale:** Enables administrators to review and aggregate system events generated by specific backend services.

### Index: `idx_audit_logs_event_timestamp`
- **Target Table:** `audit_logs`
- **Target Columns:** `event_timestamp DESC`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_audit_logs_event_timestamp ON audit_logs(event_timestamp DESC);`
- **Rationale:** Crucial for retrieving recent audit history sorted chronologically for forensics investigation and compliance dashboard listings.

### Index: `idx_risk_assessments_user_id`
- **Target Table:** `risk_assessments`
- **Target Column:** `user_id`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_risk_assessments_user_id ON risk_assessments(user_id);`
- **Rationale:** Speeds up user history risk profiling checks.

### Index: `idx_risk_assessments_transaction_id`
- **Target Table:** `risk_assessments`
- **Target Column:** `transaction_id`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_risk_assessments_transaction_id ON risk_assessments(transaction_id);`
- **Rationale:** Speeds up transaction risk evaluation lookups.

### Index: `idx_risk_assessments_user_created`
- **Target Table:** `risk_assessments`
- **Target Columns:** `(user_id, created_at DESC)`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_risk_assessments_user_created ON risk_assessments(user_id, created_at DESC);`
- **Rationale:** Optimizes paginated list lookups of user historical fraud scores.

### Index: `idx_kyc_records_user_id`
- **Target Table:** `kyc_records`
- **Target Column:** `user_id`
- **Index Type:** B-Tree (Unique)
- **SQL:** `CREATE UNIQUE INDEX idx_kyc_records_user_id ON kyc_records(user_id);`
- **Rationale:** Ensures quick lookup of a user's compliance/verification records and enforces 1-to-1 constraint in the database.

### Index: `idx_kyc_documents_record_id`
- **Target Table:** `kyc_documents`
- **Target Column:** `kyc_record_id`
- **Index Type:** B-Tree
- **SQL:** `CREATE INDEX idx_kyc_documents_record_id ON kyc_documents(kyc_record_id);`
- **Rationale:** Fast mapping and integrity checks of uploaded document lists associated with a specific KYC record.

---

## 10. API Gateway Database Isolation Policy

- **Owned Tables:** None.
- **Datasource Settings:** The `api-gateway` contains **no** JPA, JDBC, Hibernate, or datasource configurations.
- **Database Access Policy:** The gateway is strictly forbidden from opening connections to any platform database. It remains 100% stateless to maximize horizontal scalability, minimize edge latency, and limit attack surfaces.