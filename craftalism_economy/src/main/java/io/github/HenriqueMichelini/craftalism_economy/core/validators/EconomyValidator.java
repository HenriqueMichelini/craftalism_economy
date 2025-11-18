package io.github.HenriqueMichelini.craftalism_economy.core.validators;

public class EconomyValidator {
    public EconomyValidator() {}

    public boolean hasSufficientFunds(long currentBalance, long amount) {
        return currentBalance >= amount && isValidAmount(amount);
    }

    private boolean isValidAmount(long amount) {
        return amount >= 0;
    }

    public boolean isGreaterThanZero(long amount) {
        return amount > 0;
    }
}
