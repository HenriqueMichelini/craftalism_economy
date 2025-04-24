package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Optional;

public class BalanceCommand implements CommandExecutor {
    private static final TextColor PREFIX_COLOR = NamedTextColor.GREEN;
    private static final TextColor BALANCE_COLOR = NamedTextColor.WHITE;
    private static final TextColor ERROR_COLOR = NamedTextColor.RED;

    private final EconomyManager economyManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;

    public BalanceCommand(EconomyManager economyManager, JavaPlugin plugin, MoneyFormat moneyFormat) {
        this.economyManager = economyManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!validateSender(sender)) return true;
        Player player = (Player) sender;

        return args.length == 0 ? showOwnBalance(player) :
                args.length == 1 ? showOtherBalance(player, args[0]) :
                        showUsage(player);
    }

    private boolean validateSender(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(errorComponent("Only players can use this command."));
        return false;
    }

    private boolean showOwnBalance(Player player) {
        BigDecimal balance = economyManager.getBalance(player.getUniqueId());
        sendBalanceMessage(player, "Your balance is: ", balance);
        logQuery(player.getName(), "self");
        return true;
    }

    private boolean showOtherBalance(Player requester, String targetName) {
        return resolvePlayer(targetName)
                .map(target -> {
                    BigDecimal balance = economyManager.getBalance(target.getUniqueId());
                    sendBalanceMessage(requester, target.getName() + "'s balance is: ", balance);
                    logQuery(requester.getName(), target.getName());
                    return true;
                })
                .orElseGet(() -> {
                    requester.sendMessage(errorComponent("Player not found."));
                    return false;
                });
    }

    private Optional<OfflinePlayer> resolvePlayer(String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        return (player.hasPlayedBefore() || player.isOnline()) ?
                Optional.of(player) :
                Optional.empty();
    }

    private boolean showUsage(Player player) {
        player.sendMessage(errorComponent("Usage: /balance [player]"));
        return true;
    }

    private void sendBalanceMessage(Player recipient, String prefix, BigDecimal balance) {
        String formatted = moneyFormat.formatPrice(balance);
        recipient.sendMessage(
                Component.text(prefix)
                        .color(PREFIX_COLOR)
                        .append(Component.text(formatted).color(BALANCE_COLOR))
        );
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }

    private void logQuery(String requester, String target) {
        plugin.getLogger().info("[Balance] " + requester + " checked balance of " + target);
    }
}