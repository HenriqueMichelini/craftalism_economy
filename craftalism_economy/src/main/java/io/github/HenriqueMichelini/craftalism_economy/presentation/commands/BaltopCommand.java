//package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;
//
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyFormatter;
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.BaltopMessages;
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.validators.PlayerValidator;
//import net.kyori.adventure.text.Component;
//import net.kyori.adventure.text.format.NamedTextColor;
//import net.kyori.adventure.text.format.TextColor;
//
//import org.bukkit.Bukkit;
//import org.bukkit.OfflinePlayer;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//
//import org.bukkit.plugin.java.JavaPlugin;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.logging.Logger;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//public class BaltopCommand implements CommandExecutor {
//
//    private static final int ENTRIES_PER_PAGE = 10;
//    private static final String LOG_PREFIX = "[CE.Baltop]";
//    private static final String UNKNOWN_PLAYER_NAME = "Unknown Player";
//
//    private static final NamedTextColor HEADER_COLOR = NamedTextColor.GOLD;
//    private static final NamedTextColor INFO_COLOR = NamedTextColor.YELLOW;
//    private static final TextColor RANK_COLOR = TextColor.color(0xFFFF55); // Bright yellow
//    private static final TextColor NAME_COLOR = TextColor.color(0x55FF55); // Bright green
//    private static final TextColor SEPARATOR_COLOR = TextColor.color(0xFFFFFF); // White
//    private static final TextColor BALANCE_COLOR = TextColor.color(0x55FFFF); // Bright cyan
//    private static final TextColor PAGE_INFO_COLOR = TextColor.color(0xFFAA00); // Orange
//
//    private final BalanceManager balanceManager;
//    private final CurrencyFormatter currencyFormatter;
//    private final Logger logger;
//    private final PlayerValidator playerValidator;
//    private final CommandValidator commandValidator;
//
//    private BaltopMessages messages;
//
//    public BaltopCommand(@NotNull BalanceManager balanceManager,
//                         @NotNull JavaPlugin plugin,
//                         @NotNull CurrencyFormatter currencyFormatter) {
//        this.balanceManager = Objects.requireNonNull(balanceManager, "BalanceManager cannot be null");
//        Objects.requireNonNull(plugin, "Plugin cannot be null");
//        this.currencyFormatter = Objects.requireNonNull(currencyFormatter, "CurrencyFormatter cannot be null");
//        this.logger = plugin.getLogger();
//        this.playerValidator = new PlayerValidator();
//        this.commandValidator = new CommandValidator();
//    }
//
//    @Override
//    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
//                             @NotNull String label, String @NotNull [] args) {
//        Player player = (Player) sender;
//
//        try {
//            if (!playerValidator.isSenderAPlayer(sender)) {
//                return true;
//            }
//
//            if (args.length > 1) {
//                messages.sendBaltopUsage(player);
//                return true;
//            }
//
//            int page = 1;
//            if (args.length == 1) {
//                try {
//                    page = Integer.parseInt(args[0]);
//                    if (page < 1) {
//                        messages.sendBaltopInvalidPage(player);
//                        return true;
//                    }
//                } catch (NumberFormatException e) {
//                    messages.sendBaltopInvalidPage(player);
//                    return true;
//                }
//            }
//
//            List<BalanceEntry> allBalances = getTopBalances();
//            if (allBalances.isEmpty()) {
//                messages.sendBaltopNoData(player);
//                messages.sendBaltopUsage(player);
//                return true;
//            }
//
//            int totalPages = (int) Math.ceil((double) allBalances.size() / ENTRIES_PER_PAGE);
//            if (page > totalPages) {
//                messages.sendBaltopPageNotExist(player, String.valueOf(page), String.valueOf(totalPages));
//                return true;
//            }
//
//            int startIndex = (page - 1) * ENTRIES_PER_PAGE;
//            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, allBalances.size());
//            List<BalanceEntry> pageBalances = allBalances.subList(startIndex, endIndex);
//
//            sendBaltopDisplay(player, pageBalances, page, totalPages);
//            messages.sendBaltopUsage(player);
//
//            return true;
//
//        } catch (Exception e) {
//            logger.warning(LOG_PREFIX + " Error executing baltop command: " + e.getMessage());
//            messages.sendBaltopError(player);
//            return true;
//        }
//    }
//
//    private List<BalanceEntry> getTopBalances() {
//        return balanceManager.getAllBalances()
//                .entrySet()
//                .stream()
//                .filter(entry -> entry.getValue() > 0)
//                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
//                .map(entry -> new BalanceEntry(entry.getKey(), entry.getValue()))
//                .collect(Collectors.toList());
//    }
//
//    private void sendBaltopDisplay(@NotNull Player player, @NotNull List<BalanceEntry> balances,
//                                   int currentPage, int totalPages) {
//        messages.sendBaltopHeader(player, String.valueOf(balances.size()), String.valueOf(currentPage), String.valueOf(totalPages));
//        sendBalanceEntries(player, balances, (currentPage - 1) * ENTRIES_PER_PAGE);
//        messages.sendBaltopFooterNext(player, String.valueOf(totalPages));
//    }
//
//    private void sendBalanceEntries(@NotNull Player player, @NotNull List<BalanceEntry> balances, int startRank) {
//        IntStream.range(0, balances.size())
//                .forEach(i -> {
//                    int rank = startRank + i + 1;
//                    BalanceEntry entry = balances.get(i);
//                    Component message = buildBalanceEntryComponent(rank, entry);
//                    player.sendMessage(message);
//                });
//    }
//
//    private Component buildBalanceEntryComponent(int rank, @NotNull BalanceEntry entry) {
//        String formattedBalance;
//        try {
//            formattedBalance = currencyFormatter.formatCurrency(entry.balance());
//        } catch (Exception e) {
//            logger.warning(LOG_PREFIX + " Error formatting currency for balance " + entry.balance() + ": " + e.getMessage());
//            formattedBalance = String.valueOf(entry.balance());
//        }
//
//        return Component.text()
//                .append(Component.text("#" + rank + " ").color(RANK_COLOR))
//                .append(Component.text(entry.playerName()).color(NAME_COLOR))
//                .append(Component.text(" - ").color(SEPARATOR_COLOR))
//                .append(Component.text(formattedBalance).color(BALANCE_COLOR))
//                .build();
//    }
//
//    private record BalanceEntry(@NotNull UUID uuid, long balance) {
//        public String playerName() {
//            try {
//                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
//                String name = player.getName();
//                return (name != null && !name.trim().isEmpty()) ? name : UNKNOWN_PLAYER_NAME;
//            } catch (Exception e) {
//                return UNKNOWN_PLAYER_NAME;
//            }
//        }
//    }
//}