package io.github.HenriqueMichelini.craftalism_economy.core.currency;

import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    private final String currencySymbol;
    private final String fallbackValue;
    private final JavaPlugin plugin;
    private final ThreadLocal<NumberFormat> formatter;

    public static final long DECIMAL_SCALE = 10000; // 4 decimal places
    public static final BigDecimal DECIMAL_SCALE_BD = BigDecimal.valueOf(DECIMAL_SCALE);

    public CurrencyFormatter(Locale locale, String currencySymbol, String fallbackValue, JavaPlugin plugin) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale cannot be null");
        }
        if (currencySymbol == null) {
            throw new IllegalArgumentException("Currency symbol cannot be null");
        }
        if (fallbackValue == null) {
            throw new IllegalArgumentException("Fallback value cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        this.currencySymbol = currencySymbol;
        this.fallbackValue = fallbackValue;
        this.plugin = plugin;

        this.formatter = ThreadLocal.withInitial(() -> {
            NumberFormat nf = NumberFormat.getInstance(locale);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(4); // Support up to 4 decimal places
            nf.setGroupingUsed(true);
            nf.setRoundingMode(RoundingMode.HALF_UP); // Consistent rounding behavior
            return nf;
        });
    }

    public String formatCurrency(long amount) {
        try {
            // Convert to BigDecimal for precise arithmetic
            BigDecimal amountDecimal = BigDecimal.valueOf(amount);
            BigDecimal displayValue = amountDecimal.divide(DECIMAL_SCALE_BD, 4, RoundingMode.HALF_UP);

            // Use ThreadLocal formatter (no synchronization needed with ThreadLocal)
            return currencySymbol + formatter.get().format(displayValue);
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting currency amount " + amount + ": " + e.getMessage());
            return currencySymbol + fallbackValue;
        }
    }

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        try {
            return currencySymbol + formatter.get().format(amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting BigDecimal currency amount " + amount + ": " + e.getMessage());
            return currencySymbol + fallbackValue;
        }
    }

    public BigDecimal toDisplayValue(long amount) {
        return BigDecimal.valueOf(amount).divide(DECIMAL_SCALE_BD, 4, RoundingMode.HALF_UP);
    }

    public long fromDisplayValue(BigDecimal displayValue) {
        if (displayValue == null) {
            throw new IllegalArgumentException("Display value cannot be null");
        }
        return displayValue.multiply(DECIMAL_SCALE_BD).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public String getFallbackValue() {
        return fallbackValue;
    }

    public void cleanup() {
        formatter.remove();
    }
}