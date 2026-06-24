package com.cryptovault.transaction.exception;

import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;

/**
 * Custom exception thrown when a transaction payload violates validation constraints (e.g. self transfer, negative amount).
 */
public class InvalidTransactionException extends BusinessException {

    public InvalidTransactionException(String message) {
        super(message, ErrorCode.INVALID_TRANSACTION);
    }
}
