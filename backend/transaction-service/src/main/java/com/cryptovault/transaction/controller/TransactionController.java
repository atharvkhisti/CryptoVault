package com.cryptovault.transaction.controller;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.transaction.dto.request.DepositRequest;
import com.cryptovault.transaction.dto.request.TransferRequest;
import com.cryptovault.transaction.dto.request.WithdrawRequest;
import com.cryptovault.transaction.dto.response.TransactionResponse;
import com.cryptovault.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller exposing transaction endpoints under `/api/transactions`
 * for executing transfers, deposits, withdrawals, and retrieving historical logs.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Lifecycle Interface", description = "Endpoints for initiating and tracking deposits, withdrawals, transfers, and transaction history logs")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Executes a balance transfer between two wallets.
     *
     * @param request transfer details DTO
     * @return REST Response containing transfer results
     */
    @PostMapping("/transfer")
    @Operation(summary = "Transfer Funds", description = "Initiates a balance transfer of a specified cryptocurrency quantity between a source and destination wallet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transfer completed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Risk rules violation, insufficient wallet funds, or validation failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "One or both wallets not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        UUID userId = getCurrentUserId();
        TransactionResponse response = transactionService.transfer(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", response));
    }

    /**
     * Executes a deposit into a wallet.
     *
     * @param request deposit details DTO
     * @return REST Response containing deposit results
     */
    @PostMapping("/deposit")
    @Operation(summary = "Deposit Funds", description = "Creates a deposit transaction adding cryptocurrency funds to the designated wallet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deposit completed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(@Valid @RequestBody DepositRequest request) {
        UUID userId = getCurrentUserId();
        TransactionResponse response = transactionService.deposit(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Deposit completed successfully", response));
    }

    /**
     * Executes a withdrawal from a wallet.
     *
     * @param request withdrawal details DTO
     * @return REST Response containing withdrawal results
     */
    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw Funds", description = "Creates a withdrawal transaction subtracting cryptocurrency funds from the designated wallet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Withdrawal completed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Risk failure, insufficient funds, or validation failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        UUID userId = getCurrentUserId();
        TransactionResponse response = transactionService.withdraw(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal completed successfully", response));
    }

    /**
     * Retrieves the authenticated user's transaction history.
     *
     * @return REST Response containing the list of transaction records
     */
    @GetMapping
    @Operation(summary = "Get Transaction History", description = "Retrieves a comprehensive list of transaction records associated with the caller's user account.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory() {
        UUID userId = getCurrentUserId();
        List<TransactionResponse> response = transactionService.getTransactionHistory(userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved successfully", response));
    }

    /**
     * Retrieves details of a specific transaction by its ID.
     *
     * @param id transaction UUID
     * @return REST Response containing the transaction details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Transaction Details", description = "Retrieves detailed properties of a specific transaction by its unique identifier (UUID).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable("id") @Parameter(description = "Unique UUID of the transaction", example = "92bf8a59-122e-407b-a1bc-cd14c2b9a788") UUID id
    ) {
        UUID userId = getCurrentUserId();
        TransactionResponse response = transactionService.getTransactionById(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", response));
    }

    /**
     * Retrieves user transactions filtered by status.
     *
     * @param status transaction status
     * @return REST Response containing filtered transactions list
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Filter Transactions by Status", description = "Retrieves user transaction records filtered by a specific execution status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions filtered by status retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByStatus(
            @PathVariable("status") @Parameter(description = "Transaction execution status to filter by", example = "COMPLETED") TransactionStatus status
    ) {
        UUID userId = getCurrentUserId();
        List<TransactionResponse> response = transactionService.getTransactionsByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.success("Transactions filtered by status retrieved successfully", response));
    }

    private UUID getCurrentUserId() {
        JwtUserPrincipal principal = (JwtUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getUserId();
    }
}
