package com.cryptovault.transaction.mapper;

import com.cryptovault.transaction.dto.response.TransactionResponse;
import com.cryptovault.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

/**
 * Component mapping Transaction entities to TransactionResponse DTO schemas.
 */
@Component
public class TransactionMapper {

    /**
     * Converts a Transaction entity to a TransactionResponse DTO.
     *
     * @param transaction JPA transaction entity
     * @return populated DTO response representation
     */
    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .referenceNumber(transaction.getReferenceNumber())
                .timestamp(transaction.getCreatedAt())
                .build();
    }
}
