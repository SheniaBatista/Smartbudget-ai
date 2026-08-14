package com.smartbudget.infrastructure.persistence.mapper;

import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.infrastructure.persistence.entity.TransactionEntity;

public final class TransactionEntityMapper {
    private TransactionEntityMapper() {
    }

    public static TransactionEntity toEntity(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.id().uuid());
        entity.setDescription(transaction.description());
        entity.setAmount(transaction.amount());
        entity.setType(transaction.type());
        entity.setCategory(transaction.category());
        entity.setOccurredAt(transaction.occurredAt());
        entity.setCreatedAt(transaction.createdAt());
        return entity;
    }

    public static Transaction toDomain(TransactionEntity entity) {
        return Transaction.restore(
                TransactionId.of(entity.getId()),
                entity.getDescription(),
                entity.getAmount(),
                entity.getType(),
                entity.getCategory(),
                entity.getOccurredAt(),
                entity.getCreatedAt());
    }
}
