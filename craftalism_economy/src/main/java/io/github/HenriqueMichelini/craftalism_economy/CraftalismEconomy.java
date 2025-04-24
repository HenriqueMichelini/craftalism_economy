package io.github.HenriqueMichelini.craftalism_economy;

import io.github.HenriqueMichelini.craftalism_economy.economy.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BaltopCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.PayCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.SetBalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public final class CraftalismEconomy extends JavaPlugin {
    private EconomyManager economyManager;
    private MoneyFormat moneyFormat;
    private BigDecimal defaultBalance;

    @Override
    public void onEnable() {
        // Create config if missing and load it
        saveDefaultConfig();
        reloadConfig();

        this.defaultBalance = parseDefaultBalance();

        // Initialize components
        initializeMoneyFormat();
        initializeEconomyManager();
        registerCommands();

        getLogger().info("Plugin enabled successfully");
    }

    private void initializeMoneyFormat() {
        String localeStr = getConfig().getString("locale", "en-US");
        String currencySymbol = getConfig().getString("currency-symbol", "$");
        String nullRep = getConfig().getString("null-representation", "—");

        this.moneyFormat = new MoneyFormat(
                parseLocale(localeStr),
                currencySymbol,
                nullRep
        );
    }

    private Locale parseLocale(String localeStr) {
        try {
            return Locale.forLanguageTag(localeStr.replace('_', '-'));
        } catch (Exception e) {
            getLogger().warning("Invalid locale '" + localeStr + "', defaulting to en-US");
            return Locale.US;
        }
    }

    private BigDecimal parseDefaultBalance() {
        double balance = getConfig().getDouble("default-balance", 10000.0);
        if (balance < 0) {
            getLogger().warning("Invalid negative default balance, using 10000.0");
            balance = 10000.0;
        }
        return BigDecimal.valueOf(balance)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void initializeEconomyManager() {
        this.economyManager = new EconomyManager(this, defaultBalance);
        getLogger().fine("Economy manager initialized");
    }

    private void registerCommands() {
        registerCommand("pay", new PayCommand(economyManager, this, moneyFormat));
        registerCommand("balance", new BalanceCommand(economyManager, this, moneyFormat));
        registerCommand("baltop", new BaltopCommand(economyManager, this, moneyFormat));
        registerCommand("setbalance", new SetBalanceCommand(economyManager, this, moneyFormat));
    }

    private void registerCommand(String name, CommandExecutor executor) {
        try {
            Objects.requireNonNull(getCommand(name), "Command not registered: " + name)
                    .setExecutor(executor);
            getLogger().fine("Registered command: /" + name);
        } catch (NullPointerException e) {
            getLogger().warning("Failed to register command: /" + name);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (economyManager != null) {
                economyManager.saveBalances();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error saving balances during shutdown", e);
        } finally {
            getLogger().info("Plugin disabled successfully");
        }
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}