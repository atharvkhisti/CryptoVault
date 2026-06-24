package com.cryptovault.kyc.dto.response;

import com.cryptovault.common.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>DocumentResponse</h3>
 *
 * <p><b>Why it exists:</b> DTO response representing metadata of an uploaded identity document.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Identity document metadata details representation")
public class DocumentResponse {

    @Schema(description = "Unique UUID of the stored document", example = "f2bf8a59-122e-407b-a1bc-cd14c2b9a833")
    private UUID documentId;

    @Schema(description = "Type classification of the document uploaded", example = "PASSPORT")
    private DocumentType documentType;

    @Schema(description = "Original filename uploaded by the user", example = "my_passport.jpg")
    private String fileName;

    @Schema(description = "Standard MIME type identifier of the file upload", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "Filesize in bytes", example = "2048500")
    private Long fileSize;

    @Schema(description = "Timestamp when the document was uploaded", example = "2026-06-19T19:03:49")
    private LocalDateTime uploadedAt;
}
