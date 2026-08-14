package com.smartbudget.domain.exception;

import com.smartbudget.domain.model.TransactionId;

public class TransactionNotFoundException extends DomainException {
    public TransactionNotFoundException(TransactionId id) {
        super("Transacao nao encontrada: " + id);
    }
}
