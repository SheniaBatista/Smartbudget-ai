package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.MonthlySummaryView;
import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.model.Balance;
import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.MonthlySummary;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.domain.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
public class GetMonthlySummaryUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetMonthlySummaryUseCase.class);

    private final TransactionRepository repository;

    public GetMonthlySummaryUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MonthlySummaryView execute(YearMonth month) {
        if (month == null) {
            throw new InvalidPeriodException("O mes de referencia e obrigatorio.");
        }

        DateRange range = DateRange.ofMonth(month);

        BigDecimal income = repository.sumAmount(TransactionType.INCOME, range);
        BigDecimal expense = repository.sumAmount(TransactionType.EXPENSE, range);
        long transactionCount = repository.count(range, null);
        Transaction largestExpense = repository.findLargestExpense(range).orElse(null);
        List<CategoryExpense> expensesByCategory = repository.sumExpensesGroupedByCategory(range);

        MonthlySummary summary = new MonthlySummary(
                month,
                Balance.of(income, expense),
                transactionCount,
                largestExpense,
                expensesByCategory);

        log.info("Monthly summary computed for {}: {} transactions", month, transactionCount);

        return MonthlySummaryView.from(summary);
    }
}
