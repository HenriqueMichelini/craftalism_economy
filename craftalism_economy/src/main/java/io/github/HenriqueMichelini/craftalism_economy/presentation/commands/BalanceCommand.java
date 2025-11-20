package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.BalanceMessages;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.validators.PlayerValidator;
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
    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;

    private final BalanceMessages messages;

    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin,
                          CurrencyFormatter currencyFormatter, PlayerValidator playerValidator, PluginLogger logger, BalanceMessages message) {
        this.messages = message;
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

        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.currencyFormatter = currencyFormatter;
        this.playerValidator = playerValidator;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

        if (!playerValidator.isSenderAPlayer(sender)) {
            messages.sendBalancePlayerOnly();
            return false;
        }

        Player player = (Player) sender;

        try {
            return switch (args.length) {
                case 0 -> handleOwnBalance(player);
                case 1 -> handleOtherBalance(player, args[0]);
                default -> {
                    messages.sendBalanceUsage(player);
                    yield true;
                }
            };
        } catch (Exception e) {
            plugin.getLogger().warning( " Error executing balance command: " + e.getMessage());
            messages.sendBalanceError(player);
            return true;
        }
    }

    private boolean handleOwnBalance(Player player) {
        UUID playerUuid = player.getUniqueId();

        if (!balanceManager.checkIfBalanceExists(playerUuid)) {
            balanceManager.createBalance(playerUuid);
        }

        long balance = balanceManager.getBalance(playerUuid);

        String balanceFormatted = currencyFormatter.formatCurrency(balance);

        messages.sendBalanceSelfSuccess(player, balanceFormatted);

        return true;
    }

    private boolean handleOtherBalance(Player requester, String targetName) {
        if (targetName == null || targetName.trim().isEmpty()) {
            messages.sendBalanceOtherNotFound(requester, targetName);
            messages.sendBalanceUsage(requester);
            return false;
        }

        Optional<OfflinePlayer> targetPlayer = playerValidator.resolvePlayer(requester, targetName);

        if (targetPlayer.isEmpty()) {
            messages.sendBalanceOtherNotFound(requester, targetName);
            return false;
        }

        UUID targetUuid = targetPlayer.get().getUniqueId();

        if (!balanceManager.checkIfBalanceExists(targetUuid)) {
            messages.sendBalanceOtherNoBalance(requester, targetName);
            return false;
        }

        long balance = balanceManager.getBalance(targetUuid);
        String balanceFormatted = currencyFormatter.formatCurrency(balance);

        messages.sendBalanceOtherSuccess(requester, targetName, balanceFormatted);

        return true;
    }
}