package com.smartbudget.application.usecase;

import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;

public record ListTransactionsQuery(DateRange range,
                                    TransactionType type,
                                    TransactionCategory category,
                                    int limit) {
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 200;

    public ListTransactionsQuery {
        limit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }

    public static ListTransactionsQuery recent(int limit) {
        return new ListTransactionsQuery(null, null, null, limit);
    }
}
