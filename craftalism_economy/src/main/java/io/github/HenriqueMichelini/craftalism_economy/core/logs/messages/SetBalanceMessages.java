package io.github.HenriqueMichelini.craftalism_economy.core.logs.messages;

import io.github.HenriqueMichelini.craftalism_economy.core.logs.LogManager;
import io.github.HenriqueMichelini.craftalism_economy.core.logs.PluginLogger;
import org.bukkit.entity.Player;

public class SetBalanceMessages {
    private final PluginLogger pluginLogger;

    public SetBalanceMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendSetBalanceSuccessSender(Player messageReceiver, String targetName, String amount) {
        LogManager.Placeholder targetPlaceholder = new LogManager.Placeholder("target", targetName);
        LogManager.Placeholder amountPlaceholder = new LogManager.Placeholder("amount", amount);

        pluginLogger.send(messageReceiver, "setbalance.success.sender", targetPlaceholder, amountPlaceholder);
    }

    public void sendSetBalanceSuccessReceiver(Player messageReceiver, String amount, String senderName) {
        LogManager.Placeholder amountPlaceholder = new LogManager.Placeholder("amount", amount);
        LogManager.Placeholder senderPlaceholder = new LogManager.Placeholder("sender", senderName);

        pluginLogger.send(messageReceiver, "setbalance.success.receiver", amountPlaceholder, senderPlaceholder);
    }

    public void sendSetBalanceNoPermission(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.no_permission");
    }

    public void sendSetBalancePlayerOnly(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.player_only");
    }

    public void sendSetBalanceUsage(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.usage");
    }

    public void sendSetBalancePlayerEmpty(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.player_empty");
    }

    public void sendSetBalanceAmountEmpty(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.amount_empty");
    }

    public void sendSetBalancePlayerNotFound(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.player_not_found");
    }

    public void sendSetBalanceInvalidAmount(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.invalid_amount");
    }

    public void sendSetBalanceUpdateFailed(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.update_failed");
    }

    public void sendSetBalanceException(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.exception");
    }
}