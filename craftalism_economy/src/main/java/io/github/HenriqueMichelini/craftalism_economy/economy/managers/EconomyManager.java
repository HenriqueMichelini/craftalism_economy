package io.github.HenriqueMichelini.craftalism_economy.economy.managers;

import io.github.HenriqueMichelini.craftalism_economy.economy.validators.EconomyValidator;

import java.util.UUID;

public class EconomyManager {
    private final BalanceManager balanceManager;
    private final EconomyValidator economyValidator;

    public EconomyManager(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
        this.economyValidator = new EconomyValidator();
    }

    public boolean deposit(UUID playerUUID, long amount) {
        if (economyValidator.isGreaterThanZero(amount)) {
            balanceManager.setBalance(playerUUID, balanceManager.getBalance(playerUUID) + amount);
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID playerUUID, long amount) {
        if (!economyValidator.isGreaterThanZero(amount)) {
            return false;
        }

        long currentBalance = balanceManager.getBalance(playerUUID);

        if (!economyValidator.hasSufficientFunds(currentBalance, amount)) {
            return false;
        }

        balanceManager.setBalance(playerUUID, currentBalance - amount);
        return true;
    }

    public boolean transferBalance(UUID from, UUID to, long amount) {
        if (!economyValidator.isGreaterThanZero(amount)) {
            return false;
        }

        long fromBalance = balanceManager.getBalance(from);
        long toBalance = balanceManager.getBalance(to);

        if (!economyValidator.hasSufficientFunds(fromBalance, amount)) {
            return false;
        }

        balanceManager.setBalance(from, fromBalance - amount);
        balanceManager.setBalance(to, toBalance + amount);
        return true;
    }
}