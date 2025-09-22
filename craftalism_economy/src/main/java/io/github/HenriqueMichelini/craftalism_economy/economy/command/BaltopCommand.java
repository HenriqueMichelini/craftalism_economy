package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Command executor for the /baltop command.
 *
 * Displays the top richest players on the server, ranked by their account balance.
 * Only available to players (not console) to ensure proper formatting display.
 */
public class BaltopCommand implements CommandExecutor {

    // Configuration constants
    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final String LOG_PREFIX = "[CE.Baltop]";
    private static final String UNKNOWN_PLAYER_NAME = "Unknown Player";

    // Color scheme constants
    private static final NamedTextColor HEADER_COLOR = NamedTextColor.GOLD;
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final TextColor RANK_COLOR = TextColor.color(0xFFFF55); // Bright yellow
    private static final TextColor NAME_COLOR = TextColor.color(0x55FF55); // Bright green
    private static final TextColor SEPARATOR_COLOR = TextColor.color(0xFFFFFF); // White
    private static final TextColor BALANCE_COLOR = TextColor.color(0x55FFFF); // Bright cyan

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final Logger logger;

    /**
     * Creates a new BaltopCommand instance.
     *
     * @param balanceManager the balance manager for retrieving player balances (must not be null)
     * @param plugin the plugin instance for logging (must not be null)
     * @param currencyFormatter the formatter for currency display (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public BaltopCommand(@NotNull BalanceManager balanceManager,
                         @NotNull JavaPlugin plugin,
                         @NotNull CurrencyFormatter currencyFormatter) {
        this.balanceManager = Objects.requireNonNull(balanceManager, "BalanceManager cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.currencyFormatter = Objects.requireNonNull(currencyFormatter, "CurrencyFormatter cannot be null");
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        try {
            if (!isPlayerSender(sender)) return true;

            Player player = (Player) sender;

            List<BalanceEntry> topBalances = getTopBalances();
            if (topBalances.isEmpty()) {
                sendNoDataMessage(player);
                logCommandUsage(player);
                return true;
            }

            sendBaltopDisplay(player, topBalances);
            logCommandUsage(player);

            return true;

        } catch (Exception e) {
            logger.warning(LOG_PREFIX + " Error executing baltop command: " + e.getMessage());
            sender.sendMessage(errorComponent("An error occurred while retrieving balance rankings."));
            return true;
        }
    }

    /**
     * Validates that the sender is a player.
     *
     * @param sender the command sender
     * @return true if sender is a player, false otherwise
     */
    private boolean isPlayerSender(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }

        sender.sendMessage(errorComponent("This command is only available to players."));
        return false;
    }

    /**
     * Retrieves and sorts the top player balances.
     *
     * @return list of top balance entries, sorted by balance descending
     */
    private List<BalanceEntry> getTopBalances() {
        return balanceManager.getAllBalances()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0) // Only show players with positive balances
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(DEFAULT_TOP_LIMIT)
                .map(entry -> new BalanceEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Sends the complete baltop display to the player.
     *
     * @param player the player to send the display to
     * @param balances the sorted list of balance entries
     */
    private void sendBaltopDisplay(@NotNull Player player, @NotNull List<BalanceEntry> balances) {
        sendHeader(player, balances.size());
        sendBalanceEntries(player, balances);
    }

    /**
     * Sends the header message for the baltop display.
     *
     * @param player the player to send to
     * @param entryCount the number of entries being displayed
     */
    private void sendHeader(@NotNull Player player, int entryCount) {
        TextComponent header = Component.text("Top " + entryCount + " Richest Players:")
                .color(HEADER_COLOR)
                .decorate(TextDecoration.BOLD);

        player.sendMessage(header);
    }

    /**
     * Sends all balance entries to the player.
     *
     * @param player the player to send to
     * @param balances the list of balance entries
     */
    private void sendBalanceEntries(@NotNull Player player, @NotNull List<BalanceEntry> balances) {
        IntStream.range(0, balances.size())
                .forEach(i -> {
                    int rank = i + 1;
                    BalanceEntry entry = balances.get(i);
                    Component message = buildBalanceEntryComponent(rank, entry);
                    player.sendMessage(message);
                });
    }

    /**
     * Builds a formatted component for a single balance entry.
     *
     * @param rank the player's rank (1-based)
     * @param entry the balance entry
     * @return formatted component for the entry
     */
    private Component buildBalanceEntryComponent(int rank, @NotNull BalanceEntry entry) {
        String formattedBalance = currencyFormatter.formatCurrency(entry.balance());

        return Component.text()
                .append(Component.text("#" + rank + " ").color(RANK_COLOR))
                .append(Component.text(entry.playerName()).color(NAME_COLOR))
                .append(Component.text(" - ").color(SEPARATOR_COLOR))
                .append(Component.text(formattedBalance).color(BALANCE_COLOR))
                .build();
    }

    /**
     * Sends a message when no balance data is available.
     *
     * @param player the player to send the message to
     */
    private void sendNoDataMessage(@NotNull Player player) {
        player.sendMessage(Component.text("No player balance data available.")
                .color(NamedTextColor.YELLOW));
    }

    /**
     * Logs the command usage for administrative purposes.
     *
     * @param player the player who executed the command
     */
    private void logCommandUsage(@NotNull Player player) {
        logger.info(LOG_PREFIX + " " + player.getName() + " viewed balance rankings");
    }

    /**
     * Creates an error component with the specified text.
     *
     * @param text the error message text
     * @return the formatted error component
     */
    private Component errorComponent(@NotNull String text) {
        return Component.text(text).color(ERROR_COLOR);
    }

    /**
     * Resolves a player's display name from their UUID.
     *
     * @param uuid the player's UUID
     * @return the player's name or a default name if unavailable
     */
    private String resolvePlayerName(@NotNull UUID uuid) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName();
            return (name != null && !name.trim().isEmpty()) ? name : UNKNOWN_PLAYER_NAME;
        } catch (Exception e) {
            logger.warning(LOG_PREFIX + " Failed to resolve player name for UUID " + uuid + ": " + e.getMessage());
            return UNKNOWN_PLAYER_NAME;
        }
    }

    /**
     * Record representing a player's balance entry in the rankings.
     *
     * @param uuid the player's unique identifier
     * @param balance the player's balance amount
     */
    private record BalanceEntry(@NotNull UUID uuid, long balance) {

        /**
         * Gets the display name for this player.
         *
         * @return the player's name or "Unknown Player" if not available
         */
        public String playerName() {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String name = player.getName();
                return (name != null && !name.trim().isEmpty()) ? name : UNKNOWN_PLAYER_NAME;
            } catch (Exception e) {
                return UNKNOWN_PLAYER_NAME;
            }
        }
    }
}