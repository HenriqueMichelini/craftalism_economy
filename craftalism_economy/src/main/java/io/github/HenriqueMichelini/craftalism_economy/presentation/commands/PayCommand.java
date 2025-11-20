package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.managers.EconomyManager;
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

public class PayCommand implements CommandExecutor {
    private static final String LOG_PREFIX = "[CE.Pay]";

    private final EconomyManager economyManager;
    private final BalanceManager balanceManager;
    private final JavaPlugin plugin;
    private final CurrencyFormatter currencyFormatter;
    private final PlayerValidator playerValidator;
    private final CurrencyParser currencyParser;

    private final PayMessages messages;

    public PayCommand(
            @NotNull EconomyManager economyManager,
            @NotNull BalanceManager balanceManager,
            @NotNull JavaPlugin plugin,
            @NotNull CurrencyFormatter currencyFormatter,
            @NotNull PlayerValidator playerValidator,
            @NotNull CurrencyParser currencyParser,
            @NotNull PayMessages messages
    ) {
        this.economyManager = economyManager;
        this.balanceManager = balanceManager;
        this.plugin = plugin;
        this.currencyFormatter = currencyFormatter;
        this.playerValidator = playerValidator;
        this.currencyParser = currencyParser;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!playerValidator.isSenderAPlayer(sender)) {
            messages.sendPayPlayerOnly();
            return true;
        }

        Player payer = (Player) sender;

        try {
            // tem jeitos melhores
//            if (!validateArguments(payer, args)) {
//                return true;
//            }

            Optional<OfflinePlayer> payeeOpt = resolvePayee(payer, args[0]);
            if (payeeOpt.isEmpty()) {
                return true;
            }

            OfflinePlayer payee = payeeOpt.get();
            UUID payeeUuid = payee.getUniqueId();

            if (!validateNotSelfPayment(payer, payeeUuid)) {
                return true;
            }

            Optional<Long> amountOpt = currencyParser.parseAmount(payer, args[1]);
            if (amountOpt.isEmpty()) {
                return true;
            }

            long amount = amountOpt.get();

            if (!processPayment(payer, payeeUuid, amount)) {
                return true;
            }

            sendSuccessMessages(payer, payee, amount);
            //logTransaction(payer, payee, amount);

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning(LOG_PREFIX + " Error executing pay command: " + e.getMessage());
            messages.sendPayException(payer);
            return true;
        }
    }

    private Optional<OfflinePlayer> resolvePayee(Player payer, String payeeName) {
        Optional<OfflinePlayer> playerOpt = playerValidator.resolvePlayer(payer, payeeName);

        if (playerOpt.isEmpty()) {
            messages.sendPayPlayerNotFound(payer);
            return Optional.empty();
        }

        OfflinePlayer player = playerOpt.get();
        UUID playerUuid = player.getUniqueId();

        if (!balanceManager.checkIfBalanceExists(playerUuid)) {
            messages.sendPayNoAccount(payer, payeeName);
            return Optional.empty();
        }

        return playerOpt;
    }

    private boolean validateNotSelfPayment(Player payer, UUID payeeUuid) {
        if (payer.getUniqueId().equals(payeeUuid)) {
            messages.sendPaySelfPayment(payer);
            return false;
        }
        return true;
    }

    private boolean processPayment(Player payer, UUID payeeUuid, long amount) {
        boolean success = economyManager.transferBalance(payer.getUniqueId(), payeeUuid, amount);

        if (!success) {
            messages.sendPayInsufficientFunds(payer);
            return false;
        }

        return true;
    }

    private void sendSuccessMessages(Player payer, OfflinePlayer payee, long amount) {
        String formattedAmount = currencyFormatter.formatCurrency(amount);
        String payeeName = getPlayerName(payee);

        messages.sendPaySuccessSender(payer, formattedAmount, payeeName);
        messages.sendPaySuccessReceiver(payee.getPlayer(), formattedAmount, payer.getName());

//        if (payee instanceof Player onlinePayee) {
//            Component payeeMessage = buildReceivedMessage(
//                    "You received ", formattedAmount, " from ", payer.getName()
//            );
//            onlinePayee.sendMessage(payeeMessage);
//        }
    }

    //  isso deveria estar no resolver talvez?
    private String getPlayerName(OfflinePlayer player) {
        String name = player.getName();
        return (name != null && !name.isEmpty()) ? name : "Unknown Player";
    }
}