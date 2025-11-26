package io.github.HenriqueMichelini.craftalism_economy.infra.api.repository;

import io.github.HenriqueMichelini.craftalism_economy.domain.model.Balance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceCacheRepository {
    private final Map<UUID, Balance> cache = new ConcurrentHashMap<UUID, Balance>();

    public Optional<Balance> find(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public void save(Balance balance) {
        cache.put(balance.getUuid(), new Balance(balance.getUuid(), balance.getAmount()));
    }

    public void delete(UUID uuid) {
        cache.remove(uuid);
    }
}