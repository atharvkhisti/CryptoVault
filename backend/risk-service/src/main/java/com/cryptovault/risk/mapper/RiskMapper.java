package com.cryptovault.risk.mapper;

import com.cryptovault.risk.entity.RiskAssessment;
import com.cryptovault.risk.dto.response.RiskResponse;
import org.springframework.stereotype.Component;

/**
 * <h3>RiskMapper</h3>
 *
 * <p><b>Why it exists:</b> Translates database entity mappings to presentation Data Transfer Objects.</p>
 * <p><b>Architectural Layer:</b> Mapping Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Mapper Pattern.</p>
 * <p><b>Banking Relevance:</b> Ensures internal storage models are strictly isolated from JSON serialization specs.</p>
 * <p><b>Scalability Considerations:</b> Highly efficient pure Java mapping without reflection.</p>
 * <p><b>Interview Talking Points:</b> Maps properties explicitly, avoiding the runtime reflection overhead of MapStruct or ModelMapper.</p>
 */
@Component
public class RiskMapper {

    /**
     * Map RiskAssessment entity to RiskResponse DTO.
     *
     * @param assessment database entity
     * @return presentation response DTO
     */
    public RiskResponse toResponse(RiskAssessment assessment) {
        if (assessment == null) {
            return null;
        }
        return RiskResponse.builder()
                .id(assessment.getId())
                .userId(assessment.getUserId())
                .transactionId(assessment.getTransactionId())
                .riskLevel(assessment.getRiskLevel())
                .status(assessment.getStatus())
                .riskScore(assessment.getRiskScore())
                .triggeredRule(assessment.getTriggeredRule())
                .comments(assessment.getComments())
                .createdAt(assessment.getCreatedAt())
                .build();
    }
}
