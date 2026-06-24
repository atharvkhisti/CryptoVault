package com.cryptovault.kyc.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * <h3>StorageService</h3>
 *
 * <p><b>Why it exists:</b> Interface defining storage access methods (store, retrieve, delete) to decouple business services from physical storage implementations.</p>
 * <p><b>Architectural Layer:</b> Service Strategy Interface Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Abstracting file operations allows simple switching from local hard drives to secure, encrypted, and compliant cloud storage (like AWS S3) without rewriting business code.</p>
 * <p><b>Scalability Considerations:</b> Seamlessly supports distributed microservices scaling, as swapping strategies enables centralized cloud storage instead of local disk sharing.</p>
 * <p><b>Interview Talking Points:</b> Serves as the core strategy contract, allowing the KYC Service to run locally with <code>LocalStorageService</code> while swapping to <code>S3StorageService</code> in staging/prod configurations.</p>
 */
public interface StorageService {

    /**
     * Stores an uploaded file.
     *
     * @param file the multipart file to store
     * @param subFolder custom subdirectory name (e.g., user-id)
     * @param fileName target filename
     * @return the unique path reference to retrieve the file
     * @throws IOException if storage fails
     */
    String store(MultipartFile file, String subFolder, String fileName) throws IOException;

    /**
     * Retrieves stored file bytes.
     *
     * @param filePath the storage path reference
     * @return byte array of the file contents
     * @throws IOException if retrieval fails
     */
    byte[] retrieve(String filePath) throws IOException;

    /**
     * Deletes a stored file.
     *
     * @param filePath the storage path reference
     * @throws IOException if deletion fails
     */
    void delete(String filePath) throws IOException;
}
