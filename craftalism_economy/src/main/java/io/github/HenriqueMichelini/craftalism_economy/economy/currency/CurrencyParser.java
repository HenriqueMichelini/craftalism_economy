package io.github.HenriqueMichelini.craftalism_economy.economy.currency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Parses user input currency amounts and converts them to internal long representation.
 *
 * This class handles the conversion from user-friendly decimal strings (like "1.23")
 * to the internal scaled long format used by the economy system.
 */
public class CurrencyParser {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;

    // Maximum allowed value to prevent overflow when scaling
    private static final BigDecimal MAX_ALLOWED_VALUE = new BigDecimal(Long.MAX_VALUE)
            .divide(CurrencyFormatter.DECIMAL_SCALE_BD, 4, RoundingMode.DOWN);

    /**
     * Parses a string amount into the internal long representation.
     *
     * @param player the player to send error messages to (must not be null)
     * @param input the input string to parse (must not be null)
     * @return Optional containing the parsed amount, or empty if parsing failed
     * @throws IllegalArgumentException if player or input is null
     */
    public static Optional<Long> parseAmount(Player player, String input) {
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

            // Remove common currency symbols if present
            String cleaned = removeCurrencySymbols(trimmed);

            if (cleaned.isEmpty()) {
                sendErrorMessage(player, "Invalid amount format. Use numbers only.");
                return Optional.empty();
            }

            BigDecimal decimal = new BigDecimal(cleaned);

            // Check for negative values
            if (decimal.signum() < 0) {
                sendErrorMessage(player, "Amount cannot be negative.");
                return Optional.empty();
            }

            // Check for zero
            if (decimal.signum() == 0) {
                sendErrorMessage(player, "Amount must be greater than zero.");
                return Optional.empty();
            }

            // Check if value is too large to prevent overflow
            if (decimal.compareTo(MAX_ALLOWED_VALUE) > 0) {
                sendErrorMessage(player, "Amount is too large.");
                return Optional.empty();
            }

            // Scale to internal representation
            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

            // Check for too many decimal places (more than 4)
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

    /**
     * Parses a string amount with custom error messages.
     *
     * @param player the player to send messages to
     * @param input the input string to parse
     * @param sendMessages whether to send error messages to the player
     * @return Optional containing the parsed amount, or empty if parsing failed
     * @throws IllegalArgumentException if player or input is null
     */
    public static Optional<Long> parseAmount(Player player, String input, boolean sendMessages) {
        if (!sendMessages) {
            return parseAmountSilently(input);
        }
        return parseAmount(player, input);
    }

    /**
     * Parses a string amount without sending any messages to a player.
     *
     * @param input the input string to parse
     * @return Optional containing the parsed amount, or empty if parsing failed
     * @throws IllegalArgumentException if input is null
     */
    public static Optional<Long> parseAmountSilently(String input) {
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

            // Check for negative or zero values
            if (decimal.signum() <= 0) {
                return Optional.empty();
            }

            // Check if value is too large
            if (decimal.compareTo(MAX_ALLOWED_VALUE) > 0) {
                return Optional.empty();
            }

            // Scale to internal representation
            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

            // Check for too many decimal places
            if (scaled.scale() > 0 && scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                return Optional.empty();
            }

            long amount = scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Optional.of(amount);

        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    /**
     * Validates that an input string represents a valid currency amount.
     *
     * @param input the input string to validate
     * @return true if the input is a valid currency amount, false otherwise
     */
    public static boolean isValidAmount(String input) {
        return parseAmountSilently(input).isPresent();
    }

    /**
     * Gets the maximum allowed value that can be parsed.
     *
     * @return the maximum allowed BigDecimal value
     */
    public static BigDecimal getMaxAllowedValue() {
        return MAX_ALLOWED_VALUE;
    }

    /**
     * Removes common currency symbols from the input string.
     *
     * @param input the input string
     * @return the cleaned string with currency symbols removed
     */
    private static String removeCurrencySymbols(String input) {
        // Remove common currency symbols and whitespace
        return input.replaceAll("[$€£¥₹₽₩¢]", "").trim();
    }

    /**
     * Sends an error message to the player.
     *
     * @param player the player to send the message to
     * @param message the error message
     */
    private static void sendErrorMessage(Player player, String message) {
        player.sendMessage(Component.text(message).color(ERROR_COLOR));
    }

    /**
     * Sends a success message to the player.
     *
     * @param player the player to send the message to
     * @param message the success message
     */
    private static void sendSuccessMessage(Player player, String message) {
        player.sendMessage(Component.text(message).color(SUCCESS_COLOR));
    }
}