package io.github.HenriqueMichelini.craftalism_economy.infra.api.client;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpClientService {

    private final HttpClient http;
    private final String baseUrl;

    public HttpClientService(String baseUrl) {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.baseUrl = baseUrl;
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");
    }

    public CompletableFuture<HttpResponse<String>> get(String path) {
        return http.sendAsync(
                request(path).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    public CompletableFuture<HttpResponse<String>> post(String path, String body) {
        return http.sendAsync(
                request(path).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    public CompletableFuture<HttpResponse<String>> put(String path, String body) {
        return http.sendAsync(
                request(path).PUT(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
