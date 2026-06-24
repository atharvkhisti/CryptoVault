package com.cryptovault.audit.exception;

import java.util.UUID;

/**
 * <h3>AuditLogNotFoundException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown when an audit log entry cannot be located by its UUID.</p>
 * <p><b>Architectural Layer:</b> Exception / Business Domain Layer.</p>
 * <p><b>Compliance Relevance:</b> Signals log retrieval failures, helping system auditors track invalid search bounds.</p>
 * <p><b>Event-Driven Integration Path:</b> Thrown synchronously when queries are made on logs; can trigger alerts for unusual queries.</p>
 * <p><b>Enterprise Patterns Used:</b> Custom Domain Exception Pattern.</p>
 * <p><b>Interview Talking Points:</b> Extends a standard runtime exception and explicitly references the missing audit log UUID, feeding into global MVC exception translators.</p>
 */
public class AuditLogNotFoundException extends RuntimeException {

    /**
     * Instantiates exception with details of the missing record ID.
     *
     * @param id the missing audit log UUID
     */
    public AuditLogNotFoundException(UUID id) {
        super(String.format("Audit log with ID=%s was not found.", id));
    }
}
