package io.github.HenriqueMichelini.craftalism_economy.core.validators;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerValidator {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;

    public Optional<OfflinePlayer> resolvePlayer(Player requester, @Nullable String username) {
        if (username == null || username.isBlank()) {
            requester.sendMessage(Component.text("Username cannot be empty.").color(ERROR_COLOR));
            return Optional.empty();
        }

        Player onlinePlayer = Bukkit.getPlayerExact(username);
        if (onlinePlayer != null) {
            return Optional.of(onlinePlayer);
        }

        OfflinePlayer offlinePlayer;
        try {
            offlinePlayer = Bukkit.getOfflinePlayerIfCached(username);
        } catch (Exception e) {
            offlinePlayer = Bukkit.getOfflinePlayer(username);
        }

        if (offlinePlayer != null) {
            boolean isRealPlayer = (offlinePlayer.getUniqueId().version() != 0) && offlinePlayer.hasPlayedBefore();
            if (isRealPlayer) {
                return Optional.of(offlinePlayer);
            }
        }

        requester.sendMessage(Component.text("Player '" + username + "' not found. They must have joined this server at least once.").color(ERROR_COLOR));
        return Optional.empty();
    }

    public boolean isSenderAPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        sender.sendMessage(Component.text("This command can only be executed by a player.").color(ERROR_COLOR));
        return false;
    }
}
