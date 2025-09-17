package io.github.HenriqueMichelini.craftalism_economy.economy.util;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class Validators {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;
    private final BalanceManager balanceManager;

    public Validators(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    public Optional<OfflinePlayer> resolvePlayer(Player requester, @Nullable String username) {
        if (username == null || username.isBlank()) {
            requester.sendMessage(Component.text("Username cannot be empty.").color(ERROR_COLOR));
            return Optional.empty();
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        if (player.hasPlayedBefore() || player.isOnline()) {
            return Optional.of(player);
        }

        requester.sendMessage(Component.text("Player not found.").color(ERROR_COLOR));
        return Optional.empty();
    }

    public boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    public boolean validateArguments(CommandSender sender, String[] args, int numOfArgs, String usageMessage) {
        if (args.length == numOfArgs) return true;
        sender.sendMessage(Component.text(usageMessage).color(ERROR_COLOR));
        return false;
    }

    public boolean validateArguments(Player player, String[] args, int numOfArgs, String usageMessage) {
        if (args.length == numOfArgs) return true;
        player.sendMessage(Component.text(usageMessage).color(ERROR_COLOR));
        return false;
    }

    public boolean hasSufficientFunds(UUID playerUUID, long amount) {
        if (!isValidAmount(amount)) {
            return false;
        }
        return balanceManager.getBalance(playerUUID) >= amount;
    }

    public boolean isValidAmount(long amount) {
        return amount >= 0;
    }
}
