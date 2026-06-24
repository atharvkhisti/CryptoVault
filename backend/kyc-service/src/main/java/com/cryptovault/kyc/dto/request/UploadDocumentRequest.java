package com.cryptovault.kyc.dto.request;

import com.cryptovault.common.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * <h3>UploadDocumentRequest</h3>
 *
 * <p><b>Why it exists:</b> DTO payload encapsulating raw document uploads.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentRequest {

    @NotNull(message = "File is required")
    private MultipartFile file;

    @NotNull(message = "Document type is required")
    private DocumentType documentType;
}
