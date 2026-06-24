package com.cryptovault.kyc.service.impl;

import com.cryptovault.kyc.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

/**
 * <h3>LocalStorageService</h3>
 *
 * <p><b>Why it exists:</b> Strategy implementation that saves, retrieves, and deletes files using the local file system.</p>
 * <p><b>Architectural Layer:</b> Service Implementation Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern (concrete strategy).</p>
 * <p><b>Financial Compliance Relevance:</b> Useful for lightweight developer setup and sandbox configurations, avoiding AWS S3 billing spikes during development.</p>
 * <p><b>Scalability Considerations:</b> Not suitable for multi-node deployments since files are isolated on a single server's disk (mitigated by moving to the S3 Strategy in production).</p>
 * <p><b>Interview Talking Points:</b> Uses standard java <code>java.nio.file.Path</code> operations and cleans paths to prevent directory traversal exploits.</p>
 */
@Service("localStorageService")
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path baseStoragePath;

    public LocalStorageService(@Value("${kyc.storage.local.dir:./uploads}") String uploadDir) {
        this.baseStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStoragePath);
            log.info("Initialized local file storage directory at: {}", this.baseStoragePath);
        } catch (IOException e) {
            log.error("Could not create local storage base directory: {}", this.baseStoragePath, e);
            throw new RuntimeException("Could not initialize local storage path", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subFolder, String fileName) throws IOException {
        if (subFolder.contains("..") || fileName.contains("..")) {
            throw new SecurityException("Directory traversal attack detected: " + subFolder + " / " + fileName);
        }

        // Clean path variables to prevent directory traversal
        String cleanedSubFolder = Paths.get(subFolder).getFileName().toString();
        String cleanedFileName = Paths.get(fileName).getFileName().toString();

        Path targetDir = this.baseStoragePath.resolve(cleanedSubFolder).normalize();
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(cleanedFileName).normalize();
        
        // Assert that the file target is inside the base storage path to prevent exploits
        if (!targetFile.startsWith(this.baseStoragePath)) {
            throw new SecurityException("Cannot store file outside base directory");
        }

        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Successfully stored file locally at: {}", targetFile);
        return targetFile.toString();
    }

    @Override
    public byte[] retrieve(String filePath) throws IOException {
        if (filePath.contains("..")) {
            throw new SecurityException("Directory traversal attack detected in filePath: " + filePath);
        }
        Path targetFile = Paths.get(filePath).toAbsolutePath().normalize();
        
        if (!targetFile.startsWith(this.baseStoragePath)) {
            throw new SecurityException("Cannot retrieve file outside base directory");
        }

        if (!Files.exists(targetFile)) {
            throw new NoSuchFileException("File not found: " + targetFile);
        }

        return Files.readAllBytes(targetFile);
    }

    @Override
    public void delete(String filePath) throws IOException {
        if (filePath.contains("..")) {
            throw new SecurityException("Directory traversal attack detected in filePath: " + filePath);
        }
        Path targetFile = Paths.get(filePath).toAbsolutePath().normalize();
        
        if (!targetFile.startsWith(this.baseStoragePath)) {
            throw new SecurityException("Cannot delete file outside base directory");
        }

        Files.deleteIfExists(targetFile);
        log.debug("Successfully deleted file locally at: {}", targetFile);
    }
}
