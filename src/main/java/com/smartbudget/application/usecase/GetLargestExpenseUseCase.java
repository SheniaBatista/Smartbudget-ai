package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetLargestExpenseUseCase {
    private final TransactionRepository repository;

    public GetLargestExpenseUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<TransactionView> execute(DateRange range) {
        DateRange effective = range != null ? range : DateRange.allTime();
        return repository.findLargestExpense(effective).map(TransactionView::from);
    }
}
