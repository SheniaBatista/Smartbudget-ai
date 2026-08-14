package com.smartbudget.application.support;

import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TransactionFixtures {
    private TransactionFixtures() {
    }

    public static Transaction expense(String description, String amount,
                                      TransactionCategory category, LocalDate date) {
        return Transaction.create(description, new BigDecimal(amount), TransactionType.EXPENSE, category, date);
    }

    public static Transaction income(String description, String amount,
                                     TransactionCategory category, LocalDate date) {
        return Transaction.create(description, new BigDecimal(amount), TransactionType.INCOME, category, date);
    }
}
