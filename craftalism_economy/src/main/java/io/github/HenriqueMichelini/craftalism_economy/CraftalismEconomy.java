package io.github.HenriqueMichelini.craftalism_economy;

import io.github.HenriqueMichelini.craftalism_economy.economy.*;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BaltopCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.PayCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.SetBalanceCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CraftalismEconomy extends JavaPlugin {

    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        this.economyManager = new EconomyManager(this);
        this.economyManager.loadBalances();

        Objects.requireNonNull(getCommand("pay")).setExecutor(new PayCommand(economyManager));
        Objects.requireNonNull(getCommand("balance")).setExecutor(new BalanceCommand(economyManager));
        Objects.requireNonNull(getCommand("baltop")).setExecutor(new BaltopCommand(economyManager)); // Register baltop
        Objects.requireNonNull(getCommand("setbalance")).setExecutor(new SetBalanceCommand(economyManager));

        // Save balances on disable
        getServer().getPluginManager().registerEvents(new Listener() {}, this);

        getLogger().info("[Craftalism Economy] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (this.economyManager != null) {
            this.economyManager.saveBalances();
        }
        getLogger().info("[Craftalism Economy] Plugin disabled!");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
