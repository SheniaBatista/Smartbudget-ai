package com.smartbudget.application.dto;

import com.smartbudget.domain.model.DateRange;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodExpenseView(LocalDate from,
                                LocalDate to,
                                BigDecimal totalExpense,
                                long transactionCount) {
    public static PeriodExpenseView of(DateRange range, BigDecimal totalExpense, long transactionCount) {
        return new PeriodExpenseView(range.from(), range.to(), totalExpense, transactionCount);
    }
}
