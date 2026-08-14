package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidPeriodException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DateRange - intervalos de consulta")
class DateRangeTest {
    @Test
    @DisplayName("ofMonth cobre do primeiro ao ultimo dia do mes")
    void coversWholeMonth() {
        DateRange range = DateRange.ofMonth(YearMonth.of(2026, 2));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("ofDay produz um intervalo de um unico dia")
    void singleDayRange() {
        LocalDate today = LocalDate.of(2026, 8, 14);
        DateRange range = DateRange.ofDay(today);

        assertThat(range.from()).isEqualTo(today);
        assertThat(range.to()).isEqualTo(today);
        assertThat(range.contains(today)).isTrue();
        assertThat(range.contains(today.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("rejeita data final anterior a inicial")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> DateRange.of(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    @DisplayName("rejeita limites nulos")
    void rejectsNullBounds() {
        assertThatThrownBy(() -> DateRange.of(null, LocalDate.now()))
                .isInstanceOf(InvalidPeriodException.class);
    }

    @Test
    @DisplayName("allTime abrange qualquer data plausivel")
    void allTimeCoversEverything() {
        DateRange range = DateRange.allTime();

        assertThat(range.contains(LocalDate.of(1990, 1, 1))).isTrue();
        assertThat(range.contains(LocalDate.now())).isTrue();
    }
}
