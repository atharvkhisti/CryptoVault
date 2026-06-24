package com.cryptovault.transaction.exception;

import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;

/**
 * Custom exception thrown when a wallet has insufficient funds to execute a withdrawal or transfer.
 */
public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String message) {
        super(message, ErrorCode.INSUFFICIENT_BALANCE);
    }
}
