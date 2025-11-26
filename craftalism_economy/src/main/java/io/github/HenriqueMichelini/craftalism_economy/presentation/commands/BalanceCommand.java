package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.application.dto.BalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism_economy.application.service.BalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.BalanceMessages;
import io.github.HenriqueMichelini.craftalism_economy.presentation.validation.PlayerNameCheck;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BalanceCommand implements CommandExecutor {

    private final BalanceMessages messages;
    private final PlayerNameCheck playerNameCheck;
    private final BalanceCommandApplicationService balanceService;

    public BalanceCommand(
            BalanceMessages messages,
            PlayerNameCheck playerNameCheck,
            BalanceCommandApplicationService balanceService) {
        this.messages = messages;
        this.playerNameCheck = playerNameCheck;
        this.balanceService = balanceService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 0) {
            return handleSelfBalance(sender);
        }

        return handleOtherBalance(sender, args[0]);
    }

    private boolean handleSelfBalance(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.sendBalancePlayerOnly();
            return true;
        }

        balanceService.executeSelf(player.getUniqueId())
                .thenAccept(result -> handleResult(player, result, player.getName()));

        return true;
    }

    private boolean handleOtherBalance(CommandSender sender, String targetName) {
        if (!(sender instanceof Player player)) {
            messages.sendBalancePlayerOnly();
            return true;
        }

        if (!playerNameCheck.isValid(targetName)) {
            messages.sendBalanceInvalidName(player);
            return true;
        }

        balanceService.executeOther(targetName)
                .thenAccept(result -> handleResult(player, result, targetName));

        return true;
    }

    private void handleResult(Player sender, BalanceExecutionResult result, String targetName) {
        switch (result.status()) {
            case SUCCESS_SELF ->
                    messages.sendBalanceSelfSuccess(sender, String.valueOf(result.amount()));

            case SUCCESS_OTHER ->
                    messages.sendBalanceOtherSuccess(sender, targetName, String.valueOf(result.amount()));

            case NO_BALANCE ->
                    messages.sendBalanceOtherNoBalance(sender, targetName);

            case NOT_FOUND ->
                    messages.sendBalanceOtherNotFound(sender, targetName);

            default ->
                    messages.sendBalanceError(sender);
        }
    }
}