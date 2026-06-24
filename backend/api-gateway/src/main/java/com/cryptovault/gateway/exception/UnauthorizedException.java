package com.cryptovault.gateway.exception;

import com.cryptovault.common.exception.BusinessException;
import com.cryptovault.common.enums.ErrorCode;

/**
 * <h3>UnauthorizedException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown when a request lacks authentication credentials or has insufficient privileges.</p>
 * <p><b>Architectural Layer:</b> Exception/Security Layer.</p>
 * <p><b>Design Patterns Used:</b> Custom Exception Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Entry point authorization enforcement and failure signaling.</p>
 * <p><b>Enterprise Relevance:</b> Restricts access to downstream services by signaling unauthenticated states early at the edge.</p>
 * <p><b>Interview Talking Points:</b> Standardizes security-related HTTP status code mappings. It inherits from {@link BusinessException} to automatically bind with the common-lib error translation system.</p>
 */
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED_ACCESS);
    }
}
