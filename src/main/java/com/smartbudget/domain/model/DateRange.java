package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidPeriodException;

import java.time.LocalDate;
import java.time.YearMonth;

public record DateRange(LocalDate from, LocalDate to) {
    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2999;

    public DateRange {
        if (from == null || to == null) {
            throw new InvalidPeriodException("As datas de inicio e fim do periodo sao obrigatorias.");
        }
        if (to.isBefore(from)) {
            throw new InvalidPeriodException(
                    "A data final (" + to + ") nao pode ser anterior a data inicial (" + from + ").");
        }
        if (from.getYear() < MIN_YEAR || to.getYear() > MAX_YEAR) {
            throw new InvalidPeriodException(
                    "O periodo deve estar entre os anos " + MIN_YEAR + " e " + MAX_YEAR + ".");
        }
    }

    public static DateRange of(LocalDate from, LocalDate to) {
        return new DateRange(from, to);
    }

    public static DateRange ofMonth(YearMonth month) {
        if (month == null) {
            throw new InvalidPeriodException("O mes de referencia e obrigatorio.");
        }
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }

    public static DateRange ofDay(LocalDate day) {
        return new DateRange(day, day);
    }

    public static DateRange allTime() {
        return new DateRange(LocalDate.of(MIN_YEAR, 1, 1), LocalDate.of(MAX_YEAR, 12, 31));
    }

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }

    @Override
    public String toString() {
        return from + " a " + to;
    }
}
