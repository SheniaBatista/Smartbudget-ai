package com.smartbudget.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record CategoryExpense(TransactionCategory category, BigDecimal total, long transactionCount) {
    private static final int MONETARY_SCALE = 2;

    public CategoryExpense {
        Objects.requireNonNull(category, "category");
        total = total == null
                ? BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_UP)
                : total.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal shareOf(BigDecimal totalExpenses) {
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        }
        return total.multiply(BigDecimal.valueOf(100))
                .divide(totalExpenses, MONETARY_SCALE, RoundingMode.HALF_UP);
    }
}
