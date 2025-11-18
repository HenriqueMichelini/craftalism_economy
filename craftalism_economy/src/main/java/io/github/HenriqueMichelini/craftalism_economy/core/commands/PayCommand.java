package io.github.HenriqueMichelini.craftalism_economy.core.commands;

import io.github.HenriqueMichelini.craftalism_economy.core.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.core.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.core.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.core.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.core.validators.PlayerValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class PayCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;
    private static final String LOG_PREFIX = "[CE.Pay]";

    private final EconomyManager economyManager;
    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;
    private final CurrencyParser currencyParser;

    public PayCommand(EconomyManager economyManager, BalanceManager balanceManager,
                      JavaPlugin plugin, CurrencyFormatter currencyFormatter,
                      PlayerValidator playerValidator, CurrencyParser currencyParser) {
        if (economyManager == null) {
            throw new IllegalArgumentException("EconomyManager cannot be null");
        }
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

        this.economyManager = economyManager;
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.currencyFormatter = currencyFormatter;
        this.playerValidator = playerValidator;
        this.currencyParser = currencyParser;
    }

    @Deprecated
    public PayCommand(EconomyManager economyManager, BalanceManager balanceManager,
                      JavaPlugin plugin, CurrencyFormatter currencyFormatter, CurrencyParser currencyParser) {
        this(economyManager, balanceManager, plugin, currencyFormatter, new PlayerValidator(), currencyParser);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!playerValidator.isSenderAPlayer(sender)) {
            sender.sendMessage(errorComponent("This command can only be used by players."));
            return true;
        }

        Player payer = (Player) sender;

        try {
            if (!validateArguments(payer, args)) {
                return true;
            }

            Optional<OfflinePlayer> payeeOpt = resolvePayee(payer, args[0]);
            if (payeeOpt.isEmpty()) {
                return true;
            }

            OfflinePlayer payee = payeeOpt.get();
            UUID payeeUuid = payee.getUniqueId();

            if (!validateNotSelfPayment(payer, payeeUuid)) {
                return true;
            }

            Optional<Long> amountOpt = currencyParser.parseAmount(payer, args[1]);
            if (amountOpt.isEmpty()) {
                return true;
            }

            long amount = amountOpt.get();

            if (!processPayment(payer, payeeUuid, amount)) {
                return true;
            }

            sendSuccessMessages(payer, payee, amount);
            logTransaction(payer, payee, amount);

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing pay command: " + e.getMessage());
            payer.sendMessage(errorComponent("An error occurred while processing the payment."));
            return true;
        }
    }

    private boolean validateArguments(Player payer, String[] args) {
        if (args.length != 2) {
            sendUsageMessage(payer);
            return false;
        }

        if (args[0] == null || args[0].trim().isEmpty()) {
            payer.sendMessage(errorComponent("Player name cannot be empty."));
            sendUsageMessage(payer);
            return false;
        }

        if (args[1] == null || args[1].trim().isEmpty()) {
            payer.sendMessage(errorComponent("Amount cannot be empty."));
            sendUsageMessage(payer);
            return false;
        }

        return true;
    }

    private Optional<OfflinePlayer> resolvePayee(Player payer, String payeeName) {
        Optional<OfflinePlayer> playerOpt = playerValidator.resolvePlayer(payer, payeeName);

        if (playerOpt.isEmpty()) {
            payer.sendMessage(errorComponent("Player not found."));
            return Optional.empty();
        }

        OfflinePlayer player = playerOpt.get();
        UUID playerUuid = player.getUniqueId();

        if (!balanceManager.checkIfBalanceExists(playerUuid)) {
            payer.sendMessage(errorComponent("Player " + payeeName + " doesn't have an account in the economy system."));
            return Optional.empty();
        }

        return playerOpt;
    }

    private boolean validateNotSelfPayment(Player payer, UUID payeeUuid) {
        if (payer.getUniqueId().equals(payeeUuid)) {
            payer.sendMessage(errorComponent("You cannot pay yourself."));
            return false;
        }
        return true;
    }

    private boolean processPayment(Player payer, UUID payeeUuid, long amount) {
        boolean success = economyManager.transferBalance(payer.getUniqueId(), payeeUuid, amount);

        if (!success) {
            payer.sendMessage(errorComponent("Transaction failed. You may not have sufficient funds."));
            return false;
        }

        return true;
    }

    private void sendSuccessMessages(Player payer, OfflinePlayer payee, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String payeeName = getPlayerName(payee);

        Component payerMessage = buildPaymentMessage(
                "You paid ", formattedAmount, " to ", payeeName
        );
        payer.sendMessage(payerMessage);

        if (payee instanceof Player onlinePayee) {
            Component payeeMessage = buildReceivedMessage(
                    "You received ", formattedAmount, " from ", payer.getName()
            );
            onlinePayee.sendMessage(payeeMessage);
        }
    }

    private Component buildPaymentMessage(String... parts) {
        return buildAlternatingColorMessage(parts);
    }

    private Component buildReceivedMessage(String... parts) {
        return buildAlternatingColorMessage(parts);
    }

    private Component buildAlternatingColorMessage(String... parts) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < parts.length; i++) {
            NamedTextColor color = (i % 2 == 0) ? SUCCESS_COLOR : VALUE_COLOR;
            builder.append(Component.text(parts[i]).color(color));
        }

        return builder.build();
    }

    private String getPlayerName(OfflinePlayer player) {
        String name = player.getName();
        return (name != null && !name.isEmpty()) ? name : "Unknown Player";
    }

    private void logTransaction(Player payer, OfflinePlayer payee, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String payeeName = getPlayerName(payee);

        plugin.getLogger().info(String.format("%s %s paid %s %s",
                LOG_PREFIX,
                payer.getName(),
                payeeName,
                formattedAmount
        ));
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage(errorComponent("Usage: /pay <player> <amount>"));
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }
}