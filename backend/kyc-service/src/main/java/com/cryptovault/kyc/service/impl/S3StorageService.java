package com.cryptovault.kyc.service.impl;

import com.cryptovault.kyc.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <h3>S3StorageService</h3>
 *
 * <p><b>Why it exists:</b> Strategy implementation placeholder for AWS S3 cloud file storage (Phase 2 migration target).</p>
 * <p><b>Architectural Layer:</b> Service Implementation Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern (concrete strategy).</p>
 * <p><b>Financial Compliance Relevance:</b> Production-grade S3 bucket integrations support encryption-at-rest (KMS keys) and version control, satisfying regulatory compliance demands.</p>
 * <p><b>Scalability Considerations:</b> Highly scalable, removing file state from JVM server disks and enabling cluster replication across AWS ECS tasks.</p>
 * <p><b>Interview Talking Points:</b> Serves as a direct illustration of the Strategy Pattern's power, showing how we can swap storage backends by updating configurations without touching core domain code.</p>
 */
@Service("s3StorageService")
@Slf4j
public class S3StorageService implements StorageService {

    @Override
    public String store(MultipartFile file, String subFolder, String fileName) throws IOException {
        log.info("[S3 STUB] Mocking upload to AWS S3 bucket: path={}/{}", subFolder, fileName);
        return "s3://cryptovault-kyc-bucket/" + subFolder + "/" + fileName;
    }

    @Override
    public byte[] retrieve(String filePath) throws IOException {
        log.info("[S3 STUB] Mocking retrieval from AWS S3 bucket: path={}", filePath);
        return "Mock S3 Document Byte Contents".getBytes();
    }

    @Override
    public void delete(String filePath) throws IOException {
        log.info("[S3 STUB] Mocking deletion from AWS S3 bucket: path={}", filePath);
    }
}
