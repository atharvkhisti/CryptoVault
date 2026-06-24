package com.cryptovault.risk.exception;

import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;

/**
 * <h3>RiskAssessmentNotFoundException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown when a requested Risk Assessment resource does not exist in the database.</p>
 * <p><b>Architectural Layer:</b> Exception / Domain Layer.</p>
 * <p><b>Design Patterns Used:</b> Exception pattern subclassing.</p>
 * <p><b>Banking Relevance:</b> Prevents missing compliance records requests from crashing business streams, throwing clean code details.</p>
 * <p><b>Scalability Considerations:</b> Lightweight stack trace representation.</p>
 * <p><b>Interview Talking Points:</b> Extends <code>BusinessException</code> to assign unified system error codes (<code>ErrorCode.INTERNAL_SERVER_ERROR</code>).</p>
 */
public class RiskAssessmentNotFoundException extends BusinessException {

    public RiskAssessmentNotFoundException(String message) {
        super(message, ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
