package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism_economy.application.service.PayCommandApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.enums.PayResult;
import io.github.HenriqueMichelini.craftalism_economy.presentation.validation.PlayerNameCheck;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PayCommand implements CommandExecutor {
    private static final String PERMISSION = "craftalism.pay";

    private final PayMessages messages;
    private final PayCommandApplicationService service;
    private final PlayerNameCheck playerNameCheck;

    public PayCommand(PayMessages messages, PayCommandApplicationService service, PlayerNameCheck playerNameCheck) {
        this.messages = messages;
        this.service = service;
        this.playerNameCheck = playerNameCheck;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendPayPlayerOnly();
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            messages.sendPayNoPermission(player);
            return true;
        }

        if (args.length < 2) {
            messages.sendPayUsage(player);
            return true;
        }

        String targetName = args[0];

        if (!playerNameCheck.isValid(targetName)) {
            messages.sendPayInvalidName(player);
            return true;
        }

        String amountStr = args[1];

        if (amountStr.isEmpty()) {
            messages.sendPayAmountEmpty(player);
            return true;
        }

        if (!amountStr.matches("\\d+")) {
            messages.sendPayInvalidAmount(player);
            return true;
        }


        long amount = Long.parseLong(amountStr);

        if (targetName.equalsIgnoreCase(player.getName())) {
            messages.sendPaySelfPayment(player);
            return true;
        }

        service.execute(player.getUniqueId(), player.getName(), targetName, amount)
                .thenAccept(result -> {
                    switch (result) {
                        case PayResult.SUCCESS -> {
                            messages.sendPaySuccessSender(player, amountStr, targetName);
                            messages.sendPaySuccessReceiver(player, amountStr, targetName);
                        }
                        case PayResult.TARGET_NOT_FOUND -> messages.sendPayPlayerNotFound(player);
                        case PayResult.NOT_ENOUGH_FUNDS -> messages.sendPayInsufficientFunds(player);
                        case PayResult.INVALID_AMOUNT -> messages.sendPayInvalidAmount(player);
                        case PayResult.CANNOT_PAY_SELF -> messages.sendPaySelfPayment(player);
                        default -> messages.sendPayException(player);
                    }
                });


        return true;
    }
}
