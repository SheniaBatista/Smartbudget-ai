package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID uuid) {
    public TransactionId {
        Objects.requireNonNull(uuid, "O identificador da transacao nao pode ser nulo.");
    }

    public TransactionId() {
        this(UUID.randomUUID());
    }

    public static TransactionId generate() {
        return new TransactionId();
    }

    public static TransactionId of(UUID uuid) {
        return new TransactionId(uuid);
    }

    public static TransactionId of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("O identificador da transacao e obrigatorio.");
        }
        try {
            return new TransactionId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException exception) {
            throw new InvalidTransactionException(
                    "Identificador de transacao invalido: '" + value + "'. Use um UUID.");
        }
    }

    @Override
    public String toString() {
        return uuid.toString();
    }
}
