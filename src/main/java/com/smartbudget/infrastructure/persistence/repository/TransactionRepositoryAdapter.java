package com.smartbudget.infrastructure.persistence.repository;

import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.domain.repository.TransactionRepository;
import com.smartbudget.infrastructure.persistence.entity.TransactionEntity;
import com.smartbudget.infrastructure.persistence.mapper.TransactionEntityMapper;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepositoryAdapter implements TransactionRepository {
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final TransactionJpaRepository jpaRepository;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = jpaRepository.save(TransactionEntityMapper.toEntity(transaction));
        return TransactionEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id.uuid()).map(TransactionEntityMapper::toDomain);
    }

    @Override
    public boolean deleteById(TransactionId id) {
        if (id == null || !jpaRepository.existsById(id.uuid())) {
            return false;
        }
        jpaRepository.deleteById(id.uuid());
        return true;
    }

    @Override
    public List<Transaction> findRecent(int limit) {
        return jpaRepository.findAll(PageRequest.of(0, sanitize(limit), NEWEST_FIRST))
                .getContent().stream()
                .map(TransactionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> search(DateRange range, TransactionType type, TransactionCategory category, int limit) {
        Specification<TransactionEntity> specification = filterBy(range, type, category);
        return jpaRepository.findAll(specification, PageRequest.of(0, sanitize(limit), NEWEST_FIRST))
                .getContent().stream()
                .map(TransactionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumAmount(TransactionType type, DateRange range) {
        BigDecimal total = jpaRepository.sumAmount(type, range.from(), range.to());
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public List<CategoryExpense> sumExpensesGroupedByCategory(DateRange range) {
        return jpaRepository.sumGroupedByCategory(TransactionType.EXPENSE, range.from(), range.to()).stream()
                .map(projection -> new CategoryExpense(
                        projection.getCategory(),
                        projection.getTotal(),
                        projection.getTransactionCount() != null ? projection.getTransactionCount() : 0L))
                .toList();
    }

    @Override
    public Optional<Transaction> findLargestExpense(DateRange range) {
        return jpaRepository
                .findFirstByTypeAndOccurredAtBetweenOrderByAmountDescCreatedAtDesc(
                        TransactionType.EXPENSE, range.from(), range.to())
                .map(TransactionEntityMapper::toDomain);
    }

    @Override
    public long count(DateRange range, TransactionType type) {
        return type == null
                ? jpaRepository.countByOccurredAtBetween(range.from(), range.to())
                : jpaRepository.countByTypeAndOccurredAtBetween(type, range.from(), range.to());
    }

    private Specification<TransactionEntity> filterBy(DateRange range,
                                                      TransactionType type,
                                                      TransactionCategory category) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (range != null) {
                Path<LocalDate> occurredAt = root.get("occurredAt");
                predicates.add(builder.between(occurredAt, range.from(), range.to()));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (category != null) {
                predicates.add(builder.equal(root.get("category"), category));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private int sanitize(int limit) {
        return limit <= 0 ? 1 : limit;
    }
}
