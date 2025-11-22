package io.github.HenriqueMichelini.craftalism_economy.application.service;

import io.github.HenriqueMichelini.craftalism_economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exception.NotFoundException;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.PlayerApiService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerApplicationService {
    private final PlayerApiService api;
    private final PlayerCacheRepository cache;

    public PlayerApplicationService(PlayerApiService api, PlayerCacheRepository cache) {
        this.api = api;
        this.cache = cache;
    }

    public CompletableFuture<Player> loadPlayerOnJoin(UUID uuid, String name) {
        return api.getOrCreatePlayer(uuid, name)
                .thenApply(dto -> {
                    Player player = new Player(
                            dto.uuid(),
                            dto.name(),
                            dto.createdAt()
                    );
                    cache.save(player);
                    return player;
                });
    }

    public CompletableFuture<Player> syncPlayer(UUID uuid) {
        return api.getPlayerByUuid(uuid)
                .thenApply(dto -> {
                    Player updated = new Player(
                            dto.uuid(),
                            dto.name(),
                            dto.createdAt()
                    );

                    cache.save(updated);
                    return updated;
                });
    }

    public CompletableFuture<PlayerResponseDTO> getOrCreatePlayer(UUID uuid, String name) {
        return api.getPlayerByUuid(uuid)
                .exceptionallyCompose(ex -> {
                    if (ex instanceof NotFoundException) {
                        return api.createPlayer(uuid, name);
                    }
                    return CompletableFuture.failedFuture(ex);
                });
    }

    public CompletableFuture<Player> getCachedOrFetch(UUID uuid, String name) {
        Optional<Player> cached = cache.find(uuid);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        return api.getOrCreatePlayer(uuid, name)
                .thenApply(dto -> {
                    Player player = new Player(dto.uuid(), dto.name(), dto.createdAt());
                    cache.save(player);
                    return player;
                });
    }
}