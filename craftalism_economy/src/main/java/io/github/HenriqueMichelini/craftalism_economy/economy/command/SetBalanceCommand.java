package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

public class SetBalanceCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public SetBalanceCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 2) {
            commandSender.sendMessage(Component.text("Usage: /setbalance <player> <amount>").color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            commandSender.sendMessage(Component.text("Player not found.").color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount < 0) {
                commandSender.sendMessage(Component.text("Balance cannot be negative.").color(TextColor.color(NamedTextColor.RED))); // Red
                return true;
            }

            // Format the amount with thousands separators and two decimal places
            String formattedAmount = formatAmount(amount);

            economyManager.setBalance(target.getUniqueId(), amount);

            // Message to the command sender
            commandSender.sendMessage(
                    Component.text("Set ")
                            .color(TextColor.color(NamedTextColor.GREEN)) // Green
                            .append(Component.text(Objects.requireNonNull(target.getName())).color(TextColor.color(NamedTextColor.WHITE))) // White
                            .append(Component.text("'s balance to $").color(TextColor.color(NamedTextColor.GREEN))) // Green
                            .append(Component.text(formattedAmount).color(TextColor.color(NamedTextColor.WHITE))) // White
                            .append(Component.text(".").color(TextColor.color(NamedTextColor.GREEN))) // Green
            );

            // Notify the player if online
            if (target.isOnline()) {
                Objects.requireNonNull(target.getPlayer()).sendMessage(
                        Component.text("Your balance has been set to $")
                                .color(TextColor.color(NamedTextColor.GREEN)) // Green
                                .append(Component.text(formattedAmount).color(TextColor.color(NamedTextColor.WHITE))) // White
                                .append(Component.text(".").color(TextColor.color(NamedTextColor.GREEN))) // Green
                );
            }
        } catch (NumberFormatException e) {
            commandSender.sendMessage(Component.text("Invalid amount.").color(TextColor.color(NamedTextColor.RED))); // Red
        }

        return true;
    }

    private String formatAmount(double amount) {
        // Create a DecimalFormat to format the amount with commas and two decimal places
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(amount);
    }
}
