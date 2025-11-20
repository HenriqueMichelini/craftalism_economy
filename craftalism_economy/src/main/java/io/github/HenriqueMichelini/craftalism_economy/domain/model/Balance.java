package io.github.HenriqueMichelini.craftalism_economy.domain.model;

import java.util.UUID;

public class Balance {
    UUID uuid;
    Long amount;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}