package com.smartbudget.domain.exception;

public class InvalidTransactionException extends DomainException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
