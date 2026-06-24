package com.cryptovault.notification.exception;

import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;

/**
 * <h3>NotificationNotFoundException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown when a requested notification cannot be found by its UUID identifier.</p>
 * <p><b>Architectural Layer:</b> Exception Layer.</p>
 * <p><b>Design Patterns Used:</b> Custom Exception Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Boundary verification (safely rejects requests targeting non-existent resources).</p>
 * <p><b>Future AWS Integration Path:</b> Propagates error logs if SQS listeners attempt updates on non-existent records.</p>
 * <p><b>Enterprise Relevance:</b> Enforces clear business boundary rules and custom status mappings.</p>
 * <p><b>Interview Talking Points:</b> Extends {@link BusinessException} to automatically translate into standard API response error payloads mapped to database query exclusions.</p>
 */
public class NotificationNotFoundException extends BusinessException {

    /**
     * Constructs a new NotificationNotFoundException.
     *
     * @param message error explanation
     */
    public NotificationNotFoundException(String message) {
        super(message, ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
