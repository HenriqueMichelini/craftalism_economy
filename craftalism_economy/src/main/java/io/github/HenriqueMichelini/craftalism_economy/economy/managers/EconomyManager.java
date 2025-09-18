package io.github.HenriqueMichelini.craftalism_economy.economy.managers;

import io.github.HenriqueMichelini.craftalism_economy.economy.validators.EconomyValidator;

import java.util.UUID;

public class EconomyManager {
    private final BalanceManager balanceManager;
    private final EconomyValidator economyValidator;

    public EconomyManager(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
        this.economyValidator = new EconomyValidator(balanceManager);
    }

    public boolean deposit(UUID playerUUID, long amount) {
        if (economyValidator.isValidAmount(amount)) {
            balanceManager.setBalance(playerUUID, balanceManager.getBalance(playerUUID) + amount);
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID playerUUID, long amount) {
        if (economyValidator.isValidAmount(amount) && economyValidator.hasSufficientFunds(playerUUID, amount)) {
            balanceManager.setBalance(playerUUID, balanceManager.getBalance(playerUUID)- amount);
            return true;
        }
        return false;
    }

    public boolean transferBalance(UUID from, UUID to, long amount) {
        if (economyValidator.isValidAmount(amount) && economyValidator.hasSufficientFunds(from, amount)) {
            withdraw(from, amount);
            deposit(to, amount);
            return true;
        }
        return false;
    }
}