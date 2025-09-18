package io.github.HenriqueMichelini.craftalism_economy.economy.validators;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;

import java.util.UUID;

public class EconomyValidator {
    private final BalanceManager balanceManager;

    public EconomyValidator(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    public boolean hasSufficientFunds(UUID playerUUID, long amount) {
        if (!isValidAmount(amount)) {
            return false;
        }
        return balanceManager.getBalance(playerUUID) >= amount;
    }

    public boolean isValidAmount(long amount) {
        return amount >= 0;
    }
}
