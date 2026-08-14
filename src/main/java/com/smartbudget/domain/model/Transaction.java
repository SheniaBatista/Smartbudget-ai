package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Transaction {
    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    private static final int MONETARY_SCALE = 2;

    private final TransactionId id;
    private final String description;
    private final BigDecimal amount;
    private final TransactionType type;
    private final TransactionCategory category;
    private final LocalDate occurredAt;
    private final LocalDateTime createdAt;

    private Transaction(TransactionId id,
                        String description,
                        BigDecimal amount,
                        TransactionType type,
                        TransactionCategory category,
                        LocalDate occurredAt,
                        LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static Transaction create(String description,
                                     BigDecimal amount,
                                     TransactionType type,
                                     TransactionCategory category,
                                     LocalDate occurredAt) {
        LocalDate effectiveDate = occurredAt != null ? occurredAt : LocalDate.now();
        if (effectiveDate.isAfter(LocalDate.now())) {
            throw new InvalidTransactionException(
                    "A data da transacao nao pode estar no futuro: " + effectiveDate);
        }
        return new Transaction(
                TransactionId.generate(),
                validateDescription(description),
                validateAmount(amount),
                requireType(type),
                requireCategory(category),
                effectiveDate,
                LocalDateTime.now());
    }

    public static Transaction restore(TransactionId id,
                                      String description,
                                      BigDecimal amount,
                                      TransactionType type,
                                      TransactionCategory category,
                                      LocalDate occurredAt,
                                      LocalDateTime createdAt) {
        return new Transaction(
                Objects.requireNonNull(id, "id"),
                validateDescription(description),
                validateAmount(amount),
                requireType(type),
                requireCategory(category),
                Objects.requireNonNull(occurredAt, "occurredAt"),
                Objects.requireNonNull(createdAt, "createdAt"));
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("A descricao da transacao e obrigatoria.");
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidTransactionException(
                    "A descricao nao pode ultrapassar " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("O valor da transacao e obrigatorio.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("O valor da transacao deve ser maior que zero.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidTransactionException("O valor da transacao excede o limite suportado de " + MAX_AMOUNT + ".");
        }
        return amount.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }

    private static TransactionType requireType(TransactionType type) {
        if (type == null) {
            throw new InvalidTransactionException(
                    "O tipo da transacao e obrigatorio. Valores aceitos: " + TransactionType.accepted());
        }
        return type;
    }

    private static TransactionCategory requireCategory(TransactionCategory category) {
        if (category == null) {
            throw new InvalidTransactionException(
                    "A categoria da transacao e obrigatoria. Valores aceitos: " + TransactionCategory.accepted());
        }
        return category;
    }

    public TransactionId id() {
        return id;
    }

    public String description() {
        return description;
    }

    public BigDecimal amount() {
        return amount;
    }

    public TransactionType type() {
        return type;
    }

    public TransactionCategory category() {
        return category;
    }

    public LocalDate occurredAt() {
        return occurredAt;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Transaction transaction && id.equals(transaction.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Transaction[id=%s, type=%s, category=%s, amount=%s, occurredAt=%s]"
                .formatted(id, type, category, amount, occurredAt);
    }
}
