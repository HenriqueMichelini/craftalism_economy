package io.github.HenriqueMichelini.craftalism_economy.infra.api.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exceptions.ApiException;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exceptions.ApiServerException;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.exceptions.RateLimitException;

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
                    int status = resp.statusCode();
                    String body = resp.body();

                    if (status == 200) {
                        try {
                            BalanceResponseDTO dto = parseJson(body);
                            return CompletableFuture.completedFuture(dto);
                        } catch (ApiException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    }

                    return CompletableFuture.failedFuture(mapStatusToException(status, body));
                });
    }

    public CompletableFuture<BalanceResponseDTO> createBalance(UUID uuid) {
        BalanceResponseDTO dto = new BalanceResponseDTO(uuid, 0L);
        return http.post("/balances", gson.toJson(dto))
                .thenCompose(resp -> {
                    int status = resp.statusCode();
                    String body = resp.body();

                    if (status == 201 || status == 200) {
                        try {
                            BalanceResponseDTO parsed = parseJson(body);
                            return CompletableFuture.completedFuture(parsed);
                        } catch (ApiException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    }

                    return CompletableFuture.failedFuture(mapStatusToException(status, body));
                });
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
                    int status = resp.statusCode();
                    String respBody = resp.body();

                    if (status == 200) {
                        try {
                            BalanceResponseDTO parsed = parseJson(respBody);
                            return CompletableFuture.completedFuture(parsed);
                        } catch (ApiException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    }

                    return CompletableFuture.failedFuture(mapStatusToException(status, respBody));
                });
    }

    public CompletableFuture<Void> deposit(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);

        return http.post("/balances/" + uuid + "/deposit", gson.toJson(dto))
                .thenCompose(resp -> {
                    int status = resp.statusCode();
                    String body = resp.body();

                    if (status == 200 || status == 204) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return CompletableFuture.failedFuture(mapStatusToException(status, body));
                });
    }

    public CompletableFuture<Void> withdraw(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);

        return http.post("/balances/" + uuid + "/withdraw", gson.toJson(dto))
                .thenCompose(resp -> {
                    int status = resp.statusCode();
                    String body = resp.body();

                    if (status == 200 || status == 204) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return CompletableFuture.failedFuture(mapStatusToException(status, body));
                });
    }

    public CompletableFuture<List<BalanceResponseDTO>> getTopBalances(int limit) {
        return http.get("/balances/top?limit=" + limit)
                .thenCompose(resp -> {
                    int status = resp.statusCode();
                    String body = resp.body();

                    if (status == 200) {
                        try {
                            Type listType = new TypeToken<List<BalanceResponseDTO>>() {}.getType();
                            List<BalanceResponseDTO> list = parseJson(body, listType);
                            return CompletableFuture.completedFuture(list);
                        } catch (ApiException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    }

                    return CompletableFuture.failedFuture(mapStatusToException(status, body));
                });
    }

    // ---- Helpers ----

    private <T> T parseJson(String body) {
        try {
            T parsed = gson.fromJson(body, (Class<T>) BalanceResponseDTO.class);
            if (parsed == null) {
                throw new ApiException("Parsed JSON was null for type: " + BalanceResponseDTO.class.getSimpleName() + ", body: " + safePreview(body));
            }
            return parsed;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to parse JSON for " + BalanceResponseDTO.class.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private <T> T parseJson(String body, Type typeOfT) {
        try {
            T parsed = gson.fromJson(body, typeOfT);
            if (parsed == null) {
                throw new ApiException("Parsed JSON was null for type: " + typeOfT.getTypeName() + ", body: " + safePreview(body));
            }
            return parsed;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to parse JSON for " + typeOfT.getTypeName() + ": " + e.getMessage(), e);
        }
    }

    private ApiException mapStatusToException(int status, String body) {
        if (status == 404) {
            return new NotFoundException("Resource not found (status=404). Body: " + safePreview(body));
        }

        if (status == 429) {
            return new RateLimitException("Rate limit exceeded (status=429). Body: " + safePreview(body));
        }

        if (status >= 500) {
            return new ApiServerException("Server error (status=" + status + "). Body: " + safePreview(body));
        }

        return new ApiException("Unexpected status: " + status + ". Body: " + safePreview(body));
    }

    private String safePreview(String body) {
        if (body == null) return "";
        final int max = 500;
        if (body.length() <= max) return body;
        return body.substring(0, max) + "...(truncated)";
    }
}
