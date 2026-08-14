package com.smartbudget.infrastructure.ai.support;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BrazilianFormat {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BrazilianFormat() {
    }

    public static String money(BigDecimal value) {
        return "R$ " + decimalFormat().format(value != null ? value : BigDecimal.ZERO);
    }

    public static String percentage(BigDecimal value) {
        return decimalFormat().format(value != null ? value : BigDecimal.ZERO) + "%";
    }

    public static String date(LocalDate value) {
        return value != null ? value.format(DATE) : null;
    }

    private static DecimalFormat decimalFormat() {
        return new DecimalFormat("#,##0.00", new DecimalFormatSymbols(PT_BR));
    }
}
