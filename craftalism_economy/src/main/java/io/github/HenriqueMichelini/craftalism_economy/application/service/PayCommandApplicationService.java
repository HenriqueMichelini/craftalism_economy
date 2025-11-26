package io.github.HenriqueMichelini.craftalism_economy.application.service;

import io.github.HenriqueMichelini.craftalism_economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.enums.PayResult;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.TransactionApiService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PayCommandApplicationService {

    private final PlayerApplicationService playerService;
    private final PlayerApiService playerApi;
    private final BalanceApiService balanceApi;
    private final TransactionApiService transactionApi;

    public PayCommandApplicationService(
            PlayerApplicationService playerService,
            PlayerApiService playerApi,
            BalanceApiService balanceApi,
            TransactionApiService transactionApi
    ) {
        this.playerService = playerService;
        this.playerApi = playerApi;
        this.balanceApi = balanceApi;
        this.transactionApi = transactionApi;
    }

    public CompletableFuture<PayResult> execute(
            UUID payerUuid,
            String payerName,
            String receiverName,
            long amount
    ) {
        return playerService.getCachedOrFetch(payerUuid, payerName)
                .thenCompose(payer -> processPayment(payer, receiverName, amount))
                .exceptionally(this::handleException);
    }

    private CompletableFuture<PayResult> processPayment(
            Player payer,
            String receiverName,
            long amount
    ) {
        return playerApi.getPlayerByName(receiverName)
                .thenCompose(receiver -> validateAndExecutePayment(payer, receiver, amount))
                .exceptionally(ex -> PayResult.TARGET_NOT_FOUND);
    }

    private CompletableFuture<PayResult> validateAndExecutePayment(
            Player payer,
            PlayerResponseDTO receiver,
            long amount
    ) {
        PayResult validationResult = validatePayment(payer, receiver, amount);
        if (validationResult != PayResult.SUCCESS) {
            return CompletableFuture.completedFuture(validationResult);
        }

        return executeTransfer(payer.getUuid(), receiver.uuid(), amount);
    }

    private PayResult validatePayment(Player payer, PlayerResponseDTO receiver, long amount) {
        if (payer.getUuid().equals(receiver.uuid())) {
            return PayResult.CANNOT_PAY_SELF;
        }

        if (amount <= 0) {
            return PayResult.INVALID_AMOUNT;
        }

        return PayResult.SUCCESS;
    }

    private CompletableFuture<PayResult> executeTransfer(
            UUID payerUuid,
            UUID receiverUuid,
            long amount
    ) {
        return balanceApi.getBalance(payerUuid)
                .thenCompose(balance -> checkBalanceAndTransfer(payerUuid, receiverUuid, amount, balance.amount()));
    }

    private CompletableFuture<PayResult> checkBalanceAndTransfer(
            UUID payerUuid,
            UUID receiverUuid,
            long amount,
            long currentBalance
    ) {
        if (currentBalance < amount) {
            return CompletableFuture.completedFuture(PayResult.NOT_ENOUGH_FUNDS);
        }

        return performTransfer(payerUuid, receiverUuid, amount);
    }

    private CompletableFuture<PayResult> performTransfer(
            UUID payerUuid,
            UUID receiverUuid,
            long amount
    ) {
        return balanceApi.withdraw(payerUuid, amount)
                .thenCompose(v -> balanceApi.deposit(receiverUuid, amount))
                .thenCompose(v -> transactionApi.register(payerUuid, receiverUuid, amount))
                .thenApply(v -> PayResult.SUCCESS);
    }

    private PayResult handleException(Throwable ex) {
        return PayResult.TARGET_NOT_FOUND;
    }
}