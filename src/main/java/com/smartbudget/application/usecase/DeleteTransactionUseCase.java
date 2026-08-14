package com.smartbudget.application.usecase;

import com.smartbudget.domain.exception.TransactionNotFoundException;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTransactionUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteTransactionUseCase.class);

    private final TransactionRepository repository;

    public DeleteTransactionUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(TransactionId id) {
        if (!repository.deleteById(id)) {
            throw new TransactionNotFoundException(id);
        }
        log.info("Transaction deleted: {}", id);
    }
}
