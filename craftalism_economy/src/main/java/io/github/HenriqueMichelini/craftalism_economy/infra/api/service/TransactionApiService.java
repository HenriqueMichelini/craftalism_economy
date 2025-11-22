package io.github.HenriqueMichelini.craftalism_economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.TransactionResponseDTO;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TransactionApiService {

    private final HttpClientService http;
    private final Gson gson = new Gson();

    public TransactionApiService(HttpClientService http) {
        this.http = http;
    }

    public CompletableFuture<TransactionResponseDTO> register(UUID from, UUID to, long amount) {
        TransactionRequestDTO dto = new TransactionRequestDTO(from, to, amount);

        return http.post("/transactions", gson.toJson(dto))
                .thenApply(resp -> gson.fromJson(resp.body(), TransactionResponseDTO.class));
    }
}
