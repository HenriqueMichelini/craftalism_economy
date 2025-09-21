package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.PlayerValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Command executor for the /pay command.
 *
 * Allows players to transfer money to other players in the economy system.
 * Includes validation, logging, and user feedback for all operations.
 */
public class PayCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;
    private static final String LOG_PREFIX = "[CE.Pay]";

    private final EconomyManager economyManager;
    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;
    private final CurrencyParser currencyParser;

    /**
     * Creates a new PayCommand instance.
     *
     * @param economyManager the economy manager for processing payments (must not be null)
     * @param balanceManager the balance manager for account operations (must not be null)
     * @param plugin the plugin instance for logging (must not be null)
     * @param currencyFormatter the formatter for currency display (must not be null)
     * @param playerValidator the validator for player operations (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public PayCommand(EconomyManager economyManager, BalanceManager balanceManager,
                      JavaPlugin plugin, CurrencyFormatter currencyFormatter,
                      PlayerValidator playerValidator, CurrencyParser currencyParser) {
        if (economyManager == null) {
            throw new IllegalArgumentException("EconomyManager cannot be null");
        }
        if (balanceManager == null) {
            throw new IllegalArgumentException("BalanceManager cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (currencyFormatter == null) {
            throw new IllegalArgumentException("CurrencyFormatter cannot be null");
        }
        if (playerValidator == null) {
            throw new IllegalArgumentException("PlayerValidator cannot be null");
        }
        if (currencyParser == null) {
            throw new IllegalArgumentException("CurrencyParser cannot be null");
        }

        this.economyManager = economyManager;
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.currencyFormatter = currencyFormatter;
        this.playerValidator = playerValidator;
        this.currencyParser = currencyParser;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates a new PlayerValidator instance internally.
     *
     * @deprecated Use the constructor that accepts PlayerValidator parameter
     */
    @Deprecated
    public PayCommand(EconomyManager economyManager, BalanceManager balanceManager,
                      JavaPlugin plugin, CurrencyFormatter currencyFormatter, CurrencyParser currencyParser) {
        this(economyManager, balanceManager, plugin, currencyFormatter, new PlayerValidator(), currencyParser);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        // Validate sender is a player
        if (!playerValidator.isSenderAPlayer(sender)) {
            sender.sendMessage(errorComponent("This command can only be used by players."));
            return true;
        }

        Player payer = (Player) sender;

        try {
            // Validate argument count
            if (!validateArguments(payer, args)) {
                return true;
            }

            // Resolve recipient player
            Optional<OfflinePlayer> payeeOpt = resolvePayee(payer, args[0]);
            if (payeeOpt.isEmpty()) {
                return true; // Error message already sent
            }

            OfflinePlayer payee = payeeOpt.get();
            UUID payeeUuid = payee.getUniqueId();

            // Check self-payment
            if (!validateNotSelfPayment(payer, payeeUuid)) {
                return true;
            }

            // Parse and validate amount
            Optional<Long> amountOpt = currencyParser.parseAmount(payer, args[1]);
            if (amountOpt.isEmpty()) {
                return true; // Error message already sent by parser
            }

            long amount = amountOpt.get();

            // Process the payment
            if (!processPayment(payer, payeeUuid, amount)) {
                return true; // Error message already sent
            }

            // Send success messages and log
            sendSuccessMessages(payer, payee, amount);
            logTransaction(payer, payee, amount);

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing pay command: " + e.getMessage());
            payer.sendMessage(errorComponent("An error occurred while processing the payment."));
            return true;
        }
    }

    /**
     * Validates command arguments.
     *
     * @param payer the player executing the command
     * @param args the command arguments
     * @return true if arguments are valid, false otherwise
     */
    private boolean validateArguments(Player payer, String[] args) {
        if (args.length != 2) {
            sendUsageMessage(payer);
            return false;
        }

        if (args[0] == null || args[0].trim().isEmpty()) {
            payer.sendMessage(errorComponent("Player name cannot be empty."));
            sendUsageMessage(payer);
            return false;
        }

        if (args[1] == null || args[1].trim().isEmpty()) {
            payer.sendMessage(errorComponent("Amount cannot be empty."));
            sendUsageMessage(payer);
            return false;
        }

        return true;
    }

    /**
     * Resolves the payee player from the given name.
     *
     * @param payer the player making the request
     * @param payeeName the name of the payee
     * @return Optional containing the resolved player, or empty if not found
     */
    private Optional<OfflinePlayer> resolvePayee(Player payer, String payeeName) {
        Optional<OfflinePlayer> playerOpt = playerValidator.resolvePlayer(payer, payeeName);

        if (playerOpt.isEmpty()) {
            payer.sendMessage(errorComponent("Player not found."));
            return Optional.empty();
        }

        OfflinePlayer player = playerOpt.get();
        UUID playerUuid = player.getUniqueId();

        // Check if the target player has a balance account
        if (!balanceManager.checkIfBalanceExists(playerUuid)) {
            payer.sendMessage(errorComponent("Player " + payeeName + " doesn't have an account in the economy system."));
            return Optional.empty();
        }

        return playerOpt;
    }

    /**
     * Validates that the player is not trying to pay themselves.
     *
     * @param payer the player making the payment
     * @param payeeUuid the UUID of the payment recipient
     * @return true if not a self-payment, false otherwise
     */
    private boolean validateNotSelfPayment(Player payer, UUID payeeUuid) {
        if (payer.getUniqueId().equals(payeeUuid)) {
            payer.sendMessage(errorComponent("You cannot pay yourself."));
            return false;
        }
        return true;
    }

    /**
     * Processes the payment transaction.
     *
     * @param payer the player making the payment
     * @param payeeUuid the UUID of the payment recipient
     * @param amount the amount to transfer
     * @return true if payment was successful, false otherwise
     */
    private boolean processPayment(Player payer, UUID payeeUuid, long amount) {
        boolean success = economyManager.transferBalance(payer.getUniqueId(), payeeUuid, amount);

        if (!success) {
            payer.sendMessage(errorComponent("Transaction failed. You may not have sufficient funds."));
            return false;
        }

        return true;
    }

    /**
     * Sends success messages to both payer and payee.
     *
     * @param payer the player who made the payment
     * @param payee the player who received the payment
     * @param amount the amount that was transferred
     */
    private void sendSuccessMessages(Player payer, OfflinePlayer payee, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String payeeName = getPlayerName(payee);

        // Send message to payer
        Component payerMessage = buildPaymentMessage(
                "You paid ", formattedAmount, " to ", payeeName
        );
        payer.sendMessage(payerMessage);

        // Send message to payee if online
        if (payee instanceof Player onlinePayee) {
            Component payeeMessage = buildReceivedMessage(
                    "You received ", formattedAmount, " from ", payer.getName()
            );
            onlinePayee.sendMessage(payeeMessage);
        }
    }

    /**
     * Builds a payment confirmation message.
     *
     * @param parts alternating text and value parts
     * @return the formatted component
     */
    private Component buildPaymentMessage(String... parts) {
        return buildAlternatingColorMessage(parts);
    }

    /**
     * Builds a payment received message.
     *
     * @param parts alternating text and value parts
     * @return the formatted component
     */
    private Component buildReceivedMessage(String... parts) {
        return buildAlternatingColorMessage(parts);
    }

    /**
     * Builds a message with alternating colors for text and values.
     *
     * @param parts the message parts
     * @return the formatted component
     */
    private Component buildAlternatingColorMessage(String... parts) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < parts.length; i++) {
            NamedTextColor color = (i % 2 == 0) ? SUCCESS_COLOR : VALUE_COLOR;
            builder.append(Component.text(parts[i]).color(color));
        }

        return builder.build();
    }

    /**
     * Gets the display name of a player.
     *
     * @param player the player
     * @return the player's name or "Unknown Player" if not available
     */
    private String getPlayerName(OfflinePlayer player) {
        String name = player.getName();
        return (name != null && !name.isEmpty()) ? name : "Unknown Player";
    }

    /**
     * Logs the completed transaction.
     *
     * @param payer the player who made the payment
     * @param payee the player who received the payment
     * @param amount the amount that was transferred
     */
    private void logTransaction(Player payer, OfflinePlayer payee, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String payeeName = getPlayerName(payee);

        plugin.getLogger().info(String.format("%s %s paid %s %s",
                LOG_PREFIX,
                payer.getName(),
                payeeName,
                formattedAmount
        ));
    }

    /**
     * Sends usage instructions to the player.
     *
     * @param player the player to send the message to
     */
    private void sendUsageMessage(Player player) {
        player.sendMessage(errorComponent("Usage: /pay <player> <amount>"));
    }

    /**
     * Creates an error component with the specified text.
     *
     * @param text the error message text
     * @return the formatted error component
     */
    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }
}