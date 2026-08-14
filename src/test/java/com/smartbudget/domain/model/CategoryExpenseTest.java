package com.smartbudget.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryExpense - participacao por categoria")
class CategoryExpenseTest {
    @Test
    @DisplayName("calcula a participacao percentual sobre o total de despesas")
    void computesShare() {
        CategoryExpense food = new CategoryExpense(TransactionCategory.FOOD, new BigDecimal("730.00"), 12);

        assertThat(food.shareOf(new BigDecimal("1650.00"))).isEqualByComparingTo("44.24");
    }

    @Test
    @DisplayName("retorna zero quando o total de despesas e zero, sem dividir por zero")
    void handlesZeroTotal() {
        CategoryExpense food = new CategoryExpense(TransactionCategory.FOOD, BigDecimal.ZERO, 0);

        assertThat(food.shareOf(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        assertThat(food.shareOf(null)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("categoria unica representa cem por cento das despesas")
    void singleCategoryIsFullShare() {
        CategoryExpense transport = new CategoryExpense(TransactionCategory.TRANSPORT, new BigDecimal("410.00"), 5);

        assertThat(transport.shareOf(new BigDecimal("410.00"))).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("normaliza total nulo para zero")
    void normalizesNullTotal() {
        CategoryExpense expense = new CategoryExpense(TransactionCategory.OTHER, null, 0);

        assertThat(expense.total()).isEqualByComparingTo("0.00");
    }
}
