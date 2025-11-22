package io.github.HenriqueMichelini.craftalism_economy;

import io.github.HenriqueMichelini.craftalism_economy.application.service.PayApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.application.service.PlayerApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.CurrencyMessages;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism_economy.presentation.commands.PayCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Objects;

public final class CraftalismEconomy extends JavaPlugin {
    private CurrencyFormatter currencyFormatter;
    private CurrencyParser currencyParser;

    private LogManager logManager;
    private PluginLogger logger;

    private PayMessages payMessages;
    private PayApplicationService payApplicationService;

    private PlayerApplicationService playerApplicationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.logManager = new LogManager(this);
        this.logger = new PluginLogger(this, logManager);
        this.currencyParser = new CurrencyParser(new CurrencyMessages(logger));

        initializeMoneyFormat();
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

    private void registerCommands() {
        registerCommand("pay", new PayCommand(payMessages, payApplicationService));
//        registerCommand("balance", new BalanceCommand(balanceManager, this, currencyFormatter, playerValidator, logger, new BalanceMessages(logger)));
//        registerCommand("baltop", new BaltopCommand(balanceManager, this, currencyFormatter));
//        registerCommand("setbalance", new SetBalanceCommand(balanceManager, this, currencyFormatter, playerValidator, currencyParser));
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

    }

    public CurrencyFormatter getCurrencyFormatter() {
        return currencyFormatter;
    }

    public CurrencyParser getCurrencyParser() { return currencyParser; }
}