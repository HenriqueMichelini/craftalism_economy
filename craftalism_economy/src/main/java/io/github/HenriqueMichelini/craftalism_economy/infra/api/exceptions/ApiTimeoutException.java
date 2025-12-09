package io.github.HenriqueMichelini.craftalism_economy.infra.api.exception;

public class ApiTimeoutException extends RuntimeException {

    public ApiTimeoutException() { }

    public ApiTimeoutException(String message) {
        super(message);
    }

    public ApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
