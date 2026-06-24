package com.cryptovault.kyc.config;

import com.cryptovault.kyc.service.StorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * <h3>StorageConfig</h3>
 *
 * <p><b>Why it exists:</b> Configuration class that dynamically binds the active {@link StorageService} implementation bean at runtime.</p>
 * <p><b>Architectural Layer:</b> Infrastructure Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern Orchestration / Factory Method Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Enables zero-code configuration shifts from developer machines to highly secure cloud production systems.</p>
 * <p><b>Scalability Considerations:</b> Resolves localized server storage bounds by facilitating simple switches to AWS S3 storage.</p>
 * <p><b>Interview Talking Points:</b> Uses standard Spring <code>@Value</code> properties to select between <code>localStorageService</code> and <code>s3StorageService</code> strategies dynamically.</p>
 */
@Configuration
public class StorageConfig {

    @Bean
    @Primary
    public StorageService storageService(
            @Value("${kyc.storage.type:LOCAL}") String storageType,
            @Qualifier("localStorageService") StorageService localService,
            @Qualifier("s3StorageService") StorageService s3Service
    ) {
        if ("S3".equalsIgnoreCase(storageType)) {
            return s3Service;
        }
        return localService;
    }
}
