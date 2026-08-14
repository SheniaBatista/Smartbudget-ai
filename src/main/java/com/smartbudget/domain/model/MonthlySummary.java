package com.smartbudget.domain.model;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MonthlySummary(YearMonth period,
                             Balance balance,
                             long transactionCount,
                             Transaction largestExpense,
                             List<CategoryExpense> expensesByCategory) {
    public MonthlySummary {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(balance, "balance");
        expensesByCategory = expensesByCategory == null ? List.of() : List.copyOf(expensesByCategory);
    }

    public Optional<Transaction> findLargestExpense() {
        return Optional.ofNullable(largestExpense);
    }

    public Optional<CategoryExpense> topCategory() {
        return expensesByCategory.isEmpty() ? Optional.empty() : Optional.of(expensesByCategory.getFirst());
    }

    public boolean isEmpty() {
        return transactionCount == 0;
    }
}
