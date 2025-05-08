package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.Validators;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class BalanceCommand implements CommandExecutor {
    private static final TextColor PREFIX_COLOR = NamedTextColor.GREEN;
    private static final TextColor BALANCE_COLOR = NamedTextColor.WHITE;
    private static final TextColor ERROR_COLOR = NamedTextColor.RED;

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final MoneyFormat moneyFormat;
    private final Validators validators;

    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin, MoneyFormat moneyFormat, Validators validators) {
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.moneyFormat = moneyFormat;
        this.validators = validators;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (validators.validateSender(sender)) {
            Player player = (Player) sender;
            return args.length == 0 ? showOwnBalance(player) :
                    args.length == 1 ? showOtherBalance(player, args[0]) :
                            showUsage(player);
        }

        return false;
    }

    private boolean showOwnBalance(Player player) {
        long balance = balanceManager.getBalance(player.getUniqueId());
        sendBalanceMessage(player, "Your balance is: ", balance);
        logQuery(player.getName(), "self");
        return true;
    }

    private boolean showOtherBalance(Player requester, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target.getName() == null) {
            requester.sendMessage(errorComponent("Player does not exist."));
            return false;
        }

        long balance = balanceManager.getBalance(target.getUniqueId());
        sendBalanceMessage(requester, target.getName() + "'s balance is: ", balance);
        logQuery(requester.getName(), target.getName());
        return true;
    }

    private boolean showUsage(Player player) {
        player.sendMessage(errorComponent("Usage: /balance [player]"));
        return true;
    }

    private void sendBalanceMessage(Player recipient, String prefix, long balance) {
        String formatted = moneyFormat.formatPrice(balance);
        recipient.sendMessage(
                Component.text(prefix)
                        .color(PREFIX_COLOR)
                        .append(Component.text(formatted).color(BALANCE_COLOR))
        );
    }

    private Component errorComponent(String text) {
        return Component.text(text).color(ERROR_COLOR);
    }

    private void logQuery(String requester, String target) {
        plugin.getLogger().info("[Balance] " + requester + " checked balance of " + target);
    }
}