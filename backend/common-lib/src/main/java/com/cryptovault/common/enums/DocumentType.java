package com.cryptovault.common.enums;

/**
 * <h3>DocumentType</h3>
 *
 * <p><b>Why it exists:</b> Standardized list of government-issued identity documents allowed for KYC verification.</p>
 * <p><b>Architectural Layer:</b> Domain Model / Common Library Layer.</p>
 * <p><b>Design Patterns Used:</b> Type-Safe Enum Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Ensures only legal compliance-approved documents are accepted for verification, satisfying regulatory reporting guidelines.</p>
 * <p><b>Scalability Considerations:</b> Enables simple mapping to database strings and allows validation logic to be unified across gateway, kyc, and risk layers.</p>
 * <p><b>Interview Talking Points:</b> Enforces strict schema validations on uploaded files, rejecting unrecognized forms of ID at the entrypoint.</p>
 */
public enum DocumentType {
    PAN,
    AADHAAR,
    PASSPORT,
    DRIVING_LICENSE
}
