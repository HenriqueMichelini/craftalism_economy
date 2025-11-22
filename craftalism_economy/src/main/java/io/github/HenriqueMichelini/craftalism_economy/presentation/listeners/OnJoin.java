package io.github.HenriqueMichelini.craftalism_economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism_economy.application.service.PlayerApplicationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.logging.Logger;

public class OnJoin implements Listener {
    private final PlayerApplicationService service;
    private final Logger logger;

    public OnJoin(PlayerApplicationService service, Logger logger) {
        this.service = service;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        String name = player.getName();

        service.loadPlayerOnJoin(uuid, name)
                .exceptionally(ex -> {
                    logger.warning("Erro ao carregar player " + uuid + ": " + ex);
                    return null;
                });

    }
}