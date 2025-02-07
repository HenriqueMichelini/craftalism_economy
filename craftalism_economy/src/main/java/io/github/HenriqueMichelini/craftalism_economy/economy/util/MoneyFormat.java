package io.github.HenriqueMichelini.craftalism_economy.economy.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormat {
    public String formatPrice(BigDecimal price) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.GERMANY); // Uses . for thousands and , for decimals
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "$" + formatter.format(price.doubleValue()); // Use € symbol (or $ if preferred)
    }
}
