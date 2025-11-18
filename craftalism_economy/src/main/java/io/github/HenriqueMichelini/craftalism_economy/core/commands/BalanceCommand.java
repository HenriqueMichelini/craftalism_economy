package io.github.HenriqueMichelini.craftalism_economy.core.commands;

import io.github.HenriqueMichelini.craftalism_economy.core.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.core.logs.LogManager;
import io.github.HenriqueMichelini.craftalism_economy.core.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism_economy.core.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.core.validators.PlayerValidator;
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
    private static final String LOG_PREFIX = "[CE.Balance]";

    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;
    private final PluginLogger logger;

    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin,
                          CurrencyFormatter currencyFormatter, PlayerValidator playerValidator, PluginLogger logger) {
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
        this.logger = logger;
    }

//    @Deprecated
//    public BalanceCommand(BalanceManager balanceManager, JavaPlugin plugin, CurrencyFormatter currencyFormatter) {
//        this(balanceManager, plugin, currencyFormatter, new PlayerValidator());
//    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

        if (!playerValidator.isSenderAPlayer(sender)) {
            sender.sendMessage(errorComponent("This command can only be used by players."));
            return false;
        }

        Player player = (Player) sender;

        try {
            return switch (args.length) {
                case 0 -> handleOwnBalance(player);
                case 1 -> handleOtherBalance(player, args[0]);
                default -> {
                    sendUsageMessage(player);
                    yield true;
                }
            };
        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing balance command: " + e.getMessage());
            player.sendMessage(errorComponent("An error occurred while retrieving the balance."));
            return true;
        }
    }

    private boolean handleOwnBalance(Player player) {
        UUID playerUuid = player.getUniqueId();

        if (!balanceManager.checkIfBalanceExists(playerUuid)) {
            balanceManager.createBalance(playerUuid);
        }

        long balance = balanceManager.getBalance(playerUuid);
        sendOwnBalanceMessage(player, balance);
        //logShowOwnBalance(player.getName());
        return true;
    }

    private boolean handleOtherBalance(Player requester, String targetName) {
        if (targetName == null || targetName.trim().isEmpty()) {
            sendPlayerNotFoundMessage(requester);
            sendUsageMessage(requester);
            return false;
        }

        Optional<OfflinePlayer> targetPlayer = playerValidator.resolvePlayer(requester, targetName);
        if (targetPlayer.isEmpty()) {
            sendPlayerNotFoundMessage(requester);
            return false;
        }

        UUID targetUuid = targetPlayer.get().getUniqueId();

        if (!balanceManager.checkIfBalanceExists(targetUuid)) {
            requester.sendMessage(errorComponent("Player " + targetName + " doesn't have a balance."));
            return false;
        }

        long balance = balanceManager.getBalance(targetUuid);
        sendOtherBalanceMessage(requester, targetName, balance);
        //logShowOtherBalance(requester.getName(), targetName);
        return true;
    }

    private void sendOwnBalanceMessage(Player player, long balance) {
        String formatted = currencyFormatter.formatCurrency(balance);
        logger.send(player, "balance.self.success", new LogManager.Placeholder("balance", formatted));
    }

    private void sendOtherBalanceMessage(Player recipient, String targetName, long balance) {
        String formatted = currencyFormatter.formatCurrency(balance);
        Component message = Component.text(targetName + "'s balance is: ")
                .color(PREFIX_COLOR)
                .append(Component.text(formatted).color(BALANCE_COLOR));
        recipient.sendMessage(message);
    }

//    private void logShowOwnBalance(String requester) {
//        plugin.getLogger().info(LOG_PREFIX + " " + requester + " checked their own balance");
//    }
//
//    private void logShowOtherBalance(String requester, String target) {
//        plugin.getLogger().info(LOG_PREFIX + " " + requester + " checked " + target + "'s balance");
//    }
}