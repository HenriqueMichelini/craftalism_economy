package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

        Optional<UUID> payeeUuid = resolvePayeeUuid(args[0]);
        if (payeeUuid.isEmpty()) {
            payer.sendMessage(errorComponent("Player not found in system."));
            return true;
        }

        if (!validatePayment(payer.getUniqueId(), payeeUuid.get())) return true;

        Optional<BigDecimal> amountOpt = parseAmount(payer, args[1]);
        if (amountOpt.isEmpty()) return true;
        BigDecimal amount = amountOpt.get();

        if (!processPayment(payer.getUniqueId(), payeeUuid.get(), amount)) return true;

        sendSuccessMessages(payer, payeeUuid.get(), amount);
        logTransaction(payer, payeeUuid.get(), amount);
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

    private Optional<UUID> resolvePayeeUuid(String name) {
        // 1. Check exact name match in existing balances
        Optional<UUID> balanceMatch = economyManager.getAllBalances().keySet().stream()
                .filter(bigDecimal -> {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(bigDecimal);
                    return player.getName() != null && player.getName().equalsIgnoreCase(name);
                })
                .findFirst();

        if (balanceMatch.isPresent()) {
            return balanceMatch;
        }

        // 2. Check Bukkit's offline players
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            return Optional.of(offlinePlayer.getUniqueId());
        }

        // 3. Final check in economy system
        if (economyManager.getAllBalances().containsKey(offlinePlayer.getUniqueId())) {
            return Optional.of(offlinePlayer.getUniqueId());
        }

        return Optional.empty();
    }

    private boolean validatePayment(UUID payerUuid, UUID payeeUuid) {
        if (payerUuid.equals(payeeUuid)) {
            Objects.requireNonNull(Bukkit.getPlayer(payerUuid)).sendMessage(errorComponent("You cannot pay yourself."));
            return false;
        }
        return true;
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

    private String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown Player";
    }

    private boolean processPayment(UUID payerUuid, UUID payeeUuid, BigDecimal amount) {
        Player payer = Bukkit.getPlayer(payerUuid);

        if (economyManager.hasInsufficientFunds(payerUuid, amount)) {
            if (payer != null) {
                payer.sendMessage(errorComponent("Insufficient funds."));
            }
            return false;
        }

        if (!economyManager.transferBalance(payerUuid, payeeUuid, amount)) {
            if (payer != null) {
                payer.sendMessage(errorComponent("Transaction failed. Please try again."));
            }
            return false;
        }
        return true;
    }

    private void sendSuccessMessages(Player payer, UUID payeeUuid, BigDecimal amount) {
        String formattedAmount = moneyFormat.formatPrice(amount);
        String payeeName = getPlayerName(payeeUuid);

        // Payer message
        payer.sendMessage(buildPaymentMessage("You paid ", payeeName, " ", formattedAmount));

        // Notify payee if online
        Player onlinePayee = Bukkit.getPlayer(payeeUuid);
        if (onlinePayee != null) {
            onlinePayee.sendMessage(buildReceivedMessage(
                    "You received ", formattedAmount, " from ", payer.getName()
            ));
        }
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

    private void logTransaction(Player payer, UUID payeeUuid, BigDecimal amount) {
        String logMessage = String.format("%s paid %s %s",
                payer.getName(),
                getPlayerName(payeeUuid),
                moneyFormat.formatPrice(amount));

        plugin.getLogger().info(logMessage);
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }
}