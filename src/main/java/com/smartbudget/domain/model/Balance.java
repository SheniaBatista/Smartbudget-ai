package com.smartbudget.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Balance(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netBalance) {
    private static final int MONETARY_SCALE = 2;

    public static Balance of(BigDecimal totalIncome, BigDecimal totalExpense) {
        BigDecimal income = normalize(totalIncome);
        BigDecimal expense = normalize(totalExpense);
        return new Balance(income, expense, income.subtract(expense));
    }

    public static Balance empty() {
        return of(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public boolean isPositive() {
        return netBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }
}
