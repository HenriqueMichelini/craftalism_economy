package io.github.HenriqueMichelini.craftalism_economy.core.commands;

import io.github.HenriqueMichelini.craftalism_economy.core.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.core.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.core.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.core.validators.PlayerValidator;
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

    @Deprecated
    public SetBalanceCommand(BalanceManager balanceManager, JavaPlugin plugin,
                             CurrencyFormatter currencyFormatter) {
        this(balanceManager, plugin, currencyFormatter, new PlayerValidator(), new CurrencyParser());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        try {
            if (!hasPermission(sender)) {
                sender.sendMessage(errorComponent("You don't have permission to use this command."));
                return true;
            }

            if (!validateArguments(sender, args)) {
                return true;
            }

            String playerName = args[0];
            String amountString = args[1];

            Optional<OfflinePlayer> targetOpt = resolveTargetPlayer(sender, playerName);
            if (targetOpt.isEmpty()) {
                return true;
            }

            OfflinePlayer target = targetOpt.get();

            Optional<Long> amountOpt = parseAmount(sender, amountString);
            if (amountOpt.isEmpty()) {
                return true;
            }

            long amount = amountOpt.get();

            return processBalanceUpdate(sender, target, amount);

        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing setbalance command: " + e.getMessage());
            sender.sendMessage(errorComponent("An error occurred while setting the balance."));
            return true;
        }
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("economy.admin.setbalance") || sender.isOp();
    }

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

    private Optional<OfflinePlayer> resolveTargetPlayer(CommandSender sender, String playerName) {
        if (sender instanceof Player player) {
            Optional<OfflinePlayer> playerOpt = playerValidator.resolvePlayer(player, playerName);
            if (playerOpt.isEmpty()) {
                sender.sendMessage(errorComponent("Player not found."));
            }
            return playerOpt;
        } else {
            sender.sendMessage(errorComponent("This command currently requires execution by a player."));
            return Optional.empty();
        }
    }

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

    private void sendConfirmationMessages(CommandSender sender, OfflinePlayer target, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String targetName = getPlayerName(target);

        Component senderMessage = buildAlternatingColorMessage(
                "Set ", targetName, "'s balance to ", formattedAmount
        );
        sender.sendMessage(senderMessage);

        if (target instanceof Player onlineTarget) {
            Component targetMessage = buildAlternatingColorMessage(
                    "Your balance has been set to ", formattedAmount, " by ", sender.getName()
            );
            onlineTarget.sendMessage(targetMessage);
        }
    }

    private Component buildAlternatingColorMessage(String... parts) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < parts.length; i++) {
            TextColor color = (i % 2 == 0) ? SUCCESS_COLOR : VALUE_COLOR;
            builder.append(Component.text(parts[i]).color(color));
        }

        return builder.build();
    }

    private String getPlayerName(OfflinePlayer player) {
        String name = player.getName();
        return (name != null && !name.isEmpty()) ? name : "Unknown Player";
    }

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

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(errorComponent("Usage: /setbalance <player> <amount>"));
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }
}