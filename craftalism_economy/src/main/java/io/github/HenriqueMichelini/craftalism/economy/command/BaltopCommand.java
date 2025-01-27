package io.github.HenriqueMichelini.craftalism.economy.command;

import io.github.HenriqueMichelini.craftalism.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class BaltopCommand implements CommandExecutor {
    private final EconomyManager economyManager;

    public BaltopCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);

        // Retrieve all balances and sort them in descending order
        Map<UUID, Double> balances = economyManager.getAllBalances();
        List<Map.Entry<UUID, Double>> topBalances = balances.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Sort by balance (descending)
                .limit(10) // Limit to top 10
                .toList();

        // Display the top balances
        player.sendMessage(
                Component.text("Top 10 Richest Players:")
                        .color(TextColor.color(NamedTextColor.GOLD)) // Gold
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
        );

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : topBalances) {
            UUID uuid = entry.getKey();
            double balance = entry.getValue();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName(); // Retrieve the player's name

            player.sendMessage(
                    Component.text("#" + rank + " ")
                            .color(TextColor.color(NamedTextColor.YELLOW)) // Yellow
                            .append(Component.text(playerName != null ? playerName : "Unknown").color(TextColor.color(NamedTextColor.GREEN))) // Green for name
                            .append(Component.text(" - ").color(NamedTextColor.WHITE))
                            .append(Component.text("$" + String.format("%.2f", balance)).color(TextColor.color(NamedTextColor.AQUA))) // Aqua for balance
            );
            rank++;
        }

        return true;
    }
}
