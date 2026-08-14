package com.smartbudget.application.dto;

import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionView(UUID id,
                              String description,
                              BigDecimal amount,
                              TransactionType type,
                              String typeLabel,
                              TransactionCategory category,
                              String categoryLabel,
                              LocalDate occurredAt,
                              LocalDateTime createdAt) {
    public static TransactionView from(Transaction transaction) {
        return new TransactionView(
                transaction.id().uuid(),
                transaction.description(),
                transaction.amount(),
                transaction.type(),
                transaction.type().label(),
                transaction.category(),
                transaction.category().label(),
                transaction.occurredAt(),
                transaction.createdAt());
    }
}
