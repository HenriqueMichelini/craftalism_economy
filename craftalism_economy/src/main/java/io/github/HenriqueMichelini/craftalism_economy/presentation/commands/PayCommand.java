package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism_economy.application.service.PayApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.enums.PayResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PayCommand implements CommandExecutor {

    private final PayMessages messages;
    private final PayApplicationService service;

    public PayCommand(PayMessages messages, PayApplicationService service) {
        this.messages = messages;
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendPayPlayerOnly();
            return true;
        }

        if (args.length < 2) {
            messages.sendPayUsage(player);
            return true;
        }

        String targetName = args[0];
        String amountStr = args[1];

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
