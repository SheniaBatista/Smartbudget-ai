package com.smartbudget.application.dto;

import com.smartbudget.domain.model.MonthlySummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlySummaryView(String period,
                                 LocalDate from,
                                 LocalDate to,
                                 BigDecimal totalIncome,
                                 BigDecimal totalExpense,
                                 BigDecimal netBalance,
                                 long transactionCount,
                                 TransactionView largestExpense,
                                 CategoryExpenseView topCategory,
                                 List<CategoryExpenseView> expensesByCategory) {
    public static MonthlySummaryView from(MonthlySummary summary) {
        BigDecimal totalExpense = summary.balance().totalExpense();

        List<CategoryExpenseView> categories = summary.expensesByCategory().stream()
                .map(expense -> CategoryExpenseView.from(expense, totalExpense))
                .toList();

        return new MonthlySummaryView(
                summary.period().toString(),
                summary.period().atDay(1),
                summary.period().atEndOfMonth(),
                summary.balance().totalIncome(),
                totalExpense,
                summary.balance().netBalance(),
                summary.transactionCount(),
                summary.findLargestExpense().map(TransactionView::from).orElse(null),
                categories.isEmpty() ? null : categories.getFirst(),
                categories);
    }
}
