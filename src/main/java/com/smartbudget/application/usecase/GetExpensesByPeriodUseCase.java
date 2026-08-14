package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.PeriodExpenseView;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GetExpensesByPeriodUseCase {
    private final TransactionRepository repository;

    public GetExpensesByPeriodUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PeriodExpenseView execute(DateRange range) {
        DateRange effective = range != null ? range : DateRange.allTime();

        BigDecimal total = repository.sumAmount(TransactionType.EXPENSE, effective);
        long count = repository.count(effective, TransactionType.EXPENSE);

        return PeriodExpenseView.of(effective, total, count);
    }
}
