//package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;
//
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyFormatter;
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyParser;
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.SetBalanceMessages;
//import io.github.HenriqueMichelini.craftalism_economy.domain.service.validators.PlayerValidator;
//import net.kyori.adventure.text.Component;
//import net.kyori.adventure.text.format.NamedTextColor;
//import org.bukkit.OfflinePlayer;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.java.JavaPlugin;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Optional;
//
//public class SetBalanceCommand implements CommandExecutor {
//    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
//    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
//    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;
//    private static final String LOG_PREFIX = "[CE.SetBalance]";
//
//    private final BalanceManager balanceManager;
//    private final JavaPlugin plugin;
//    private final CurrencyFormatter currencyFormatter;
//    private final PlayerValidator playerValidator;
//    private final CurrencyParser currencyParser;
//
//    private final SetBalanceMessages messages;
//
//    public SetBalanceCommand(
//            @NotNull BalanceManager balanceManager,
//            @NotNull JavaPlugin plugin,
//            @NotNull CurrencyFormatter currencyFormatter,
//            @NotNull PlayerValidator playerValidator,
//            @NotNull CurrencyParser currencyParser,
//            @NotNull SetBalanceMessages messages
//    ) {
//
//        this.balanceManager = balanceManager;
//        this.plugin = plugin;
//        this.currencyFormatter = currencyFormatter;
//        this.playerValidator = playerValidator;
//        this.currencyParser = currencyParser;
//        this.messages = messages;
//    }
//
//    @Override
//    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
//        Player player = (Player) sender;
//        try {
//            if (!validateArguments(player, args)) {
//                return true;
//            }
//
//            String playerName = args[0];
//            String amountString = args[1];
//
//            Optional<OfflinePlayer> targetOpt = resolveTargetPlayer(sender, playerName);
//            if (targetOpt.isEmpty()) {
//                return true;
//            }
//
//            OfflinePlayer target = targetOpt.get();
//
//            Optional<Long> amountOpt = parseAmount(sender, amountString);
//            if (amountOpt.isEmpty()) {
//                return true;
//            }
//
//            long amount = amountOpt.get();
//
//            return processBalanceUpdate(sender, target, amount);
//
//        } catch (Exception e) {
//            plugin.getLogger().warning(LOG_PREFIX + " Error executing setbalance command: " + e.getMessage());
//            messages.sendSetBalanceException(player);
//            return true;
//        }
//    }
//
//    private boolean validateArguments(Player sender, String[] args) {
//        if (args.length != 2) {
//            messages.sendSetBalanceUsage(sender);
//            return false;
//        }
//
//        if (args[0] == null || args[0].trim().isEmpty()) {
//            messages.sendSetBalancePlayerEmpty(sender);
//            messages.sendSetBalanceUsage(sender);
//            return false;
//        }
//
//        if (args[1] == null || args[1].trim().isEmpty()) {
//            messages.sendSetBalanceAmountEmpty(sender);
//            messages.sendSetBalanceUsage(sender);
//            return false;
//        }
//
//        return true;
//    }
//
//    //essa funçao faz coisas demais
//    private Optional<OfflinePlayer> resolveTargetPlayer(CommandSender sender, String playerName) {
//        if (sender instanceof Player player) {
//            Optional<OfflinePlayer> playerOpt = playerValidator.resolvePlayer(player, playerName);
//            if (playerOpt.isEmpty()) {
//                messages.sendSetBalancePlayerNotFound(player);
//            }
//            return playerOpt;
//        } else {
//            sender.sendMessage(errorComponent("This command currently requires execution by a player."));
//            messages.send
//            return Optional.empty();
//        }
//    }
//
//    private Optional<Long> parseAmount(CommandSender sender, String amountString) {
//        if (sender instanceof Player player) {
//            return currencyParser.parseAmount(player, amountString);
//        } else {
//            Optional<Long> amountOpt = currencyParser.parseAmountSilently(amountString);
//            if (amountOpt.isEmpty()) {
//                sender.sendMessage(errorComponent("Invalid amount format. Use numbers only (e.g., 1.23)."));
//            }
//            return amountOpt;
//        }
//    }
//
//    private boolean processBalanceUpdate(CommandSender sender, OfflinePlayer target, long amount) {
//        try {
//            balanceManager.setBalance(target.getUniqueId(), amount);
//            sendConfirmationMessages(sender, target, amount);
//            logTransaction(sender, target, amount);
//            return true;
//        } catch (Exception e) {
//            plugin.getLogger().severe(LOG_PREFIX + " Failed to update balance for " +
//                    target.getName() + ": " + e.getMessage());
//            sender.sendMessage(errorComponent("Error updating balance. Check logs for details."));
//            return false;
//        }
//    }
//
//    private void sendConfirmationMessages(CommandSender sender, OfflinePlayer target, long amount) {
//        String formattedAmount = currencyFormatter.formatCurrency(amount);
//        String targetName = getPlayerName(target);
//
//        Component senderMessage = buildAlternatingColorMessage(
//                "Set ", targetName, "'s balance to ", formattedAmount
//        );
//        sender.sendMessage(senderMessage);
//
//        if (target instanceof Player onlineTarget) {
//            Component targetMessage = buildAlternatingColorMessage(
//                    "Your balance has been set to ", formattedAmount, " by ", sender.getName()
//            );
//            onlineTarget.sendMessage(targetMessage);
//        }
//    }
//
//    //novamente, isso deveria estar em outro lugar, to pensando ainda
//    private String getPlayerName(OfflinePlayer player) {
//        String name = player.getName();
//        return (name != null && !name.isEmpty()) ? name : "Unknown Player";
//    }
//}