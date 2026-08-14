package com.smartbudget.infrastructure.web.controller;

import com.smartbudget.application.dto.BalanceView;
import com.smartbudget.application.dto.CategoryExpenseView;
import com.smartbudget.application.dto.MonthlySummaryView;
import com.smartbudget.application.usecase.GetBalanceUseCase;
import com.smartbudget.application.usecase.GetExpensesByCategoryUseCase;
import com.smartbudget.application.usecase.GetMonthlySummaryUseCase;
import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.model.DateRange;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {
    private final GetBalanceUseCase getBalance;
    private final GetMonthlySummaryUseCase getMonthlySummary;
    private final GetExpensesByCategoryUseCase getExpensesByCategory;

    public FinanceController(GetBalanceUseCase getBalance,
                             GetMonthlySummaryUseCase getMonthlySummary,
                             GetExpensesByCategoryUseCase getExpensesByCategory) {
        this.getBalance = getBalance;
        this.getMonthlySummary = getMonthlySummary;
        this.getExpensesByCategory = getExpensesByCategory;
    }

    @GetMapping("/balance")
    public BalanceView balance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return getBalance.execute(rangeOrNull(from, to));
    }

    @GetMapping("/summary")
    public MonthlySummaryView summary(@RequestParam(required = false) String month) {
        return getMonthlySummary.execute(parseMonth(month));
    }

    @GetMapping("/expenses-by-category")
    public List<CategoryExpenseView> expensesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return getExpensesByCategory.execute(rangeOrNull(from, to));
    }

    private DateRange rangeOrNull(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return DateRange.of(
                from != null ? from : LocalDate.of(1970, 1, 1),
                to != null ? to : LocalDate.now());
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException exception) {
            throw new InvalidPeriodException("Mes invalido: '" + month + "'. Use o formato AAAA-MM, por exemplo 2026-08.");
        }
    }
}
