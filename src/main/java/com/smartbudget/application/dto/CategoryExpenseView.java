package com.smartbudget.application.dto;

import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.TransactionCategory;

import java.math.BigDecimal;

public record CategoryExpenseView(TransactionCategory category,
                                  String categoryLabel,
                                  BigDecimal total,
                                  long transactionCount,
                                  BigDecimal percentageOfExpenses) {
    public static CategoryExpenseView from(CategoryExpense expense, BigDecimal totalExpenses) {
        return new CategoryExpenseView(
                expense.category(),
                expense.category().label(),
                expense.total(),
                expense.transactionCount(),
                expense.shareOf(totalExpenses));
    }
}
