package io.github.HenriqueMichelini.craftalism_economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism_economy.application.service.PlayerApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.PlayerCacheRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.logging.Logger;

public class OnQuit {
    private final PlayerApplicationService service;
    private final Logger logger;
    private final PlayerCacheRepository cache;

    public OnQuit(PlayerApplicationService service, Logger logger, PlayerCacheRepository cache) {
        this.service = service;
        this.logger = logger;
        this.cache = cache;
    }

    @EventHandler
    public void OnPlayerQuit(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();

        cache.delete(uuid);
    }
}