package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class PayCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;
    private static final int MAX_DECIMAL_SCALE = 2;

    private final EconomyManager economyManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;

    public PayCommand(EconomyManager economyManager, JavaPlugin plugin, MoneyFormat moneyFormat) {
        this.economyManager = economyManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!validateSender(sender)) return true;
        Player payer = (Player) sender;

        if (!validateArguments(payer, args)) return true;

        Optional<Player> payeeOpt = resolvePayee(payer, args[0]);
        if (payeeOpt.isEmpty()) return true;
        Player payee = payeeOpt.get();

        if (!validatePayment(payer, payee)) return true;

        Optional<BigDecimal> amountOpt = parseAmount(payer, args[1]);
        if (amountOpt.isEmpty()) return true;
        BigDecimal amount = amountOpt.get();

        if (!processPayment(payer, payee, amount)) return true;

        sendSuccessMessages(payer, payee, amount);
        logTransaction(payer, payee, amount);
        return true;
    }

    private boolean validateSender(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(errorComponent("Only players can use this command."));
        return false;
    }

    private boolean validateArguments(Player payer, String[] args) {
        if (args.length == 2) return true;
        payer.sendMessage(errorComponent("Usage: /pay <player> <amount>"));
        return false;
    }

    private Optional<Player> resolvePayee(Player payer, String username) {
        Player payee = Bukkit.getPlayer(username);
        if (payee != null && payee.isOnline()) return Optional.of(payee);

        payer.sendMessage(errorComponent("Player not found or offline."));
        return Optional.empty();
    }

    private boolean validatePayment(Player payer, Player payee) {
        if (!payer.getUniqueId().equals(payee.getUniqueId())) return true;

        payer.sendMessage(errorComponent("You cannot pay yourself."));
        return false;
    }

    private Optional<BigDecimal> parseAmount(Player payer, String input) {
        try {
            BigDecimal amount = new BigDecimal(input).setScale(MAX_DECIMAL_SCALE, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                payer.sendMessage(errorComponent("Amount must be greater than zero."));
                return Optional.empty();
            }

            if (amount.scale() > MAX_DECIMAL_SCALE) {
                payer.sendMessage(errorComponent("Maximum of 2 decimal places allowed."));
                return Optional.empty();
            }

            return Optional.of(amount);
        } catch (NumberFormatException e) {
            payer.sendMessage(errorComponent("Invalid number format."));
            return Optional.empty();
        }
    }

    private boolean processPayment(Player payer, Player payee, BigDecimal amount) {
        if (economyManager.hasInsufficientFunds(payer.getUniqueId(), amount)) {
            payer.sendMessage(errorComponent("Insufficient funds."));
            return false;
        }

        if (!economyManager.transferBalance(payer.getUniqueId(), payee.getUniqueId(), amount)) {
            payer.sendMessage(errorComponent("Transaction failed. Please try again."));
            return false;
        }
        return true;
    }

    private void sendSuccessMessages(Player payer, Player payee, BigDecimal amount) {
        String formattedAmount = moneyFormat.formatPrice(amount);

        // Payer message
        payer.sendMessage(buildPaymentMessage("You paid ", payee.getName(), formattedAmount));

        // Payee message
        payee.sendMessage(buildReceivedMessage(
                "You received ", formattedAmount, " from ", payer.getName()
        ));
    }

    private Component buildPaymentMessage(String... parts) {
        return buildMessage(parts);
    }

    private Component buildReceivedMessage(String... parts) {
        return buildMessage(parts);
    }

    private Component buildMessage(String... parts) {
        TextComponent.Builder builder = Component.text();
        boolean isValue = false;

        for (String part : parts) {
            builder.append(Component.text(part).color(isValue ? PayCommand.VALUE_COLOR : PayCommand.SUCCESS_COLOR));
            isValue = !isValue;
        }

        return builder.build();
    }

    private void logTransaction(Player payer, Player payee, BigDecimal amount) {
        String logMessage = String.format("%s paid %s %s",
                payer.getName(),
                payee.getName(),
                moneyFormat.formatPrice(amount));

        plugin.getLogger().info(logMessage);
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }
}