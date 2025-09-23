package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.CommandValidator;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.PlayerValidator;
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
    private static final int ENTRIES_PER_PAGE = 10;
    private static final String LOG_PREFIX = "[CE.Baltop]";
    private static final String UNKNOWN_PLAYER_NAME = "Unknown Player";

    // Color scheme constants
    private static final NamedTextColor HEADER_COLOR = NamedTextColor.GOLD;
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor INFO_COLOR = NamedTextColor.YELLOW;
    private static final TextColor RANK_COLOR = TextColor.color(0xFFFF55); // Bright yellow
    private static final TextColor NAME_COLOR = TextColor.color(0x55FF55); // Bright green
    private static final TextColor SEPARATOR_COLOR = TextColor.color(0xFFFFFF); // White
    private static final TextColor BALANCE_COLOR = TextColor.color(0x55FFFF); // Bright cyan
    private static final TextColor PAGE_INFO_COLOR = TextColor.color(0xFFAA00); // Orange

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final Logger logger;
    private final PlayerValidator playerValidator;
    private final CommandValidator commandValidator;

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
        this.playerValidator = new PlayerValidator();
        this.commandValidator = new CommandValidator();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        try {
            // Validate sender is a player
            if (!playerValidator.isSenderAPlayer(sender)) {
                return true;
            }

            Player player = (Player) sender;

            // Validate arguments
            if (args.length > 1) {
                commandValidator.validateArguments(player, args, 1, "Usage: /baltop [page]");
                return true;
            }

            int page = 1;
            if (args.length == 1) {
                try {
                    page = Integer.parseInt(args[0]);
                    if (page < 1) {
                        player.sendMessage(Component.text("Page number must be at least 1.").color(ERROR_COLOR));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid page number. Please enter a valid number.").color(ERROR_COLOR));
                    return true;
                }
            }

            // Get and validate balance data
            List<BalanceEntry> allBalances = getTopBalances();
            if (allBalances.isEmpty()) {
                sendNoDataMessage(player);
                logCommandUsage(player, page);
                return true;
            }

            // Calculate pagination
            int totalPages = (int) Math.ceil((double) allBalances.size() / ENTRIES_PER_PAGE);
            if (page > totalPages) {
                player.sendMessage(Component.text("Page " + page + " does not exist. There are only " + totalPages + " pages.").color(ERROR_COLOR));
                return true;
            }

            // Get entries for current page
            int startIndex = (page - 1) * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, allBalances.size());
            List<BalanceEntry> pageBalances = allBalances.subList(startIndex, endIndex);

            // Send formatted baltop display
            sendBaltopDisplay(player, pageBalances, page, totalPages);
            logCommandUsage(player, page);

            return true;

        } catch (Exception e) {
            logger.warning(LOG_PREFIX + " Error executing baltop command: " + e.getMessage());
            sender.sendMessage(errorComponent());
            return true;
        }
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
                .map(entry -> new BalanceEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Sends the complete baltop display to the player.
     *
     * @param player the player to send the display to
     * @param balances the sorted list of balance entries for the current page
     * @param currentPage the current page number
     * @param totalPages the total number of pages
     */
    private void sendBaltopDisplay(@NotNull Player player, @NotNull List<BalanceEntry> balances,
                                   int currentPage, int totalPages) {
        sendHeader(player, balances.size(), currentPage, totalPages);
        sendBalanceEntries(player, balances, (currentPage - 1) * ENTRIES_PER_PAGE);
        sendFooter(player, currentPage, totalPages);
    }

    /**
     * Sends the header message for the baltop display.
     *
     * @param player the player to send to
     * @param entryCount the number of entries being displayed on this page
     * @param currentPage the current page number
     * @param totalPages the total number of pages
     */
    private void sendHeader(@NotNull Player player, int entryCount, int currentPage, int totalPages) {
        TextComponent header = Component.text()
                .append(Component.text("Top " + entryCount + " Richest Players")
                        .color(HEADER_COLOR)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(" (Page " + currentPage + "/" + totalPages + ")")
                        .color(PAGE_INFO_COLOR))
                .append(Component.text(":"))
                .build();

        player.sendMessage(header);
    }

    /**
     * Sends all balance entries to the player.
     *
     * @param player the player to send to
     * @param balances the list of balance entries for the current page
     * @param startRank the starting rank for this page (1-based)
     */
    private void sendBalanceEntries(@NotNull Player player, @NotNull List<BalanceEntry> balances, int startRank) {
        IntStream.range(0, balances.size())
                .forEach(i -> {
                    int rank = startRank + i + 1; // Calculate global rank
                    BalanceEntry entry = balances.get(i);
                    Component message = buildBalanceEntryComponent(rank, entry);
                    player.sendMessage(message);
                });
    }

    /**
     * Sends the footer with pagination info.
     *
     * @param player the player to send to
     * @param currentPage the current page number
     * @param totalPages the total number of pages
     */
    private void sendFooter(@NotNull Player player, int currentPage, int totalPages) {
        if (totalPages > 1) {
            TextComponent footer = Component.text()
                    .append(Component.text("Use "))
                    .append(Component.text("/baltop <page>").color(INFO_COLOR))
                    .append(Component.text(" to navigate between pages."))
                    .build();

            player.sendMessage(footer);

            if (currentPage < totalPages) {
                TextComponent nextPage = Component.text()
                        .append(Component.text("Next page: "))
                        .append(Component.text("/baltop " + (currentPage + 1)).color(INFO_COLOR))
                        .build();

                player.sendMessage(nextPage);
            }
        }
    }

    /**
     * Builds a formatted component for a single balance entry.
     *
     * @param rank the player's rank (1-based)
     * @param entry the balance entry
     * @return formatted component for the entry
     */
    private Component buildBalanceEntryComponent(int rank, @NotNull BalanceEntry entry) {
        String formattedBalance;
        try {
            formattedBalance = currencyFormatter.formatCurrency(entry.balance());
        } catch (Exception e) {
            logger.warning(LOG_PREFIX + " Error formatting currency for balance " + entry.balance() + ": " + e.getMessage());
            formattedBalance = String.valueOf(entry.balance());
        }

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
                .color(INFO_COLOR));
    }

    /**
     * Logs the command usage for administrative purposes.
     *
     * @param player the player who executed the command
     * @param page the page number viewed
     */
    private void logCommandUsage(@NotNull Player player, int page) {
        String pageInfo = page > 1 ? " (page " + page + ")" : "";
        logger.info(LOG_PREFIX + " " + player.getName() + " viewed balance rankings" + pageInfo);
    }

    /**
     * Creates an error component with the specified text.
     *
     * @return the formatted error component
     */
    private Component errorComponent() {
        return Component.text("An error occurred while retrieving balance rankings.").color(ERROR_COLOR);
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