package io.github.HenriqueMichelini.craftalism_economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism_economy.infra.config.GsonFactory;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exception.NotFoundException;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerApiService {

    private final HttpClientService http;
    private final Gson gson;

    // Construtor padrão (usa GsonFactory)
    public PlayerApiService(HttpClientService http) {
        this(http, GsonFactory.getInstance());
    }

    // Construtor com Gson customizado (para testes)
    public PlayerApiService(HttpClientService http, Gson gson) {
        this.http = http;
        this.gson = gson;
    }

    public CompletableFuture<PlayerResponseDTO> getPlayerByUuid(UUID uuid) {
        return http.get("/players/" + uuid)
                .thenCompose(resp -> {
                    if (resp.statusCode() == 404) {
                        return CompletableFuture.failedFuture(new NotFoundException());
                    }
                    return CompletableFuture.completedFuture(
                            gson.fromJson(resp.body(), PlayerResponseDTO.class)
                    );
                });
    }

    public CompletableFuture<PlayerResponseDTO> getPlayerByName(String name) {
        return http.get("/players/" + name)
                .thenCompose(resp -> {
                    if (resp.statusCode() == 404) {
                        return CompletableFuture.failedFuture(new NotFoundException());
                    }
                    return CompletableFuture.completedFuture(
                            gson.fromJson(resp.body(), PlayerResponseDTO.class)
                    );
                });
    }

    public CompletableFuture<PlayerResponseDTO> createPlayer(UUID uuid, String name) {
        PlayerRequestDTO dto = new PlayerRequestDTO(uuid, name);
        return http.post("/players", gson.toJson(dto))
                .thenApply(response -> gson.fromJson(response.body(), PlayerResponseDTO.class));
    }

    public CompletableFuture<PlayerResponseDTO> getOrCreatePlayer(UUID uuid, String name) {
        return getPlayerByUuid(uuid)
                .exceptionallyCompose(ex -> {
                    if (ex instanceof NotFoundException) {
                        return createPlayer(uuid, name);
                    }
                    return CompletableFuture.failedFuture(ex);
                });
    }
}