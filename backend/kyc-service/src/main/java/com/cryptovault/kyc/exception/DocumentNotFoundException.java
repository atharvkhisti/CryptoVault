package com.cryptovault.kyc.exception;

import com.cryptovault.common.enums.ErrorCode;

/**
 * <h3>DocumentNotFoundException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown when an uploaded document reference is missing.</p>
 */
public class DocumentNotFoundException extends KycException {

    public DocumentNotFoundException(String message) {
        super(message, ErrorCode.DOCUMENT_NOT_FOUND);
    }
}
