package io.github.HenriqueMichelini.craftalism_economy.domain.service.managers;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.validators.AmountCheck;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EconomyManager {

    public EconomyManager(@NotNull AmountCheck amountCheck) {
    }

    public void deposit(UUID playerUUID, long amount) {

    }

    public boolean withdraw(UUID playerUUID, long amount) {

    }

    public boolean transferBalance(UUID from, UUID to, long amount) {

    }
}