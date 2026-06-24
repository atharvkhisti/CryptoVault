package com.cryptovault.common.enums;

/**
 * Centralized enumeration representing error codes across all microservices
 * to maintain consistent error handling structures.
 */
public enum ErrorCode {

    USER_NOT_FOUND,
    WALLET_NOT_FOUND,
    TRANSACTION_NOT_FOUND,
    INSUFFICIENT_BALANCE,
    INVALID_TRANSACTION,
    UNAUTHORIZED_ACCESS,
    INTERNAL_SERVER_ERROR,
    KYC_RECORD_NOT_FOUND,
    DOCUMENT_NOT_FOUND,
    MANDATORY_DOCUMENTS_MISSING,
    INVALID_FILE_TYPE,
    FILE_SIZE_EXCEEDED,
    DUPLICATE_DOCUMENT,
    INVALID_KYC_STATUS
}
