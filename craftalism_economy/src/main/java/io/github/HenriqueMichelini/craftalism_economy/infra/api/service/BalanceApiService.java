package io.github.HenriqueMichelini.craftalism_economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceUpdateRequestDTO;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BalanceApiService {

    private final HttpClientService http;
    private final Gson gson = new Gson();

    public BalanceApiService(HttpClientService http) {
        this.http = http;
    }

    public CompletableFuture<Long> getBalance(UUID uuid) {
        return http.get("/balances/" + uuid)
                .thenApply(resp -> {
                    BalanceResponseDTO dto = gson.fromJson(resp.body(), BalanceResponseDTO.class);
                    return dto.amount();
                });
    }

    public CompletableFuture<Void> deposit(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);

        return http.post("/balances/" + uuid + "/deposit", gson.toJson(dto))
                .thenApply(resp -> null); // ignoramos corpo
    }

    public CompletableFuture<Void> withdraw(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);

        return http.post("/balances/" + uuid + "/withdraw", gson.toJson(dto))
                .thenApply(resp -> null);
    }
}