package io.github.HenriqueMichelini.craftalism_economy.economy.currency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class CurrencyParser {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;

    private static final BigDecimal MAX_ALLOWED_VALUE = new BigDecimal(Long.MAX_VALUE)
            .divide(CurrencyFormatter.DECIMAL_SCALE_BD, 4, RoundingMode.DOWN);

    public Optional<Long> parseAmount(Player player, String input) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        try {
            String trimmed = input.trim();

            if (trimmed.isEmpty()) {
                sendErrorMessage(player, "Amount cannot be empty.");
                return Optional.empty();
            }

            String cleaned = removeCurrencySymbols(trimmed);

            if (cleaned.isEmpty()) {
                sendErrorMessage(player, "Invalid amount format. Use numbers only.");
                return Optional.empty();
            }

            BigDecimal decimal = new BigDecimal(cleaned);

            if (decimal.signum() < 0) {
                sendErrorMessage(player, "Amount cannot be negative.");
                return Optional.empty();
            }

            if (decimal.signum() == 0) {
                sendErrorMessage(player, "Amount must be greater than zero.");
                return Optional.empty();
            }

            if (decimal.compareTo(MAX_ALLOWED_VALUE) > 0) {
                sendErrorMessage(player, "Amount is too large.");
                return Optional.empty();
            }

            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

            if (scaled.scale() > 0 && scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                sendErrorMessage(player, "Invalid amount. Maximum 4 decimal places allowed.");
                return Optional.empty();
            }

            long amount = scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Optional.of(amount);

        } catch (NumberFormatException e) {
            sendErrorMessage(player, "Invalid amount format. Use numbers only (e.g., 1.23).");
            return Optional.empty();
        } catch (ArithmeticException e) {
            sendErrorMessage(player, "Amount value is out of valid range.");
            return Optional.empty();
        }
    }

    public Optional<Long> parseAmount(Player player, String input, boolean sendMessages) {
        if (!sendMessages) {
            return parseAmountSilently(input);
        }
        return parseAmount(player, input);
    }

    public Optional<Long> parseAmountSilently(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        try {
            String trimmed = input.trim();

            if (trimmed.isEmpty()) {
                return Optional.empty();
            }

            String cleaned = removeCurrencySymbols(trimmed);

            if (cleaned.isEmpty()) {
                return Optional.empty();
            }

            BigDecimal decimal = new BigDecimal(cleaned);

            if (decimal.signum() <= 0) {
                return Optional.empty();
            }

            if (decimal.compareTo(MAX_ALLOWED_VALUE) > 0) {
                return Optional.empty();
            }

            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

            if (scaled.scale() > 0 && scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                return Optional.empty();
            }

            long amount = scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Optional.of(amount);

        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    public boolean isValidAmount(String input) {
        return parseAmountSilently(input).isPresent();
    }

    public BigDecimal getMaxAllowedValue() {
        return MAX_ALLOWED_VALUE;
    }

    private String removeCurrencySymbols(String input) {
        return input.replaceAll("[$€£¥₹₽₩¢]", "").trim();
    }

    private void sendErrorMessage(Player player, String message) {
        player.sendMessage(Component.text(message).color(ERROR_COLOR));
    }

}