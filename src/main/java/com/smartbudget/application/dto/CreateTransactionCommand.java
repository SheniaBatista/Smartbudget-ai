package com.smartbudget.application.dto;

import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionCommand(String description,
                                       BigDecimal amount,
                                       TransactionType type,
                                       TransactionCategory category,
                                       LocalDate occurredAt) {
}
