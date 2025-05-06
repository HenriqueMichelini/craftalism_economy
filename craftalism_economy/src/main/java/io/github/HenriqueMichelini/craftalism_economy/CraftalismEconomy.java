package io.github.HenriqueMichelini.craftalism_economy;

import io.github.HenriqueMichelini.craftalism_economy.economy.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BaltopCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.PayCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.SetBalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.MoneyFormat;
import io.github.HenriqueMichelini.craftalism_economy.economy.util.Validators;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public final class CraftalismEconomy extends JavaPlugin {
    private EconomyManager economyManager;
    private MoneyFormat moneyFormat;
    private long defaultBalance;
    private Validators validators;
    private final File balancesFile = new File(this.getDataFolder(), "balances.yml");

    private final FileConfiguration balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
    private BalanceManager balanceManager;

    @Override
    public void onEnable() {
        // Create config if missing and load it
        saveDefaultConfig();
        reloadConfig();

        this.defaultBalance = parseDefaultBalance();
        this.balanceManager = new BalanceManager(balancesFile, balancesConfig, defaultBalance, this);
        this.validators = new Validators(economyManager, balanceManager);

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

    private long parseDefaultBalance() {
        long balance = getConfig().getLong("default-balance", 10000);
        if (balance < 0) {
            getLogger().warning("Invalid negative default balance, using 10000.0000");
            balance = 10000;
        }
        return balance;
    }

    private void initializeEconomyManager() {
        this.economyManager = new EconomyManager(validators, balanceManager);
        getLogger().fine("Economy manager initialized");
    }

    private void registerCommands() {
        registerCommand("pay", new PayCommand(economyManager, this, moneyFormat, validators));
        registerCommand("balance", new BalanceCommand(balanceManager, this, moneyFormat, validators));
        registerCommand("baltop", new BaltopCommand(economyManager, this, moneyFormat));
        registerCommand("setbalance", new SetBalanceCommand(balanceManager, this, moneyFormat, validators));
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
            if (balanceManager != null) {
                balanceManager.saveBalances();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error saving balances during shutdown", e);
        } finally {
            getLogger().info("Plugin disabled successfully");
        }
    }

    public FileConfiguration getBalancesConfig() {
        return balancesConfig;
    }

    public File getBalancesFile() {
        return balancesFile;
    }

    public Validators getValidators() {
        return validators;
    }

    public long getDefaultBalance() {
        return defaultBalance;
    }

    public MoneyFormat getMoneyFormat() {
        return moneyFormat;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}