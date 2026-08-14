package com.smartbudget.infrastructure.ai.support;

import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.model.DateRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ToolArguments - conversao dos argumentos vindos do modelo")
class ToolArgumentsTest {
    @Test
    @DisplayName("converte data ISO e ignora espacos")
    void parsesDate() {
        assertThat(ToolArguments.optionalDate("2026-08-14", "a data")).isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(ToolArguments.optionalDate("  2026-08-14  ", "a data")).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    @DisplayName("data ausente vira nulo, sinalizando parametro opcional")
    void absentDateIsNull() {
        assertThat(ToolArguments.optionalDate(null, "a data")).isNull();
        assertThat(ToolArguments.optionalDate("   ", "a data")).isNull();
    }

    @Test
    @DisplayName("data em formato brasileiro e recusada com orientacao de formato")
    void rejectsNonIsoDate() {
        assertThatThrownBy(() -> ToolArguments.optionalDate("14/08/2026", "a data inicial"))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessageContaining("AAAA-MM-DD")
                .hasMessageContaining("a data inicial");
    }

    @Test
    @DisplayName("mes ausente assume o mes corrente")
    void monthDefaultsToCurrent() {
        assertThat(ToolArguments.monthOrCurrent(null)).isEqualTo(YearMonth.now());
        assertThat(ToolArguments.monthOrCurrent("")).isEqualTo(YearMonth.now());
    }

    @Test
    @DisplayName("converte mes no formato AAAA-MM")
    void parsesMonth() {
        assertThat(ToolArguments.monthOrCurrent("2026-08")).isEqualTo(YearMonth.of(2026, 8));
    }

    @Test
    @DisplayName("mes escrito por extenso e recusado com orientacao de formato")
    void rejectsTextualMonth() {
        assertThatThrownBy(() -> ToolArguments.monthOrCurrent("agosto"))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessageContaining("AAAA-MM");
    }

    @Test
    @DisplayName("intervalo sem limites vira nulo, indicando todo o historico")
    void emptyRangeIsNull() {
        assertThat(ToolArguments.optionalRange(null, null)).isNull();
    }

    @Test
    @DisplayName("intervalo completo respeita os dois limites")
    void buildsFullRange() {
        DateRange range = ToolArguments.optionalRange("2026-08-01", "2026-08-31");

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("apenas a data inicial fecha o intervalo em hoje")
    void openEndedRangeClosesToday() {
        DateRange range = ToolArguments.optionalRange("2026-01-01", null);

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(range.to()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("intervalo invertido e recusado")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> ToolArguments.optionalRange("2026-08-31", "2026-08-01"))
                .isInstanceOf(InvalidPeriodException.class);
    }
}
