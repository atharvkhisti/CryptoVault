package com.cryptovault.wallet.controller;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.wallet.dto.request.CreateWalletRequest;
import com.cryptovault.wallet.dto.request.DepositRequest;
import com.cryptovault.wallet.dto.request.WithdrawRequest;
import com.cryptovault.wallet.dto.response.WalletResponse;
import com.cryptovault.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing API endpoints under `/api/wallets`
 * for deposit, withdrawal, and wallet listing management.
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management Interface", description = "Endpoints for creating, managing, depositing, withdrawing, and checking balances of cryptocurrency wallets")
public class WalletController {

    private final WalletService walletService;

    /**
     * Endpoint to instantiate a new wallet account for the authenticated user context.
     *
     * @param request wallet creation request DTO
     * @return 201 CREATED containing the created wallet response
     */
    @PostMapping
    @Operation(summary = "Create Wallet", description = "Instantiates a new wallet account for the authenticated user context and specified currency.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Wallet created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameter failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Wallet for the currency already exists", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        UUID userId = getCurrentUserId();
        WalletResponse response = walletService.createWallet(userId, request);
        return new ResponseEntity<>(
                ApiResponse.success("Wallet created successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Endpoint to fetch all active wallets belonging to the authenticated user context.
     *
     * @return 200 OK containing list of user wallets
     */
    @GetMapping
    @Operation(summary = "Get User Wallets", description = "Fetches all active wallets belonging to the authenticated user context.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallets retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getUserWallets() {
        UUID userId = getCurrentUserId();
        List<WalletResponse> response = walletService.getUserWallets(userId);
        return ResponseEntity.ok(ApiResponse.success("Wallets retrieved successfully", response));
    }

    /**
     * Endpoint to deposit funds into a user's wallet.
     *
     * @param request deposit request details DTO
     * @return 200 OK containing the updated wallet status response
     */
    @PostMapping("/deposit")
    @Operation(summary = "Deposit Funds", description = "Deposits funds into an authenticated user's wallet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Funds deposited successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure or negative amount", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found for the user", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(@Valid @RequestBody DepositRequest request) {
        UUID userId = getCurrentUserId();
        WalletResponse response = walletService.deposit(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Funds deposited successfully", response));
    }

    /**
     * Endpoint to withdraw funds from a user's wallet.
     *
     * @param request withdrawal request details DTO
     * @return 200 OK containing the updated wallet status response
     */
    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw Funds", description = "Withdraws funds from an authenticated user's wallet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Funds withdrawn successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance or invalid amount", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found for the user", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        UUID userId = getCurrentUserId();
        WalletResponse response = walletService.withdraw(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Funds withdrawn successfully", response));
    }

    /**
     * Endpoint to fetch a wallet's details by its ID.
     *
     * @param id wallet UUID
     * @return 200 OK containing the wallet details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Wallet Details", description = "Retrieves wallet details by its unique identifier (UUID).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @PathVariable("id") @Parameter(description = "Unique UUID of the wallet", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710") UUID id
    ) {
        WalletResponse response = walletService.getWalletById(id);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", response));
    }

    /**
     * Endpoint to debit a wallet's balance.
     *
     * @param id     wallet UUID
     * @param amount amount to subtract
     * @return 200 OK containing the updated wallet state
     */
    @PostMapping("/{id}/debit")
    @Operation(summary = "Debit Wallet", description = "Debits a specific wallet's balance (internal/system use).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet debited successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient funds or invalid amount", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<WalletResponse>> debitWallet(
            @PathVariable("id") @Parameter(description = "Unique UUID of the wallet", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710") UUID id,
            @RequestParam("amount") @Parameter(description = "Amount of funds to debit", example = "10.00") BigDecimal amount
    ) {
        WalletResponse response = walletService.debit(id, amount);
        return ResponseEntity.ok(ApiResponse.success("Wallet debited successfully", response));
    }

    /**
     * Endpoint to credit a wallet's balance.
     *
     * @param id     wallet UUID
     * @param amount amount to add
     * @return 200 OK containing the updated wallet state
     */
    @PostMapping("/{id}/credit")
    @Operation(summary = "Credit Wallet", description = "Credits a specific wallet's balance (internal/system use).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet credited successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credit amount", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<WalletResponse>> creditWallet(
            @PathVariable("id") @Parameter(description = "Unique UUID of the wallet", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710") UUID id,
            @RequestParam("amount") @Parameter(description = "Amount of funds to credit", example = "10.00") BigDecimal amount
    ) {
        WalletResponse response = walletService.credit(id, amount);
        return ResponseEntity.ok(ApiResponse.success("Wallet credited successfully", response));
    }

    private UUID getCurrentUserId() {
        JwtUserPrincipal principal = (JwtUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getUserId();
    }
}
