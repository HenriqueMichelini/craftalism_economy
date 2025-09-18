package io.github.HenriqueMichelini.craftalism_economy.economy.currency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Optional;

public class CurrencyParser {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;

    public static Optional<Long> parseAmount(Player payer, String input) {
        try {
            input = input.trim();
            BigDecimal decimal = new BigDecimal(input);
            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

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
