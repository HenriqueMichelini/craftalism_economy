package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class BalanceCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public BalanceCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        MoneyFormat moneyFormat = new MoneyFormat();
        String formattedBalance;

        if (args.length == 0) {
            // Show the player's own balance
            BigDecimal balance = economyManager.getBalance(player.getUniqueId());
            formattedBalance = moneyFormat.formatPrice(balance);
            player.sendMessage(
                    Component.text("Your balance is: ")
                            .color(NamedTextColor.GREEN) // Green for the prefix
                            .append(
                                    Component.text(formattedBalance)
                                            .color(TextColor.color(NamedTextColor.GREEN)) // Light green for the balance
                            )
            );
        } else if (args.length == 1) {
            // Show another player's balance
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(Component.text("Player not found or offline.").color(NamedTextColor.RED));
                return true;
            }

            BigDecimal targetBalance = economyManager.getBalance(target.getUniqueId());
            formattedBalance = moneyFormat.formatPrice(targetBalance);
            player.sendMessage(
                    Component.text(target.getName() + "'s balance is: ")
                            .color(NamedTextColor.GREEN) // Green for the prefix
                            .append(
                                    Component.text(formattedBalance)
                                            .color(TextColor.color(NamedTextColor.GREEN)) // Light green for the balance
                            )
            );
        } else {
            player.sendMessage(Component.text("Usage: /balance [player]").color(NamedTextColor.RED));
        }

        return true;
    }
}
