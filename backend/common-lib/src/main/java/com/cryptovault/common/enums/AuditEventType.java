package com.cryptovault.common.enums;

/**
 * <h3>AuditEventType</h3>
 *
 * <p><b>Why it exists:</b> Enumeration cataloging all compliance and system security events recorded within the platform.</p>
 * <p><b>Architectural Layer:</b> Domain Enums / Common Domain Layer.</p>
 * <p><b>Compliance Relevance:</b> Serves as the central registry of auditable actions required for SOX/HIPAA/GDPR alignment.</p>
 * <p><b>Event-Driven Integration Path:</b> Included in outbound SQS/SNS event payloads to route message consumers based on transaction classification.</p>
 * <p><b>Enterprise Patterns Used:</b> Type-Safe Enum Pattern.</p>
 * <p><b>Interview Talking Points:</b> Centralizes microservices-wide event taxonomies, guaranteeing consistent messaging types across gateway, auth, risk, and transaction domains.</p>
 */
public enum AuditEventType {
    // Authentication
    USER_REGISTERED,
    USER_LOGIN,
    USER_LOGOUT,
    PASSWORD_CHANGED,

    // Wallet
    WALLET_CREATED,
    DEPOSIT,
    WITHDRAW,
    BALANCE_UPDATED,

    // Transaction
    TRANSFER_INITIATED,
    TRANSFER_COMPLETED,
    TRANSFER_FAILED,

    // KYC
    KYC_SUBMITTED,
    KYC_APPROVED,
    KYC_REJECTED,

    // Risk
    RISK_ALERT_GENERATED,
    ACCOUNT_FLAGGED,

    // System
    API_ACCESS,
    SECURITY_VIOLATION
}
