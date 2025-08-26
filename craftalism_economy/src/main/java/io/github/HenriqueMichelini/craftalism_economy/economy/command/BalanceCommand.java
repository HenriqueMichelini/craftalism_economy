package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.Validators;
import net.kyori.adventure.text.Component;
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
import java.util.UUID;

public class BalanceCommand implements CommandExecutor {
    private static final TextColor PREFIX_COLOR = NamedTextColor.GREEN;
    private static final TextColor BALANCE_COLOR = NamedTextColor.WHITE;
    private static final TextColor ERROR_COLOR = NamedTextColor.RED;

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;
    private final Validators validators;

    private long balance;

    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin, MoneyFormat moneyFormat, Validators validators) {
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
        this.validators = validators;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!validators.isPlayer(sender)) {
            return false;
        }

        Player player = (Player) sender;

        if (args.length == 0 && showOwnBalance(player)) {
            logShowOwnBalance(player.getName());
            return true;
        }

        String targetName = args[0];
        if (targetName == null || targetName.isEmpty()) {
            sendPlayerNotFoundMessage(player);
            sendUsageMessage(player);
            return false;
        }

        if(args.length == 1) {
            showOtherBalance(player, targetName);
            logShowOtherBalance(player.getName(), targetName);
            return true;
        }
        
        return false;
    }

    private boolean showOwnBalance(Player player) {
        if(balanceManager.checkIfBalanceExists(player.getUniqueId())) {
            this.balance = balanceManager.getBalance(player.getUniqueId());
        }
        sendOwnBalanceMessage(player, this.balance);
        return true;
    }

    private void sendOwnBalanceMessage(Player player, long balance) {
        String formatted = moneyFormat.formatPrice(balance);
        player.sendMessage(
                Component.text("Your balance is: ")
                        .color(PREFIX_COLOR)
                        .append(Component.text(formatted).color(BALANCE_COLOR))
        );
    }

    private void showOtherBalance(Player requester, String targetName) {
        Optional<OfflinePlayer> player = validators.resolvePlayer(requester, targetName);

        if (player.isEmpty()) {
            sendPlayerNotFoundMessage(requester);
            return;
        }

        UUID uuid = player.get().getUniqueId();

        this.balance = balanceManager.getBalance(uuid);

        logShowOtherBalance(requester.getName(), targetName);
        sendOtherBalanceMessage(requester, targetName, balance);
    }

    private void sendOtherBalanceMessage(Player recipient, String targetName, long balance) {
        String formatted = moneyFormat.formatPrice(balance);
        recipient.sendMessage(
                Component.text(targetName + "'s balance is: ")
                        .color(PREFIX_COLOR)
                        .append(Component.text(formatted).color(BALANCE_COLOR))
        );
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage(errorComponent("Usage: /balance [player]"));
    }

    private void sendPlayerNotFoundMessage(Player player) {
        Component message = errorComponent("player not found");
        player.sendMessage(message);
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }

    private void logShowOwnBalance(String requester) {
        plugin.getLogger().info("[CE.Balance] " + requester + " checked its own balance");
    }

    private void logShowOtherBalance(String requester, String target) {
        plugin.getLogger().info("[CE.Balance] " + requester + " checked " + target + "'s balance.");
    }
}