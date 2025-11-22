package io.github.HenriqueMichelini.craftalism_economy.infra.api.repository;

import io.github.HenriqueMichelini.craftalism_economy.domain.model.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCacheRepository {
    private final Map<UUID, Player> cache = new ConcurrentHashMap<UUID, Player>();

    public Optional<Player> find(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public void save(Player player) {
        cache.put(player.getUuid(), new Player(player.getUuid(), player.getName(), player.getCreatedAt())); // copy constructor
    }

    public void delete(UUID uuid) {
        cache.remove(uuid);
    }
}