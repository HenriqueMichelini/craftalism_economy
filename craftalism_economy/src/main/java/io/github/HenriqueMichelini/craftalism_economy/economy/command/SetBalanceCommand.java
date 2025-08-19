package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.Validators;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SetBalanceCommand implements CommandExecutor {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor VALUE_COLOR = NamedTextColor.WHITE;

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;
    private final Validators validators;

    public SetBalanceCommand(BalanceManager balanceManager, JavaPlugin plugin, MoneyFormat moneyFormat, Validators validators) {
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
        this.validators = validators;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        Optional<Long> amount;
        Player player = (Player) sender;

        if (!validators.validateArguments(sender, args, args.length, "use /setbalance <player name> <value>")) return false;

        String playerName = args[0];
        String amountToSet = args[1];

        if (!validators.isNotPlayer(sender)) return false;

        Optional<OfflinePlayer> target = validators.resolvePlayer(player, playerName);

        if(target.isEmpty()) return false;

        amount = moneyFormat.parseAmount(player, amountToSet);

        if (amount.isEmpty()) {
            sender.sendMessage("Error: Amount missing!");
            return false;
        }

        return processBalanceUpdate(sender, target.get(), amount.get());
    }

    private boolean processBalanceUpdate(CommandSender sender, OfflinePlayer target, long amount) {
        try {
            balanceManager.setBalance(target.getUniqueId(), amount);
            sendConfirmationMessages(sender, target, amount);
            logTransaction(sender, target, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update balance: " + e.getMessage());
            sender.sendMessage(Component.text("Error updating balance. Check logs for details.")
                    .color(ERROR_COLOR));
            return false;
        }
    }

    private void sendConfirmationMessages(CommandSender sender, OfflinePlayer target, long amount) {
        String formattedAmount = moneyFormat.formatPrice(amount);
        String targetName = Optional.ofNullable(target.getName()).orElse("Unknown Player");

        sender.sendMessage(buildMessage("Set ", targetName, "'s balance to ", formattedAmount));

        Optional.ofNullable(target.getPlayer()).ifPresent(player ->
                player.sendMessage(buildMessage("Your balance has been set to ", formattedAmount, ""))
        );
    }

    private Component buildMessage(String... parts) {
        TextComponent.Builder builder = Component.text();
        boolean isValue = false;

        for (String part : parts) {
            TextColor color = isValue ? VALUE_COLOR : SUCCESS_COLOR;
            builder.append(Component.text(part).color(color));
            isValue = !isValue;
        }

        return builder.build();
    }

    private void logTransaction(CommandSender sender, OfflinePlayer target, long amount) {
        String logMessage = String.format("%s set %s's balance to %s",
                sender.getName(),
                Optional.ofNullable(target.getName()).orElse("UNKNOWN"),
                moneyFormat.formatPrice(amount));

        plugin.getLogger().info(logMessage);
    }
}