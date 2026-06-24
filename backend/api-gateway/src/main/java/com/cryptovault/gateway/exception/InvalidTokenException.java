package com.cryptovault.gateway.exception;

import com.cryptovault.common.exception.BusinessException;
import com.cryptovault.common.enums.ErrorCode;

/**
 * <h3>InvalidTokenException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown specifically when a JSON Web Token is expired, invalidly signed, or malformed.</p>
 * <p><b>Architectural Layer:</b> Exception/Security Layer.</p>
 * <p><b>Design Patterns Used:</b> Custom Exception Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Cryptographic payload integrity validation failure.</p>
 * <p><b>Enterprise Relevance:</b> Differentiates expired or structural token issues from missing credentials, allowing specific handling (e.g. prompt refresh token logic).</p>
 * <p><b>Interview Talking Points:</b> Separation of concerns in exception design; separating token lifecycle failures from broad access authorization blocks.</p>
 */
public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(message, ErrorCode.UNAUTHORIZED_ACCESS);
    }
}
