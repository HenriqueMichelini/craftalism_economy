package io.github.HenriqueMichelini.craftalism_economy;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.BaltopCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.PayCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.command.SetBalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.CommandValidator;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.EconomyValidator;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.PlayerValidator;
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
    private CurrencyFormatter currencyFormatter;

    private long defaultBalance;
    private final File balancesFile = new File(this.getDataFolder(), "balances.yml");
    private final FileConfiguration balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
    private BalanceManager balanceManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.defaultBalance = parseDefaultBalance();
        this.balanceManager = new BalanceManager(balancesFile, balancesConfig, this);

        initializeMoneyFormat();
        initializeEconomyManager();
        registerCommands();

        getLogger().info("Plugin enabled successfully");
    }

    private void initializeMoneyFormat() {
        String localeStr = getConfig().getString("locale", "en-US");
        String currencySymbol = getConfig().getString("currency-symbol", "$");
        String nullRep = getConfig().getString("null-representation", "—");

        this.currencyFormatter = new CurrencyFormatter(
                parseLocale(localeStr),
                currencySymbol,
                nullRep,
                this

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
        long balance = getConfig().getLong("default-balance", 100000000);
        if (balance < 0) {
            getLogger().warning("Invalid negative default balance, using 10000.0000");
            balance = 100000000;
        }
        return balance;
    }

    private void initializeEconomyManager() {
        this.economyManager = new EconomyManager(balanceManager);
        getLogger().fine("Economy manager initialized");
    }

    private void registerCommands() {
        registerCommand("pay", new PayCommand(economyManager, balanceManager, this, currencyFormatter));
        registerCommand("balance", new BalanceCommand(balanceManager, this, currencyFormatter));
        registerCommand("baltop", new BaltopCommand(balanceManager, this, currencyFormatter));
        registerCommand("setbalance", new SetBalanceCommand(balanceManager, this, currencyFormatter));
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

    public PlayerValidator getPlayerValidator() { return new PlayerValidator(); }

    public CommandValidator getCommandValidator() { return new CommandValidator(); }

    public EconomyValidator getEconomyValidator() { return new EconomyValidator(balanceManager); }

    public long getDefaultBalance() {
        return defaultBalance;
    }

    public CurrencyFormatter getCurrencyFormatter() {
        return currencyFormatter;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}