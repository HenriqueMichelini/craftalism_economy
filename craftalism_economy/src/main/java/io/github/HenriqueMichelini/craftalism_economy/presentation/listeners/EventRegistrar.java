package io.github.HenriqueMichelini.craftalism_economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism_economy.application.service.*;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class EventRegistrar {
    private final JavaPlugin plugin;
    private final PlayerApplicationService playerApplicationService;

    public EventRegistrar(
            JavaPlugin plugin,
            PlayerApplicationService playerApplicationService

    ) {
        this.plugin = plugin;
        this.playerApplicationService = playerApplicationService;
    }

    public void registerAll() {
        plugin.getServer().getPluginManager().registerEvents(
                new OnJoin(playerApplicationService),
                plugin
        );
    }
}