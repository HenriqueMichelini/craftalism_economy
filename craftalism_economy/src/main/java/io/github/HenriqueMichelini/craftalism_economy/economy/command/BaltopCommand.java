package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BaltopCommand implements CommandExecutor {
    private static final int TOP_LIMIT = 10;
    private static final NamedTextColor HEADER_COLOR = NamedTextColor.GOLD;
    private static final TextColor RANK_COLOR = TextColor.color(NamedTextColor.YELLOW);
    private static final TextColor NAME_COLOR = TextColor.color(NamedTextColor.GREEN);
    private static final TextColor SEPARATOR_COLOR = TextColor.color(NamedTextColor.WHITE);
    private static final TextColor BALANCE_COLOR = TextColor.color(NamedTextColor.AQUA);

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;

    public BaltopCommand(BalanceManager balanceManager, JavaPlugin plugin, MoneyFormat moneyFormat) {
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!validateSender(sender)) return true;
        Player player = (Player) sender;

        List<Map.Entry<UUID, Long>> topBalances = getSortedBalances();
        sendBaltopHeader(player);
        sendBaltopEntries(player, topBalances);
        logCommandUsage(player);

        return true;
    }

    private boolean validateSender(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(Component.text("Only players can use this command.")
                .color(NamedTextColor.RED));
        return false;
    }

    private List<Map.Entry<UUID, Long>> getSortedBalances() {
        return balanceManager.getAllBalances().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<UUID, Long>::getValue).reversed())
                .limit(TOP_LIMIT)
                .collect(Collectors.toList());
    }

    private void sendBaltopHeader(Player player) {
        player.sendMessage(
                Component.text("Top " + TOP_LIMIT + " Richest Players:")
                        .color(HEADER_COLOR)
                        .decorate(TextDecoration.BOLD)
        );
    }

    private void sendBaltopEntries(Player player, List<Map.Entry<UUID, Long>> entries) {
        int[] rank = {1};

        entries.forEach(entry -> {
            String playerName = Optional.ofNullable(Bukkit.getOfflinePlayer(entry.getKey()).getName())
                    .orElse("Unknown");

            player.sendMessage(buildBaltopEntry(
                    rank[0]++,
                    playerName,
                    moneyFormat.formatPrice(entry.getValue())
            ));
        });
    }

    private Component buildBaltopEntry(int rank, String name, String balance) {
        return Component.text()
                .append(Component.text("#" + rank + " ").color(RANK_COLOR))
                .append(Component.text(name).color(NAME_COLOR))
                .append(Component.text(" - ").color(SEPARATOR_COLOR))
                .append(Component.text(balance).color(BALANCE_COLOR))
                .build();
    }

    private void logCommandUsage(Player player) {
        plugin.getLogger().info("[Baltop] " + player.getName() + " viewed balance rankings");
    }
}