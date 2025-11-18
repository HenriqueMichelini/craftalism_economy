package io.github.HenriqueMichelini.craftalism_economy.core.managers;

import io.github.HenriqueMichelini.craftalism_economy.core.validators.EconomyValidator;

import java.util.UUID;

public class EconomyManager {
    private final BalanceManager balanceManager;
    private final EconomyValidator economyValidator;

    public EconomyManager(BalanceManager balanceManager, EconomyValidator economyValidator) {
        if (balanceManager == null) {
            throw new IllegalArgumentException("BalanceManager cannot be null");
        }
        if (economyValidator == null) {
            throw new IllegalArgumentException("EconomyValidator cannot be null");
        }
        this.balanceManager = balanceManager;
        this.economyValidator = economyValidator;
    }

    public boolean deposit(UUID playerUUID, long amount) {
        if (playerUUID == null) return false;
        if (!economyValidator.isGreaterThanZero(amount)) return false;

        long currentBalance = balanceManager.getBalance(playerUUID);

        if (willOverflow(currentBalance, amount)) {
            return false;
        }

        balanceManager.setBalance(playerUUID, currentBalance + amount);
        return true;
    }

    public boolean withdraw(UUID playerUUID, long amount) {
        if (playerUUID == null) return false;
        if (!economyValidator.isGreaterThanZero(amount)) return false;

        long currentBalance = balanceManager.getBalance(playerUUID);

        if (!economyValidator.hasSufficientFunds(currentBalance, amount)) return false;

        balanceManager.setBalance(playerUUID, currentBalance - amount);
        return true;
    }

    public boolean transferBalance(UUID from, UUID to, long amount) {
        if (from == null || to == null) return false;
        if (!economyValidator.isGreaterThanZero(amount)) return false;

        if (from.equals(to)) return false;

        long fromBalance = balanceManager.getBalance(from);
        long toBalance = balanceManager.getBalance(to);

        if (!economyValidator.hasSufficientFunds(fromBalance, amount)) return false;

        if (willOverflow(toBalance, amount)) {
            return false;
        }

        balanceManager.setBalance(from, fromBalance - amount);
        balanceManager.setBalance(to, toBalance + amount);
        return true;
    }

    private boolean willOverflow(long currentBalance, long amount) {
        return currentBalance > Long.MAX_VALUE - amount;
    }
}