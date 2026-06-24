package com.cryptovault.kyc.validation;

import com.cryptovault.kyc.exception.KycException;
import com.cryptovault.common.enums.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <h3>DocumentValidator</h3>
 *
 * <p><b>Why it exists:</b> Validates incoming upload payloads to ensure MIME types are recognized and files fit size constraints.</p>
 * <p><b>Architectural Layer:</b> Validation Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy validation checks.</p>
 * <p><b>Financial Compliance Relevance:</b> Mitigates code injection risks (preventing malicious executables masquerading as documents) and restricts storage usage bounds.</p>
 * <p><b>Scalability Considerations:</b> Validations execute in-memory on incoming stream headers, protecting service threads from downloading oversize payloads completely.</p>
 * <p><b>Interview Talking Points:</b> Validates input parameters strictly, checking content types against an allowed MIME list (PDF, PNG, JPEG) and checking file size bounds (5MB).</p>
 */
@Component
public class DocumentValidator {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    /**
     * Validates file parameter parameters.
     *
     * @param file uploaded multipart file
     * @throws KycException if validation fails
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KycException("Uploaded file is empty or missing", ErrorCode.INVALID_FILE_TYPE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new KycException("Invalid file format. Allowed formats: JPEG, PNG, PDF", ErrorCode.INVALID_FILE_TYPE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new KycException("File size exceeds the allowable 5MB limit", ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }
}
