package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.PlayerValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Command executor for the /setbalance command.
 *
 * Allows administrators to set a player's balance to a specific amount.
 * Includes validation, logging, and user feedback for all operations.
 */
public class SetBalanceCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;
    private static final String LOG_PREFIX = "[CE.SetBalance]";

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;
    private final CurrencyParser currencyParser;

    /**
     * Creates a new SetBalanceCommand instance.
     *
     * @param balanceManager the balance manager for account operations (must not be null)
     * @param plugin the plugin instance for logging (must not be null)
     * @param currencyFormatter the formatter for currency display (must not be null)
     * @param playerValidator the validator for player operations (must not be null)
     * @param currencyParser the parser for currency amounts (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public SetBalanceCommand(BalanceManager balanceManager, JavaPlugin plugin,
                             CurrencyFormatter currencyFormatter, PlayerValidator playerValidator,
                             CurrencyParser currencyParser) {
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

        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.currencyFormatter = currencyFormatter;
        this.playerValidator = playerValidator;
        this.currencyParser = currencyParser;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates a new PlayerValidator and CurrencyParser instance internally.
     *
     * @deprecated Use the constructor that accepts both PlayerValidator and CurrencyParser parameters
     */
    @Deprecated
    public SetBalanceCommand(BalanceManager balanceManager, JavaPlugin plugin,
                             CurrencyFormatter currencyFormatter) {
        this(balanceManager, plugin, currencyFormatter, new PlayerValidator(), new CurrencyParser());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        try {
            // Validate sender permissions (this should typically be checked by plugin permissions)
            if (!hasPermission(sender)) {
                sender.sendMessage(errorComponent("You don't have permission to use this command."));
                return true;
            }

            // Validate arguments
            if (!validateArguments(sender, args)) {
                return true;
            }

            String playerName = args[0];
            String amountString = args[1];

            // Resolve target player
            Optional<OfflinePlayer> targetOpt = resolveTargetPlayer(sender, playerName);
            if (targetOpt.isEmpty()) {
                return true; // Error message already sent
            }

            OfflinePlayer target = targetOpt.get();

            // Parse amount
            Optional<Long> amountOpt = parseAmount(sender, amountString);
            if (amountOpt.isEmpty()) {
                return true; // Error message already sent
            }

            long amount = amountOpt.get();

            // Process the balance update
            return processBalanceUpdate(sender, target, amount);

        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing setbalance command: " + e.getMessage());
            sender.sendMessage(errorComponent("An error occurred while setting the balance."));
            return true;
        }
    }

    /**
     * Checks if the sender has permission to execute this command.
     * This is a basic implementation - in practice, you'd use a permission system.
     *
     * @param sender the command sender
     * @return true if sender has permission, false otherwise
     */
    private boolean hasPermission(CommandSender sender) {
        // Basic implementation - in a real plugin, you'd check actual permissions
        return sender.hasPermission("economy.admin.setbalance") || sender.isOp();
    }

    /**
     * Validates command arguments.
     *
     * @param sender the command sender
     * @param args the command arguments
     * @return true if arguments are valid, false otherwise
     */
    private boolean validateArguments(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendUsageMessage(sender);
            return false;
        }

        if (args[0] == null || args[0].trim().isEmpty()) {
            sender.sendMessage(errorComponent("Player name cannot be empty."));
            sendUsageMessage(sender);
            return false;
        }

        if (args[1] == null || args[1].trim().isEmpty()) {
            sender.sendMessage(errorComponent("Amount cannot be empty."));
            sendUsageMessage(sender);
            return false;
        }

        return true;
    }

    /**
     * Resolves the target player from the given name.
     *
     * @param sender the command sender
     * @param playerName the name of the target player
     * @return Optional containing the resolved player, or empty if not found
     */
    private Optional<OfflinePlayer> resolveTargetPlayer(CommandSender sender, String playerName) {
        // For console commands, we can't use the player-based resolution method
        if (sender instanceof Player player) {
            Optional<OfflinePlayer> playerOpt = playerValidator.resolvePlayer(player, playerName);
            if (playerOpt.isEmpty()) {
                sender.sendMessage(errorComponent("Player not found."));
            }
            return playerOpt;
        } else {
            // For console senders, we need a different resolution strategy
            // This is a simplified implementation - in practice, you'd have a more robust system
            sender.sendMessage(errorComponent("This command currently requires execution by a player."));
            return Optional.empty();
        }
    }

    /**
     * Parses the amount string using appropriate parser based on sender type.
     *
     * @param sender the command sender
     * @param amountString the amount string to parse
     * @return Optional containing the parsed amount, or empty if parsing failed
     */
    private Optional<Long> parseAmount(CommandSender sender, String amountString) {
        if (sender instanceof Player player) {
            return currencyParser.parseAmount(player, amountString);
        } else {
            // For console, use silent parsing
            Optional<Long> amountOpt = currencyParser.parseAmountSilently(amountString);
            if (amountOpt.isEmpty()) {
                sender.sendMessage(errorComponent("Invalid amount format. Use numbers only (e.g., 1.23)."));
            }
            return amountOpt;
        }
    }

    /**
     * Processes the balance update operation.
     *
     * @param sender the command sender
     * @param target the target player
     * @param amount the amount to set
     * @return true if the operation was successful
     */
    private boolean processBalanceUpdate(CommandSender sender, OfflinePlayer target, long amount) {
        try {
            balanceManager.setBalance(target.getUniqueId(), amount);
            sendConfirmationMessages(sender, target, amount);
            logTransaction(sender, target, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe(LOG_PREFIX + " Failed to update balance for " +
                    target.getName() + ": " + e.getMessage());
            sender.sendMessage(errorComponent("Error updating balance. Check logs for details."));
            return false;
        }
    }

    /**
     * Sends confirmation messages to sender and target player if online.
     *
     * @param sender the command sender
     * @param target the target player
     * @param amount the amount that was set
     */
    private void sendConfirmationMessages(CommandSender sender, OfflinePlayer target, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String targetName = getPlayerName(target);

        // Send confirmation to command sender
        Component senderMessage = buildAlternatingColorMessage(
                "Set ", targetName, "'s balance to ", formattedAmount
        );
        sender.sendMessage(senderMessage);

        // Send notification to target player if online
        if (target instanceof Player onlineTarget) {
            Component targetMessage = buildAlternatingColorMessage(
                    "Your balance has been set to ", formattedAmount, " by ", sender.getName()
            );
            onlineTarget.sendMessage(targetMessage);
        }
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
            TextColor color = (i % 2 == 0) ? SUCCESS_COLOR : VALUE_COLOR;
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
     * Logs the completed balance update.
     *
     * @param sender the command sender
     * @param target the target player
     * @param amount the amount that was set
     */
    private void logTransaction(CommandSender sender, OfflinePlayer target, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String targetName = getPlayerName(target);

        plugin.getLogger().info(String.format("%s %s set %s's balance to %s",
                LOG_PREFIX,
                sender.getName(),
                targetName,
                formattedAmount
        ));
    }

    /**
     * Sends usage instructions to the sender.
     *
     * @param sender the command sender
     */
    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(errorComponent("Usage: /setbalance <player> <amount>"));
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