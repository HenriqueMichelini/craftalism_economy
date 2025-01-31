package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

public class PayCommand implements CommandExecutor {
    private final EconomyManager economyManager;

    public PayCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.text("Only players can use this command.").color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("Usage: /pay <player> <amount>").color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        Player payee = Bukkit.getPlayer(args[0]);
        if (payee == null || !payee.isOnline()) {
            player.sendMessage(Component.text("Player not found or offline.").color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        if (player.getUniqueId().equals(payee.getUniqueId())) {
            player.sendMessage(Component.text("You cannot pay yourself.").color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        // Parse the amount and validate decimal places
        BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]);

            // Block amounts with more than 2 decimal places
            if (amount.scale() > 2) {
                player.sendMessage(Component.text("You can only pay amounts with up to 2 decimal places.")
                        .color(TextColor.color(NamedTextColor.RED))); // Red
                return true;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                player.sendMessage(Component.text("The amount must be greater than zero.")
                        .color(TextColor.color(NamedTextColor.RED))); // Red
                return true;
            }

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount. Please enter a valid number.")
                    .color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        UUID payerUUID = player.getUniqueId();
        UUID payeeUUID = payee.getUniqueId();

        if (economyManager.getBalance(payerUUID).compareTo(amount) < 0) {
            player.sendMessage(Component.text("You don't have enough money.")
                    .color(TextColor.color(NamedTextColor.RED))); // Red
            return true;
        }

        // Format the amount with thousands separators and two decimal places
        String formattedAmount = formatAmount(amount);

        // Perform the transfer
        economyManager.transferBalance(payerUUID, payeeUUID, amount);

        // Message to the payer
        player.sendMessage(
                Component.text("You paid ")
                        .color(TextColor.color(NamedTextColor.GREEN)) // Green
                        .append(Component.text(payee.getName()).color(TextColor.color(NamedTextColor.WHITE))) // White
                        .append(Component.text(" $").color(TextColor.color(NamedTextColor.GREEN))) // Green
                        .append(Component.text(formattedAmount).color(TextColor.color(NamedTextColor.WHITE))) // White
        );

        // Message to the payee
        payee.sendMessage(
                Component.text("You received $")
                        .color(TextColor.color(NamedTextColor.GREEN)) // Green
                        .append(Component.text(formattedAmount).color(TextColor.color(NamedTextColor.WHITE))) // White
                        .append(Component.text(" from ").color(TextColor.color(NamedTextColor.GREEN))) // Green
                        .append(Component.text(player.getName()).color(TextColor.color(NamedTextColor.WHITE))) // White
        );

        return true;
    }

    private String formatAmount(BigDecimal amount) {
        // Create a DecimalFormat to format the amount with commas and two decimal places
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(amount.setScale(2, RoundingMode.HALF_UP));
    }
}