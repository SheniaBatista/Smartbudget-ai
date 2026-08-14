package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum TransactionType {
    INCOME("Receita"),
    EXPENSE("Despesa");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TransactionType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("O tipo da transacao e obrigatorio. Valores aceitos: " + accepted());
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidTransactionException(
                        "Tipo de transacao invalido: '" + value + "'. Valores aceitos: " + accepted()));
    }

    public static String accepted() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
