package com.cryptovault.risk.controller;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.RiskResponse;
import com.cryptovault.risk.service.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <h3>RiskController</h3>
 *
 * <p><b>Why it exists:</b> Exposes REST API endpoints allowing other microservices to trigger risk assessments and compliance teams to query histories.</p>
 * <p><b>Architectural Layer:</b> Controller / REST Interface Layer.</p>
 * <p><b>Design Patterns Used:</b> Front Controller Pattern.</p>
 * <p><b>Banking Relevance:</b> Gateway routing controller exposing endpoints to fetch fraud scores and block compromised user credentials.</p>
 * <p><b>Scalability Considerations:</b> Stateless controllers that parse context headers, resolving concurrency blocks.</p>
 * <p><b>Interview Talking Points:</b> Wraps responses in standard, unified <code>ApiResponse</code> envelopes and uses Spring <code>@Valid</code> constraints checking.</p>
 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Engine Interface", description = "Endpoints for checking real-time fraud scores, transaction rules validation, and historical risk records")
public class RiskController {

    private final RiskService riskService;

    /**
     * Trigger risk evaluation on a transaction.
     *
     * @param request target transaction risk details
     * @return 201 Created containing RiskResponse payload
     */
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate Transaction Risk", description = "Executes fraud checks and compliance evaluation rules on the incoming transaction context details.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Risk evaluation completed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<RiskResponse>> evaluateRisk(@Valid @RequestBody EvaluateRiskRequest request) {
        log.info("Received POST /api/risk/evaluate for user={}", request.getUserId());
        RiskResponse response = riskService.evaluateTransactionRisk(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Risk evaluation completed successfully", response));
    }

    /**
     * Fetch a specific risk assessment by its UUID.
     *
     * @param id risk assessment identifier
     * @return 200 OK containing assessment details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Risk Assessment Details", description = "Retrieves details of a specific transaction risk assessment by its unique UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Risk assessment details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Risk assessment record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<RiskResponse>> getRiskAssessment(
            @PathVariable("id") @Parameter(description = "Unique UUID of the risk assessment record", example = "d2bf8a59-122e-407b-a1bc-cd14c2b9a811") UUID id
    ) {
        log.info("Received GET /api/risk/{}", id);
        RiskResponse response = riskService.getRiskAssessment(id);
        return ResponseEntity.ok(ApiResponse.success("Risk assessment details retrieved successfully", response));
    }

    /**
     * Fetch user risk history.
     *
     * @param userId user identifier
     * @return list of risk history records
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Risk History", description = "Retrieves all risk assessments records registered for a specific user UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User risk history retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User risk history not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<RiskResponse>>> getUserRiskHistory(
            @PathVariable("userId") @Parameter(description = "Unique UUID of the target user", example = "123e4567-e89b-12d3-a456-426614174000") UUID userId
    ) {
        log.info("Received GET /api/risk/user/{}", userId);
        List<RiskResponse> response = riskService.getUserRiskHistory(userId);
        return ResponseEntity.ok(ApiResponse.success("User risk history retrieved successfully", response));
    }

    /**
     * Fetch all risk assessments records registered.
     *
     * @return list of all assessments
     */
    @GetMapping("/history")
    @Operation(summary = "Get All Risk Assessments", description = "Retrieves all risk evaluation assessments registered in the platform database (compliance dashboard admin query).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All risk assessments history retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<RiskResponse>>> getAllAssessments() {
        log.info("Received GET /api/risk/history");
        List<RiskResponse> response = riskService.getAllAssessments();
        return ResponseEntity.ok(ApiResponse.success("All risk assessments history retrieved successfully", response));
    }
}
