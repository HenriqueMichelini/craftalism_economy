package io.github.HenriqueMichelini.craftalism_economy.application.service;

import io.github.HenriqueMichelini.craftalism_economy.domain.model.Balance;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exception.NotFoundException;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.BalanceCacheRepository;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.BalanceApiService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BalanceApplicationService {

    private final BalanceApiService api;
    private final BalanceCacheRepository cache;

    public BalanceApplicationService(BalanceApiService api, BalanceCacheRepository cache) {
        this.api = api;
        this.cache = cache;
    }

    public CompletableFuture<Optional<Balance>> getBalance(UUID uuid) {
        return api.getBalance(uuid)
                .thenApply(dto -> Optional.of(toBalance(dto)))
                .exceptionally(ex -> {
                    if (isNotFoundException(ex)) {
                        return Optional.empty();
                    }
                    throw new CompletionException(ex);
                });
    }

    public CompletableFuture<Balance> getOrCreateBalance(UUID uuid) {
        return api.getBalance(uuid)
                .exceptionallyCompose(ex -> {
                    if (isNotFoundException(ex)) {
                        return api.createBalance(uuid);
                    }
                    return CompletableFuture.failedFuture(ex);
                })
                .thenApply(this::toBalance);
    }

    public CompletableFuture<Balance> loadOnJoin(UUID uuid) {
        return getOrCreateBalance(uuid)
                .thenApply(balance -> {
                    cache.save(balance);
                    return balance;
                });
    }

    public CompletableFuture<Balance> syncBalance(UUID uuid) {
        return api.getBalance(uuid)
                .thenApply(dto -> {
                    Balance balance = toBalance(dto);
                    cache.save(balance);
                    return balance;
                });
    }

    public CompletableFuture<Balance> getCachedOrFetch(UUID uuid) {
        return cache.find(uuid)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> loadOnJoin(uuid));
    }

    private Balance toBalance(BalanceResponseDTO dto) {
        return new Balance(dto.uuid(), dto.amount());
    }

    private boolean isNotFoundException(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause instanceof NotFoundException;
    }
}