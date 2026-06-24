package com.cryptovault.common.exception;

import com.cryptovault.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Custom runtime exception class mapping structural business validations
 * to a corresponding {@link ErrorCode}.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Constructs a new BusinessException.
     *
     * @param message exception description
     * @param errorCode unified system error code reference
     */
    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
