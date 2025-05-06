package io.github.HenriqueMichelini.craftalism_economy.economy.managers;

import io.github.HenriqueMichelini.craftalism_economy.economy.util.Validators;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final Validators validators;
    private final BalanceManager balanceManager;

    public EconomyManager(Validators validators, BalanceManager balanceManager) {
        this.validators = validators;
        this.balanceManager = balanceManager;
    }

    public boolean deposit(UUID playerUUID, long amount) {
        if (validators.isValidAmount(amount)) {
            balanceManager.setBalance(playerUUID, balanceManager.getBalance(playerUUID) + amount);
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID playerUUID, long amount) {
        if (validators.isValidAmount(amount) && validators.hasSufficientFunds(playerUUID, amount)) {
            balanceManager.setBalance(playerUUID, balanceManager.getBalance(playerUUID)- amount);
            return true;
        }
        return false;
    }

    public boolean transferBalance(UUID from, UUID to, long amount) {
        if (validators.isValidAmount(amount) && validators.hasSufficientFunds(from, amount)) {
            withdraw(from, amount);
            deposit(to, amount);
        }
        return true;
    }

    public Map<UUID, Long> getAllBalances() {
        return new HashMap<>(balanceManager.getAllBalances());
    }
}