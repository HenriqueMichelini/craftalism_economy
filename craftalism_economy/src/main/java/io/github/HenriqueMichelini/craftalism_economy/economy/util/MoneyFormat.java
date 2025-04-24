package io.github.HenriqueMichelini.craftalism_economy.economy.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormat {
    private final ThreadLocal<NumberFormat> formatter;
    private final String currencySymbol;
    private final String nullValue;

    public MoneyFormat(Locale locale, String currencySymbol, String nullValue) {
        this.currencySymbol = currencySymbol;
        this.nullValue = nullValue;

        this.formatter = ThreadLocal.withInitial(() -> {
            NumberFormat nf = NumberFormat.getInstance(locale);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            nf.setGroupingUsed(true);
            return nf;
        });
    }

    public String formatPrice(BigDecimal amount) {
        if (amount == null) {
            return currencySymbol + nullValue;
        }

        try {
            synchronized (formatter.get()) {
                return currencySymbol + formatter.get().format(amount);
            }
        } catch (Exception e) {
            return currencySymbol + nullValue;
        }
    }
}