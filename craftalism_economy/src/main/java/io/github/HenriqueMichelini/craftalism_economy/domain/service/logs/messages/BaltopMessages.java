package io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.PluginLogger;
import org.bukkit.entity.Player;

public class BaltopMessages {
    private final PluginLogger pluginLogger;

    public BaltopMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendBaltopHeader(Player messageReceiver, String count, String page, String total) {
        LogManager.Placeholder countPlaceholder = new LogManager.Placeholder("count", count);
        LogManager.Placeholder pagePlaceholder = new LogManager.Placeholder("page", page);
        LogManager.Placeholder totalPlaceholder = new LogManager.Placeholder("total", total);

        pluginLogger.send(messageReceiver, "baltop.header", countPlaceholder, pagePlaceholder, totalPlaceholder);
    }

    public void sendBaltopEntry(Player messageReceiver, String rank, String player, String balance) {
        LogManager.Placeholder rankPlaceholder = new LogManager.Placeholder("rank", rank);
        LogManager.Placeholder playerPlaceholder = new LogManager.Placeholder("player", player);
        LogManager.Placeholder balancePlaceholder = new LogManager.Placeholder("balance", balance);

        pluginLogger.send(messageReceiver, "baltop.entry", rankPlaceholder, playerPlaceholder, balancePlaceholder);
    }

    public void sendBaltopFooterNext(Player messageReceiver, String nextPage) {
        LogManager.Placeholder nextPlaceholder = new LogManager.Placeholder("next", nextPage);

        pluginLogger.send(messageReceiver, "baltop.footer_next", nextPlaceholder);
    }

    public void sendBaltopNoData(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.no_data");
    }

    public void sendBaltopInvalidPage(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.invalid_page");
    }

    public void sendBaltopPageNotExist(Player messageReceiver, String page, String total) {
        LogManager.Placeholder pagePlaceholder = new LogManager.Placeholder("page", page);
        LogManager.Placeholder totalPlaceholder = new LogManager.Placeholder("total", total);

        pluginLogger.send(messageReceiver, "baltop.page_not_exist", pagePlaceholder, totalPlaceholder);
    }

    public void sendBaltopError(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.error");
    }

    public void sendBaltopUsage(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.usage");
    }
}