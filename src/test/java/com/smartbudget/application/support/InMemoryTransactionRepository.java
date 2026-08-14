package com.smartbudget.application.support;

import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class InMemoryTransactionRepository implements TransactionRepository {
    private static final Comparator<Transaction> NEWEST_FIRST =
            Comparator.comparing(Transaction::occurredAt).thenComparing(Transaction::createdAt).reversed();

    private final Map<TransactionId, Transaction> storage = new LinkedHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        storage.put(transaction.id(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean deleteById(TransactionId id) {
        return id != null && storage.remove(id) != null;
    }

    @Override
    public List<Transaction> findRecent(int limit) {
        return storage.values().stream().sorted(NEWEST_FIRST).limit(limit).toList();
    }

    @Override
    public List<Transaction> search(DateRange range, TransactionType type, TransactionCategory category, int limit) {
        return filter(range, type, category).sorted(NEWEST_FIRST).limit(limit).toList();
    }

    @Override
    public BigDecimal sumAmount(TransactionType type, DateRange range) {
        return filter(range, type, null)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<CategoryExpense> sumExpensesGroupedByCategory(DateRange range) {
        Map<TransactionCategory, List<Transaction>> grouped = new LinkedHashMap<>();
        filter(range, TransactionType.EXPENSE, null)
                .forEach(transaction -> grouped
                        .computeIfAbsent(transaction.category(), key -> new java.util.ArrayList<>())
                        .add(transaction));

        return grouped.entrySet().stream()
                .map(entry -> new CategoryExpense(
                        entry.getKey(),
                        entry.getValue().stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(CategoryExpense::total).reversed())
                .toList();
    }

    @Override
    public Optional<Transaction> findLargestExpense(DateRange range) {
        return filter(range, TransactionType.EXPENSE, null)
                .max(Comparator.comparing(Transaction::amount));
    }

    @Override
    public long count(DateRange range, TransactionType type) {
        return filter(range, type, null).count();
    }

    private Stream<Transaction> filter(DateRange range, TransactionType type, TransactionCategory category) {
        return storage.values().stream()
                .filter(transaction -> range == null || range.contains(transaction.occurredAt()))
                .filter(transaction -> type == null || transaction.type() == type)
                .filter(transaction -> category == null || transaction.category() == category);
    }
}
