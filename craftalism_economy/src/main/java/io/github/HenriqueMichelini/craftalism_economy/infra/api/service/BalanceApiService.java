package io.github.HenriqueMichelini.craftalism_economy.infra.api.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exception.NotFoundException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BalanceApiService {

    private final HttpClientService http;
    private final Gson gson = new Gson();

    public BalanceApiService(HttpClientService http) {
        this.http = http;
    }

    public CompletableFuture<BalanceResponseDTO> getBalance(UUID uuid) {
        return http.get("/balances/" + uuid)
                .thenCompose(resp -> {
                    if (resp.statusCode() == 404) {
                        return CompletableFuture.failedFuture(new NotFoundException());
                    }
                    return CompletableFuture.completedFuture(
                            gson.fromJson(resp.body(), BalanceResponseDTO.class)
                    );
                });
    }

    public CompletableFuture<BalanceResponseDTO> createBalance(UUID uuid) {
        BalanceResponseDTO dto = new BalanceResponseDTO(uuid, 0L);

        return http.post("/balances", gson.toJson(dto))
                .thenApply(response -> gson.fromJson(response.body(), BalanceResponseDTO.class));
    }

    public CompletableFuture<BalanceResponseDTO> getOrCreateBalance(UUID uuid) {
        return getBalance(uuid)
                .exceptionallyCompose(ex -> {
                    if (ex instanceof NotFoundException) {
                        return createBalance(uuid);
                    }
                    return CompletableFuture.failedFuture(ex);
                });
    }

    public CompletableFuture<BalanceResponseDTO> updateBalance(UUID uuid, Long amount) {
        BalanceResponseDTO dto = new BalanceResponseDTO(uuid, amount);
        String body = gson.toJson(dto);

        return http.put("/balances/" + uuid, body)
                .thenCompose(resp -> {
                    if (resp.statusCode() == 404) {
                        return CompletableFuture.failedFuture(new NotFoundException());
                    }
                    return CompletableFuture.completedFuture(
                            gson.fromJson(resp.body(), BalanceResponseDTO.class)
                    );
                });
    }

    public CompletableFuture<Void> deposit(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);

        return http.post("/balances/" + uuid + "/deposit", gson.toJson(dto))
                .thenApply(resp -> null);
    }

    public CompletableFuture<Void> withdraw(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);

        return http.post("/balances/" + uuid + "/withdraw", gson.toJson(dto))
                .thenApply(resp -> null);
    }

    public CompletableFuture<List<BalanceResponseDTO>> getTopBalances(int limit) {
        return http.get("/balances/top?limit=" + limit)
                .thenApply(resp -> {
                    Type listType = new TypeToken<List<BalanceResponseDTO>>(){}.getType();
                    return gson.fromJson(resp.body(), listType);
                });
    }
}