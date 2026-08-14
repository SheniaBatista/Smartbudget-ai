package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.BalanceView;
import com.smartbudget.domain.model.Balance;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GetBalanceUseCase {
    private final TransactionRepository repository;

    public GetBalanceUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BalanceView execute(DateRange range) {
        DateRange effective = range != null ? range : DateRange.allTime();

        BigDecimal income = repository.sumAmount(TransactionType.INCOME, effective);
        BigDecimal expense = repository.sumAmount(TransactionType.EXPENSE, effective);

        return BalanceView.from(effective, Balance.of(income, expense));
    }
}
