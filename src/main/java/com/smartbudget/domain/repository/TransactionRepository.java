package com.smartbudget.domain.repository;

import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;

import com.smartbudget.domain.model.TransactionId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    Optional<Transaction> findById(TransactionId id);

    boolean deleteById(TransactionId id);

    List<Transaction> findRecent(int limit);

    List<Transaction> search(DateRange range, TransactionType type, TransactionCategory category, int limit);

    BigDecimal sumAmount(TransactionType type, DateRange range);

    List<CategoryExpense> sumExpensesGroupedByCategory(DateRange range);

    Optional<Transaction> findLargestExpense(DateRange range);

    long count(DateRange range, TransactionType type);
}
