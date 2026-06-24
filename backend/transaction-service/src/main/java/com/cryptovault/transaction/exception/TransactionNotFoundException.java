package com.cryptovault.transaction.exception;

import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;

/**
 * Custom exception thrown when a requested transaction is not found in the database.
 */
public class TransactionNotFoundException extends BusinessException {

    public TransactionNotFoundException(String message) {
        super(message, ErrorCode.TRANSACTION_NOT_FOUND);
    }
}
