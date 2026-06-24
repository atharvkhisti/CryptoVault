package com.cryptovault.kyc.exception;

import com.cryptovault.common.enums.ErrorCode;

/**
 * <h3>KycRecordNotFoundException</h3>
 *
 * <p><b>Why it exists:</b> Exception thrown when a user's KYC record is missing.</p>
 */
public class KycRecordNotFoundException extends KycException {

    public KycRecordNotFoundException(String message) {
        super(message, ErrorCode.KYC_RECORD_NOT_FOUND);
    }
}
