package io.github.HenriqueMichelini.craftalism_economy.infra.api.exceptions;

public class ApiTimeoutException extends ClientException {

    public ApiTimeoutException() { }

    public ApiTimeoutException(String message) {
        super(message);
    }

    public ApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
