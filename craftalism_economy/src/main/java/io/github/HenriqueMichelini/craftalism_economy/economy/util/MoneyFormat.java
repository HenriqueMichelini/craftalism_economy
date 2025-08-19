package io.github.HenriqueMichelini.craftalism_economy.economy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class MoneyFormat {
    private final ThreadLocal<NumberFormat> formatter;
    private final String currencySymbol;
    private final String nullValue;
    public static final long DECIMAL_SCALE = 10000; // 4 decimal places
    public static final BigDecimal DECIMAL_SCALE_BD = BigDecimal.valueOf(DECIMAL_SCALE);

    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;

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

    public String formatPrice(long amount) {
        try {
            synchronized (formatter.get()) {
                long roundedAmount = Math.round((double) amount / 100);
                double displayValue = (double) roundedAmount / 100;
                return currencySymbol + formatter.get().format(displayValue);
            }
        } catch (Exception e) {
            return currencySymbol + nullValue;
        }
    }

    //consider making a class only for parseAmount and stuff like this
    public Optional<Long> parseAmount(Player payer, String input) {
        try {
            input = input.trim();
            BigDecimal decimal = new BigDecimal(input);
            BigDecimal scaled = decimal.multiply(MoneyFormat.DECIMAL_SCALE_BD);

            if (scaled.signum() < 0) {
                payer.sendMessage(Component.text("Balance cannot be negative.").color(ERROR_COLOR));
                return Optional.empty();
            }

            if (scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                payer.sendMessage(Component.text("Invalid amount. Too many decimal places.").color(ERROR_COLOR));
                return Optional.empty();
            }

            long amount = scaled.longValueExact();

            return Optional.of(amount);
        } catch (NumberFormatException e) {
            payer.sendMessage(Component.text("Invalid amount format. Use numbers only.")
                    .color(ERROR_COLOR));
            return Optional.empty();
        }
    }
}