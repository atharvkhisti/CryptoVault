package com.cryptovault.kyc.exception;

import com.cryptovault.common.enums.ErrorCode;
import lombok.Getter;

/**
 * <h3>KycException</h3>
 *
 * <p><b>Why it exists:</b> Base exception for KYC operations carrying unified platform error codes.</p>
 */
@Getter
public class KycException extends RuntimeException {

    private final ErrorCode errorCode;

    public KycException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
