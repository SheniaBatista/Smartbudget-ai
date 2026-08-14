package com.smartbudget.application.dto;

import com.smartbudget.domain.model.Balance;
import com.smartbudget.domain.model.DateRange;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceView(LocalDate from,
                          LocalDate to,
                          BigDecimal totalIncome,
                          BigDecimal totalExpense,
                          BigDecimal netBalance) {
    public static BalanceView from(DateRange range, Balance balance) {
        return new BalanceView(
                range.from(),
                range.to(),
                balance.totalIncome(),
                balance.totalExpense(),
                balance.netBalance());
    }
}
