package com.smartbudget.infrastructure.persistence.repository;

import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository
        extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {
    @Query("""
            select coalesce(sum(t.amount), 0)
            from TransactionEntity t
            where t.type = :type
              and t.occurredAt between :from and :to
            """)
    BigDecimal sumAmount(@Param("type") TransactionType type,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);

    @Query("""
            select t.category as category,
                   sum(t.amount) as total,
                   count(t) as transactionCount
            from TransactionEntity t
            where t.type = :type
              and t.occurredAt between :from and :to
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategoryTotalProjection> sumGroupedByCategory(@Param("type") TransactionType type,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    Optional<TransactionEntity> findFirstByTypeAndOccurredAtBetweenOrderByAmountDescCreatedAtDesc(
            TransactionType type, LocalDate from, LocalDate to);

    long countByOccurredAtBetween(LocalDate from, LocalDate to);

    long countByTypeAndOccurredAtBetween(TransactionType type, LocalDate from, LocalDate to);
}
