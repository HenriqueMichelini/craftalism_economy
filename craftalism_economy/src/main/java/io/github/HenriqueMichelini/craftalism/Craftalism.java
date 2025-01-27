package io.github.HenriqueMichelini.craftalism;

import io.github.HenriqueMichelini.craftalism.economy.*;
import io.github.HenriqueMichelini.craftalism.economy.command.BalanceCommand;
import io.github.HenriqueMichelini.craftalism.economy.command.BaltopCommand;
import io.github.HenriqueMichelini.craftalism.economy.command.PayCommand;
import io.github.HenriqueMichelini.craftalism.economy.command.SetBalanceCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Craftalism extends JavaPlugin {

    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        EconomyManager economyManager = new EconomyManager(this);
        economyManager.loadBalances();

        Objects.requireNonNull(getCommand("pay")).setExecutor(new PayCommand(economyManager));
        Objects.requireNonNull(getCommand("balance")).setExecutor(new BalanceCommand(economyManager));
        Objects.requireNonNull(getCommand("baltop")).setExecutor(new BaltopCommand(economyManager)); // Register baltop
        Objects.requireNonNull(getCommand("setbalance")).setExecutor(new SetBalanceCommand(economyManager));

        // Save balances on disable
        getServer().getPluginManager().registerEvents(new Listener() {}, this);

        getLogger().info("[Craftalism] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Ensure economyManager is not null before saving balances
        if (economyManager != null) {
            economyManager.saveBalances();
        }
        getLogger().info("[Craftalism] Plugin disabled!");
    }
}
