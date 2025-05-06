package io.github.HenriqueMichelini.craftalism_economy.economy.util;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Validators {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private final BalanceManager balanceManager;

    public Validators(EconomyManager economyManager, BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    public OfflinePlayer resolvePlayer(CommandSender sender, String username) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        if (player.hasPlayedBefore() || player.isOnline()) return player;

        sender.sendMessage(Component.text("Player not found.").color(ERROR_COLOR));
        return null;
    }

    public boolean validateTarget(OfflinePlayer target) {
        target.getUniqueId();
        return true;
    }

    public boolean validateSender(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(Component.text("Only players can use this command.").color(ERROR_COLOR));
        return false;
    }

    public boolean validateArguments(CommandSender sender, String[] args, int numOfArgs) {
        if (args.length == numOfArgs) return true;

        sender.sendMessage(Component.text("Usage: /setbalance <player> <amount>")
                .color(ERROR_COLOR));
        return false;
    }

    public boolean hasSufficientFunds(UUID playerUUID, long amount) {
        System.out.println("amount: " + amount);
        System.out.println("player balance: " + balanceManager.getBalance(playerUUID));
        return balanceManager.getBalance(playerUUID) - amount >= 0;
    }

    public boolean isValidAmount(long amount) {
        return amount >= 0;
    }
}
