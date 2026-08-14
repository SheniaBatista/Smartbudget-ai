package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.domain.exception.TransactionNotFoundException;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTransactionByIdUseCase {
    private final TransactionRepository repository;

    public GetTransactionByIdUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TransactionView execute(TransactionId id) {
        return repository.findById(id)
                .map(TransactionView::from)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }
}
