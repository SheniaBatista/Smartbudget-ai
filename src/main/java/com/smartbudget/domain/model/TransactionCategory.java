package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum TransactionCategory {
    FOOD("Alimentação"),
    TRANSPORT("Transporte"),
    HOUSING("Moradia"),
    HEALTH("Saúde"),
    EDUCATION("Educação"),
    ENTERTAINMENT("Lazer"),
    SHOPPING("Compras"),
    SALARY("Salário"),
    INVESTMENT("Investimentos"),
    OTHER("Outros");

    private final String label;

    TransactionCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TransactionCategory parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("A categoria e obrigatoria. Valores aceitos: " + accepted());
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidTransactionException(
                        "Categoria invalida: '" + value + "'. Valores aceitos: " + accepted()));
    }

    public static String accepted() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
