package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.CreateTransactionCommand;
import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.domain.exception.InvalidTransactionException;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTransactionUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateTransactionUseCase.class);

    private final TransactionRepository repository;

    public CreateTransactionUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransactionView execute(CreateTransactionCommand command) {
        if (command == null) {
            throw new InvalidTransactionException("Os dados da transacao sao obrigatorios.");
        }

        Transaction transaction = Transaction.create(
                command.description(),
                command.amount(),
                command.type(),
                command.category(),
                command.occurredAt());

        Transaction saved = repository.save(transaction);
        log.info("Transaction created: {} type={} category={} amount={}",
                saved.id(), saved.type(), saved.category(), saved.amount());

        return TransactionView.from(saved);
    }
}
