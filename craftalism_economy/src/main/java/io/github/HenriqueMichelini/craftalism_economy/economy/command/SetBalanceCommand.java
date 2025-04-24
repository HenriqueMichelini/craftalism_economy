package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class SetBalanceCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;

    private final EconomyManager economyManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;

    public SetBalanceCommand(EconomyManager economyManager, JavaPlugin plugin, MoneyFormat moneyFormat) {
        this.economyManager = economyManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!validateArguments(sender, args)) return true;

        OfflinePlayer target = resolvePlayer(sender, args[0]);
        if (target == null || !validateTarget(target)) return true;

        Optional<BigDecimal> amount = parseAmount(sender, args[1]);
        return amount.map(bigDecimal -> processBalanceUpdate(sender, target, bigDecimal)).orElse(true);

    }

    private boolean validateArguments(CommandSender sender, String[] args) {
        if (args.length == 2) return true;

        sender.sendMessage(Component.text("Usage: /setbalance <player> <amount>")
                .color(ERROR_COLOR));
        return false;
    }

    private OfflinePlayer resolvePlayer(CommandSender sender, String username) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        if (player.hasPlayedBefore() || player.isOnline()) return player;

        sender.sendMessage(Component.text("Player not found.").color(ERROR_COLOR));
        return null;
    }

    private boolean validateTarget(OfflinePlayer target) {
        target.getUniqueId();
        return true;

    }

    private Optional<BigDecimal> parseAmount(CommandSender sender, String input) {
        try {
            BigDecimal amount = new BigDecimal(input).setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage(Component.text("Balance cannot be negative.").color(ERROR_COLOR));
                return Optional.empty();
            }

            return Optional.of(amount);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount format. Use numbers only.")
                    .color(ERROR_COLOR));
            return Optional.empty();
        }
    }

    private boolean processBalanceUpdate(CommandSender sender, OfflinePlayer target, BigDecimal amount) {
        try {
            economyManager.setBalance(target.getUniqueId(), amount);
            sendConfirmationMessages(sender, target, amount);
            logTransaction(sender, target, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update balance: " + e.getMessage());
            sender.sendMessage(Component.text("Error updating balance. Check logs for details.")
                    .color(ERROR_COLOR));
            return false;
        }
    }

    private void sendConfirmationMessages(CommandSender sender, OfflinePlayer target, BigDecimal amount) {
        String formattedAmount = moneyFormat.formatPrice(amount);
        String targetName = Optional.ofNullable(target.getName()).orElse("Unknown Player");

        sender.sendMessage(buildMessage("Set ", targetName, "'s balance to ", formattedAmount));

        Optional.ofNullable(target.getPlayer()).ifPresent(player ->
                player.sendMessage(buildMessage("Your balance has been set to ", formattedAmount, ""))
        );
    }

    private Component buildMessage(String... parts) {
        TextComponent.Builder builder = Component.text();
        boolean isValue = false;

        for (String part : parts) {
            TextColor color = isValue ? VALUE_COLOR : SUCCESS_COLOR;
            builder.append(Component.text(part).color(color));
            isValue = !isValue;
        }

        return builder.build();
    }

    private void logTransaction(CommandSender sender, OfflinePlayer target, BigDecimal amount) {
        String logMessage = String.format("%s set %s's balance to %s",
                sender.getName(),
                Optional.ofNullable(target.getName()).orElse("UNKNOWN"),
                moneyFormat.formatPrice(amount));

        plugin.getLogger().info(logMessage);
    }
}