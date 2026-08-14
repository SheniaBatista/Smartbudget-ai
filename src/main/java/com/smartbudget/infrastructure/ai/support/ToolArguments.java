package com.smartbudget.infrastructure.ai.support;

import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.model.DateRange;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class ToolArguments {
    private ToolArguments() {
    }

    public static LocalDate optionalDate(String value, String parameterName) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new InvalidPeriodException(
                    "Valor invalido para " + parameterName + ": '" + value + "'. Use o formato AAAA-MM-DD.");
        }
    }

    public static YearMonth monthOrCurrent(String value) {
        if (isBlank(value)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new InvalidPeriodException(
                    "Mes invalido: '" + value + "'. Use o formato AAAA-MM, por exemplo 2026-08.");
        }
    }

    public static DateRange optionalRange(String from, String to) {
        LocalDate start = optionalDate(from, "a data inicial");
        LocalDate end = optionalDate(to, "a data final");

        if (start == null && end == null) {
            return null;
        }
        return DateRange.of(
                start != null ? start : LocalDate.of(1970, 1, 1),
                end != null ? end : LocalDate.now());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
