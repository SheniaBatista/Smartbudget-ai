package com.smartbudget.infrastructure.persistence.repository;

import com.smartbudget.domain.model.TransactionCategory;

import java.math.BigDecimal;

public interface CategoryTotalProjection {
    TransactionCategory getCategory();

    BigDecimal getTotal();

    Long getTransactionCount();
}
