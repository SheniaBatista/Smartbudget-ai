package com.smartbudget.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Balance - calculo de saldo")
class BalanceTest {
    @Test
    @DisplayName("saldo e a diferenca entre receitas e despesas")
    void computesNetBalance() {
        Balance balance = Balance.of(new BigDecimal("5000.00"), new BigDecimal("1650.00"));

        assertThat(balance.totalIncome()).isEqualByComparingTo("5000.00");
        assertThat(balance.totalExpense()).isEqualByComparingTo("1650.00");
        assertThat(balance.netBalance()).isEqualByComparingTo("3350.00");
        assertThat(balance.isPositive()).isTrue();
    }

    @Test
    @DisplayName("saldo negativo quando as despesas superam as receitas")
    void supportsNegativeBalance() {
        Balance balance = Balance.of(new BigDecimal("100.00"), new BigDecimal("250.50"));

        assertThat(balance.netBalance()).isEqualByComparingTo("-150.50");
        assertThat(balance.isPositive()).isFalse();
    }

    @Test
    @DisplayName("trata valores nulos como zero")
    void treatsNullAsZero() {
        Balance balance = Balance.of(null, null);

        assertThat(balance.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(balance.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(balance.netBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("normaliza a escala monetaria para duas casas")
    void normalizesScale() {
        Balance balance = Balance.of(new BigDecimal("10.1"), new BigDecimal("5"));

        assertThat(balance.totalIncome().scale()).isEqualTo(2);
        assertThat(balance.totalExpense().scale()).isEqualTo(2);
        assertThat(balance.netBalance()).isEqualByComparingTo("5.10");
    }

    @Test
    @DisplayName("balance vazio parte de zero")
    void emptyBalance() {
        assertThat(Balance.empty().netBalance()).isEqualByComparingTo("0.00");
    }
}
