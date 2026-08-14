package com.smartbudget.infrastructure.ai.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BrazilianFormat")
class BrazilianFormatTest {
    @Test
    @DisplayName("formata valores monetarios com separador de milhar e virgula decimal")
    void formatsMoney() {
        assertThat(BrazilianFormat.money(new BigDecimal("1234.56"))).isEqualTo("R$ 1.234,56");
        assertThat(BrazilianFormat.money(new BigDecimal("85"))).isEqualTo("R$ 85,00");
        assertThat(BrazilianFormat.money(new BigDecimal("5000000.5"))).isEqualTo("R$ 5.000.000,50");
    }

    @Test
    @DisplayName("valor negativo mantem o sinal")
    void formatsNegativeMoney() {
        assertThat(BrazilianFormat.money(new BigDecimal("-150.50"))).isEqualTo("R$ -150,50");
    }

    @Test
    @DisplayName("valor nulo vira zero em vez de quebrar")
    void handlesNullMoney() {
        assertThat(BrazilianFormat.money(null)).isEqualTo("R$ 0,00");
    }

    @Test
    @DisplayName("formata percentuais")
    void formatsPercentage() {
        assertThat(BrazilianFormat.percentage(new BigDecimal("44.24"))).isEqualTo("44,24%");
        assertThat(BrazilianFormat.percentage(null)).isEqualTo("0,00%");
    }

    @Test
    @DisplayName("formata datas no padrao brasileiro")
    void formatsDate() {
        assertThat(BrazilianFormat.date(LocalDate.of(2026, 8, 14))).isEqualTo("14/08/2026");
        assertThat(BrazilianFormat.date(null)).isNull();
    }
}
