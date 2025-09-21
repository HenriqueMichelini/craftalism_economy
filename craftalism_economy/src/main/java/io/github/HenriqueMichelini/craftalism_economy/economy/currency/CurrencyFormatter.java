package io.github.HenriqueMichelini.craftalism_economy.economy.currency;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats currency amounts for display with proper locale support and error handling.
 *
 * This class converts internal long values (stored as smallest currency units)
 * to formatted display strings with appropriate decimal places and locale-specific formatting.
 */
public class CurrencyFormatter {
    private final String currencySymbol;
    private final String fallbackValue;
    private final JavaPlugin plugin;
    private final ThreadLocal<NumberFormat> formatter;

    /** Scale factor for converting between internal long values and display decimals (4 decimal places) */
    public static final long DECIMAL_SCALE = 10000; // 4 decimal places
    /** BigDecimal representation of the scale factor for precise calculations */
    public static final BigDecimal DECIMAL_SCALE_BD = BigDecimal.valueOf(DECIMAL_SCALE);

    /**
     * Creates a new CurrencyFormatter with the specified configuration.
     *
     * @param locale the locale for number formatting (must not be null)
     * @param currencySymbol the currency symbol to prepend (must not be null)
     * @param fallbackValue the fallback value to display on errors (must not be null)
     * @param plugin the plugin instance for logging (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
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

    /**
     * Formats a currency amount for display.
     *
     * The input amount represents the value in the smallest currency units.
     * For example, if DECIMAL_SCALE is 10000, then:
     * - 10000 represents 1.0000 in display value
     * - 15000 represents 1.5000 in display value
     * - 12345 represents 1.2345 in display value
     *
     * @param amount the amount in the smallest currency units
     * @return formatted currency string with symbol prefix
     */
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

    /**
     * Formats a currency amount using BigDecimal for maximum precision.
     *
     * @param amount the amount as a BigDecimal
     * @return formatted currency string with symbol prefix
     * @throws IllegalArgumentException if amount is null
     */
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

    /**
     * Converts a long amount to its display value as a BigDecimal.
     *
     * @param amount the amount in smallest currency units
     * @return the display value as a BigDecimal
     */
    public BigDecimal toDisplayValue(long amount) {
        return BigDecimal.valueOf(amount).divide(DECIMAL_SCALE_BD, 4, RoundingMode.HALF_UP);
    }

    /**
     * Converts a display value to internal long representation.
     *
     * @param displayValue the display value as a BigDecimal
     * @return the amount in smallest currency units
     * @throws IllegalArgumentException if displayValue is null
     */
    public long fromDisplayValue(BigDecimal displayValue) {
        if (displayValue == null) {
            throw new IllegalArgumentException("Display value cannot be null");
        }
        return displayValue.multiply(DECIMAL_SCALE_BD).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * Gets the currency symbol used by this formatter.
     *
     * @return the currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Gets the fallback value used when formatting errors occur.
     *
     * @return the fallback value
     */
    public String getFallbackValue() {
        return fallbackValue;
    }

    /**
     * Cleans up ThreadLocal resources when the formatter is no longer needed.
     * This method should be called when the formatter is being disposed of.
     */
    public void cleanup() {
        formatter.remove();
    }
}