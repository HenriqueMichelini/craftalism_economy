package io.github.HenriqueMichelini.craftalism_economy.economy.currency;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    private final String currencySymbol;
    private final String fallbackValue;
    private final JavaPlugin plugin;
    private final ThreadLocal<NumberFormat> formatter;
    public static final long DECIMAL_SCALE = 10000; // 4 decimal places
    public static final BigDecimal DECIMAL_SCALE_BD = BigDecimal.valueOf(DECIMAL_SCALE);

    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;

    public CurrencyFormatter(Locale locale, String currencySymbol, String fallbackValue, JavaPlugin plugin) {
        this.currencySymbol = currencySymbol;
        this.fallbackValue = fallbackValue;
        this.plugin = plugin;

        this.formatter = ThreadLocal.withInitial(() -> {
            NumberFormat nf = NumberFormat.getInstance(locale);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            nf.setGroupingUsed(true);
            return nf;
        });
    }

    public String formatCurrency(long amount) {
        try {
            synchronized (formatter.get()) {
                long roundedAmount = Math.round((double) amount / 100);
                double displayValue = (double) roundedAmount / 100;
                return currencySymbol + formatter.get().format(displayValue);
            }
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
            return currencySymbol + fallbackValue;
        }
    }
}
