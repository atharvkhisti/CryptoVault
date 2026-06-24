package com.cryptovault.common.enums;

/**
 * <h3>KycStatus</h3>
 *
 * <p><b>Why it exists:</b> Standardized lifecycle states for a user's identity verification (KYC) records.</p>
 * <p><b>Architectural Layer:</b> Domain Model / Common Library Layer.</p>
 * <p><b>Design Patterns Used:</b> Type-Safe Enum Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Necessary for anti-money laundering (AML) and know-your-customer (KYC) auditing. Prevents unverified users from performing wallet debits/credits.</p>
 * <p><b>Scalability Considerations:</b> Enables fast database-level lookups and query indexing since enum statuses map to standard DB strings.</p>
 * <p><b>Interview Talking Points:</b> Enforces a strict status machine (PENDING -> UNDER_REVIEW -> APPROVED/REJECTED/EXPIRED) globally across all platform services.</p>
 */
public enum KycStatus {
    PENDING,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    EXPIRED
}
