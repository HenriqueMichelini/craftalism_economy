package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.Validators;
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

import java.util.Optional;
import java.util.UUID;

public class PayCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;

    private final EconomyManager economyManager;
    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;
    private final Validators validators;

    public PayCommand(EconomyManager economyManager, BalanceManager balanceManager, JavaPlugin plugin, MoneyFormat moneyFormat, Validators validators) {
        this.economyManager = economyManager;
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
        this.validators = validators;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!validators.isNotPlayer(sender)) return false;
        Player payer = (Player) sender;

        if (!validators.validateArguments(payer, args, args.length, "asdasdasdas")) return false;

        Optional<UUID> payeeUuid = resolvePayeeUuid(args[0]);
        if (payeeUuid.isEmpty()) {
            payer.sendMessage(errorComponent("Player not found in system."));
            return false;
        }

        if (!checkIfPlayerIsNotPayingHimself(payer, payeeUuid.get())) return false;

        Optional<Long> amountOpt = moneyFormat.parseAmount(payer, args[1]);
        if (amountOpt.isEmpty()) return false;
        long amount = amountOpt.get();

        if (!processPayment(payer, payeeUuid.get(), amount)) return false;

        sendSuccessMessages(payer, payeeUuid.get(), amount);
        logTransaction(payer, payeeUuid.get(), amount);
        return true;
    }

    private Optional<UUID> resolvePayeeUuid(String name) {
        Optional<UUID> balanceMatch = balanceManager.getAllBalances().keySet().stream()
                .filter(uuid -> {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    return player.getName() != null && player.getName().equalsIgnoreCase(name);
                })
                .findFirst();

        if (balanceMatch.isPresent()) {
            return balanceMatch;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        if ((offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline())
                && balanceManager.getAllBalances().containsKey(offlinePlayer.getUniqueId())) {
            return Optional.of(offlinePlayer.getUniqueId());
        }

        return Optional.empty();
    }

    private boolean checkIfPlayerIsNotPayingHimself(Player payer, UUID payeeUuid) {
        if (payer.getUniqueId().equals(payeeUuid)) {
            payer.sendMessage(errorComponent("You cannot pay yourself."));
            return false;
        }
        return true;
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown Player";
    }

    private boolean processPayment(Player payer, UUID payeeUuid, long amount) {
        boolean transferBalanceOk = economyManager.transferBalance(
                payer.getUniqueId(),
                payeeUuid,
                amount
        );

        if (!transferBalanceOk) {
            payer.sendMessage(errorComponent("Transaction failed. You may not have sufficient funds."));
            return false;
        }

        return true;
    }

    private void sendSuccessMessages(Player payer, UUID payeeUuid, long amount) {
        String formattedAmount = moneyFormat.formatPrice(amount);
        String payeeName = getPlayerName(payeeUuid);

        payer.sendMessage(buildPaymentMessage("You paid ", payeeName, " ", formattedAmount));

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
            builder.append(Component.text(part).color(isValue ? VALUE_COLOR : SUCCESS_COLOR));
            isValue = !isValue;
        }

        return builder.build();
    }

    private void logTransaction(Player payer, UUID payeeUuid, long amount) {
        plugin.getLogger().info(String.format("%s paid %s %s",
                payer.getName(),
                getPlayerName(payeeUuid),
                moneyFormat.formatPrice(amount)));
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }
}