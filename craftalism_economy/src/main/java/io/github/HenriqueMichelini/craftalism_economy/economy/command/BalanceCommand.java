package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.PlayerValidator;
import net.kyori.adventure.text.Component;
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
import java.util.UUID;

/**
 * Command executor for the /balance command.
 *
 * Allows players to check their own balance or another player's balance.
 * Supports both online and offline player lookups.
 */
public class BalanceCommand implements CommandExecutor {
    private static final TextColor PREFIX_COLOR = NamedTextColor.GREEN;
    private static final TextColor BALANCE_COLOR = NamedTextColor.WHITE;
    private static final TextColor ERROR_COLOR = NamedTextColor.RED;
    private static final String LOG_PREFIX = "[CE.Balance]";

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;

    /**
     * Creates a new BalanceCommand instance.
     *
     * @param balanceManager the balance manager for retrieving balances (must not be null)
     * @param plugin the plugin instance for logging (must not be null)
     * @param currencyFormatter the formatter for currency display (must not be null)
     * @param playerValidator the validator for player operations (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin,
                          CurrencyFormatter currencyFormatter, PlayerValidator playerValidator) {
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

        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.currencyFormatter = currencyFormatter;
        this.playerValidator = playerValidator;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates a new PlayerValidator instance internally.
     *
     * @deprecated Use the constructor that accepts PlayerValidator parameter
     */
    @Deprecated
    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin, CurrencyFormatter currencyFormatter) {
        this(balanceManager, plugin, currencyFormatter, new PlayerValidator());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        // Validate that sender is a player
        if (!playerValidator.isSenderAPlayer(sender)) {
            sender.sendMessage(errorComponent("This command can only be used by players."));
            return true; // Return true to prevent showing usage from plugin.yml
        }

        Player player = (Player) sender;

        try {
            // Handle different argument counts
            switch (args.length) {
                case 0:
                    return handleOwnBalance(player);
                case 1:
                    return handleOtherBalance(player, args[0]);
                default:
                    sendUsageMessage(player);
                    return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing balance command: " + e.getMessage());
            player.sendMessage(errorComponent("An error occurred while retrieving the balance."));
            return true;
        }
    }

    /**
     * Handles the case where a player checks their own balance.
     *
     * @param player the player checking their balance
     * @return true if the command was handled successfully
     */
    private boolean handleOwnBalance(Player player) {
        UUID playerUuid = player.getUniqueId();

        // Check if balance exists
        if (!balanceManager.checkIfBalanceExists(playerUuid)) {
            player.sendMessage(errorComponent("You don't have a balance yet. Contact an administrator."));
            logBalanceNotFound(player.getName());
            return true;
        }

        // Get and display balance
        long balance = balanceManager.getBalance(playerUuid);
        sendOwnBalanceMessage(player, balance);
        logShowOwnBalance(player.getName());
        return true;
    }

    /**
     * Handles the case where a player checks another player's balance.
     *
     * @param requester the player making the request
     * @param targetName the name of the target player
     * @return true if the command was handled successfully
     */
    private boolean handleOtherBalance(Player requester, String targetName) {
        // Validate target name
        if (targetName == null || targetName.trim().isEmpty()) {
            sendPlayerNotFoundMessage(requester);
            sendUsageMessage(requester);
            return true;
        }

        // Resolve target player
        Optional<OfflinePlayer> targetPlayer = playerValidator.resolvePlayer(requester, targetName);
        if (targetPlayer.isEmpty()) {
            sendPlayerNotFoundMessage(requester);
            return true;
        }

        UUID targetUuid = targetPlayer.get().getUniqueId();

        // Check if target has a balance
        if (!balanceManager.checkIfBalanceExists(targetUuid)) {
            requester.sendMessage(errorComponent("Player " + targetName + " doesn't have a balance."));
            return true;
        }

        // Get and display balance
        long balance = balanceManager.getBalance(targetUuid);
        sendOtherBalanceMessage(requester, targetName, balance);
        logShowOtherBalance(requester.getName(), targetName);
        return true;
    }

    /**
     * Sends the player's own balance message.
     *
     * @param player the player to send the message to
     * @param balance the balance amount
     */
    private void sendOwnBalanceMessage(Player player, long balance) {
        String formatted = currencyFormatter.formatCurrency(balance);
        Component message = Component.text("Your balance is: ")
                .color(PREFIX_COLOR)
                .append(Component.text(formatted).color(BALANCE_COLOR));
        player.sendMessage(message);
    }

    /**
     * Sends another player's balance message.
     *
     * @param recipient the player to send the message to
     * @param targetName the name of the target player
     * @param balance the balance amount
     */
    private void sendOtherBalanceMessage(Player recipient, String targetName, long balance) {
        String formatted = currencyFormatter.formatCurrency(balance);
        Component message = Component.text(targetName + "'s balance is: ")
                .color(PREFIX_COLOR)
                .append(Component.text(formatted).color(BALANCE_COLOR));
        recipient.sendMessage(message);
    }

    /**
     * Sends usage instructions to the player.
     *
     * @param player the player to send the message to
     */
    private void sendUsageMessage(Player player) {
        player.sendMessage(errorComponent("Usage: /balance [player]"));
    }

    /**
     * Sends a player not found message.
     *
     * @param player the player to send the message to
     */
    private void sendPlayerNotFoundMessage(Player player) {
        player.sendMessage(errorComponent("Player not found."));
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

    /**
     * Logs when a player checks their own balance.
     *
     * @param requester the name of the requesting player
     */
    private void logShowOwnBalance(String requester) {
        plugin.getLogger().info(LOG_PREFIX + " " + requester + " checked their own balance");
    }

    /**
     * Logs when a player checks another player's balance.
     *
     * @param requester the name of the requesting player
     * @param target the name of the target player
     */
    private void logShowOtherBalance(String requester, String target) {
        plugin.getLogger().info(LOG_PREFIX + " " + requester + " checked " + target + "'s balance");
    }

    /**
     * Logs when a balance is not found for a player.
     *
     * @param playerName the name of the player
     */
    private void logBalanceNotFound(String playerName) {
        plugin.getLogger().info(LOG_PREFIX + " Balance not found for player: " + playerName);
    }
}